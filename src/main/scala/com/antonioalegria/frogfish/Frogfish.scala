package com.antonioalegria.frogfish

import org.codehaus.jackson.map.ObjectMapper

import com.antonioalegria.frogfish.pubsub._
import com.antonioalegria.frogfish.esper._
import com.antonioalegria.frogfish.util._

object Frogfish extends Slf4jLogger {
    val engineId = "FrogfishOccurrenceMapReducer"
    val connectorUri = "tcp://user:user@192.168.180.110:61616"
    val inputMap = Map("uri" -> connectorUri, "topic" -> "core.Occurrence", "eventType" -> "Occurrence") :: Nil
    val outputMap = Map("uri" -> connectorUri, "topic" -> "core.OccurrenceStat", "eventType" -> "OccurrenceStat") :: Nil
    val epl = "epl/occurrence_types.epl" :: "epl/occurrence_map.epl" :: "epl/occurrence_map_monit.epl" :: "epl/occurrence_reduce.epl" :: "epl/occurrence_reduce_monit.epl" :: Nil
    val xmlConfig = "esper.cfg.xml"

    def outputListenerFromParams(uri: String, topic: String, eventType: String) = {
        val connector = new OpenWire(connectorUri)
        new OpenWireUpdateListener(connector, topic, eventType)
    }

    def main(args: Array[String]) {
        val outputListeners = outputMap.map(arg => (arg("eventType"), outputListenerFromParams(arg("uri"), arg("topic"), arg("eventType"))))
        val engine = new Engine(engineId, xmlConfig, epl, outputListeners)

        while(true) {
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
