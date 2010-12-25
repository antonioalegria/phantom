package com.antonioalegria.frogfish.esper

import com.espertech.esper.client.Configuration
import com.espertech.esper.client.EPServiceProvider
import com.espertech.esper.client.EPServiceProviderManager
import com.espertech.esper.client.UpdateListener
import com.espertech.esper.client.EventBean
import com.espertech.esper.client.deploy.Module

import scala.collection.JavaConversions._

import java.io.File
import java.util.ArrayList

import org.codehaus.jackson.map.ObjectMapper

import com.antonioalegria.frogfish.pubsub.OpenWire
import com.antonioalegria.frogfish.util.Slf4jLogger

object Engine { }

class OpenWireUpdateListener(connector: OpenWire, topic: String, eventType: String) extends UpdateListener with Slf4jLogger {
    val jsonMapper = new ObjectMapper

    def processEvent(evtBean: EventBean) {
        val eventType = evtBean.getEventType.getName
        val eventJson = jsonMapper.writeValueAsString(evtBean.getUnderlying)
        connector.pub(topic, eventJson, 300)
        debug("pub("+topic+") "+eventType+"("+eventJson+")")
    }

    override def update(newEvents: Array[EventBean], oldEvents: Array[EventBean]) {
        val newEventCount = if (newEvents != null) newEvents.length else 0
        val oldEventCount = if (oldEvents != null) oldEvents.length else 0
        debug("update: received "+newEventCount+" new events and "+oldEventCount+" old events.")

        if (newEvents == null) return
        newEvents.foreach(processEvent)
    }
}

class LogUpdateListener extends UpdateListener with Slf4jLogger {
    val jsonMapper = new ObjectMapper

    def processEvent(evtBean: EventBean) {
        val eventType = evtBean.getEventType.getName
        val eventJson = jsonMapper.writeValueAsString(evtBean.getUnderlying)
        warn(eventType+": "+eventJson)
    }

    override def update(newEvents: Array[EventBean], oldEvents: Array[EventBean]) {
        if (newEvents == null) return
        newEvents.foreach(processEvent)
    }
}

// TODO: catch exceptions in threads!!!
class Engine(engineId: String, xmlConfig: String = "esper.cfg.xml", eplFiles: List[String] = Nil, outputMaps: List[(String,UpdateListener)] = Nil) extends Slf4jLogger {

    val xmlConfigObj    = new File(xmlConfig)
    var outputListeners = Map[String,UpdateListener]()
    val config          = new Configuration()
    config.configure(xmlConfig)

    val engineProvider = EPServiceProviderManager.getProvider(engineId, config)
    val admin          = engineProvider.getEPAdministrator
    val runtime        = engineProvider.getEPRuntime
    val deployAdmin    = admin.getDeploymentAdmin

    eplFiles.foreach(installEplFile)
    deployModules
    outputMaps.foreach(arg => installOutputHandler(arg._1, arg._2))
    admin.getStatementNames.foreach(checkOutputStatement)

    def checkOutputStatement(stmtName: String) {
        debug("checkOutputStatement: "+stmtName)
        if (stmtName.startsWith("out_")) {
            info("Found output statement "+stmtName)
            admin.getStatement(stmtName).addListener(new LogUpdateListener)
        }
    }

    def installEplFile(filePath: String) {
        info("Installing EPL module "+filePath)
        val module = deployAdmin.read(filePath)
        deployAdmin.add(module)
    }

    def deployModule(module: Module) {
        info("Deploying EPL module "+module.getName)
        deployAdmin.deploy(module, null)
    }

    def installOutputHandler(eventType: String, listener: UpdateListener) {
        val outEventType = "__out_"+eventType+"__"
        val statementStr = "@Name('"+outEventType+"')\n"+
                           "insert into "+outEventType+"\n"+
                           "select * from "+eventType

        info("Installing output handler for event type "+eventType)
        info("Used EPL:\n"+statementStr)

        val statement = admin.createEPL(statementStr)
        outputListeners += eventType -> listener
        statement.addListener(listener)
    }

    def deployModules() {
        val moduleInfos = deployAdmin.getDeploymentInformation
        val modules = List.fromArray(moduleInfos.map(arg => arg.getModule))
        val orderedModules = deployAdmin.getDeploymentOrder(modules, null).getOrdered

        orderedModules.foreach(deployModule)
    }

    def insertEvent(eventType: String, event: java.util.HashMap[String,Any]) {
        runtime.sendEvent(event, eventType)
    }
}
