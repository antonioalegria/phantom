package com.antonioalegria.frogfish

import org.codehaus.jackson.map.ObjectMapper

import com.antonioalegria.frogfish.pubsub._
import com.antonioalegria.frogfish.esper._
import com.antonioalegria.frogfish.util._

class OpenWireInputListener(uri: String, topic: String, eventType: String, engine: Engine) extends Slf4jLogger {
    val jsonMapper = new ObjectMapper
    val connector = new OpenWire(uri)

    connector.sub(topic, receive)

    def receive(dest: String, msg: String) {
        debug("receive: "+dest+"("+msg+")")
        val eventMap = jsonMapper.readValue(msg, classOf[java.util.HashMap[String,Any]])

        debug("receive: inserting "+eventType+"("+eventMap+")")

        engine.insertEvent(eventType, eventMap)
    }
}

object Frogfish extends Slf4jLogger {
    val engineId = "FrogfishOccurrenceMapReducer"
    val connectorUri = "tcp://user:user@192.168.180.110:61616"
    val inputMap = Map("uri" -> connectorUri, "topic" -> "core.Occurrence", "eventType" -> "Occurrence") :: Nil
    val outputMap = Map("uri" -> connectorUri, "topic" -> "core.OccurrenceStat", "eventType" -> "OccurrenceStat") :: Nil
    val epl = "epl/occurrence_types.epl" :: "epl/occurrence_map.epl" :: "epl/occurrence_map_monit.epl" :: "epl/occurrence_reduce.epl" :: "epl/occurrence_reduce_monit.epl" :: Nil
    val xmlConfig = "esper.cfg.xml"

    var inputListeners: List[OpenWireInputListener] = Nil

    def outputListenerFromParams(uri: String, topic: String, eventType: String) = {
        val connector = new OpenWire(connectorUri)
        new OpenWireUpdateListener(connector, topic, eventType)
    }

    def registerInputListener(uri: String, topic: String, eventType: String, engine: Engine) {
        inputListeners ::= new OpenWireInputListener(uri, topic, eventType, engine)
    }

    def main(args: Array[String]) {
        val outputListeners = outputMap.map(arg => (arg("eventType"), outputListenerFromParams(arg("uri"), arg("topic"), arg("eventType"))))
        val engine = new Engine(engineId, xmlConfig, epl, outputListeners)

        // TODO: change to map
        inputMap.foreach(arg => registerInputListener(arg("uri"), arg("topic"), arg("eventType"), engine))

        while(false) {
            var dummy = Console.readLine("Press <ENTER> to inject new event: ")
            var event = new java.util.HashMap[String, Any]
            event.put("uri", "com.twitter.Hashtag")
            event.put("key", "nowplaying")
            event.put("ts", System.currentTimeMillis)
            debug(event)
            engine.insertEvent("Occurrence", event)
        }

        Thread.sleep(999999999)
    }
}
