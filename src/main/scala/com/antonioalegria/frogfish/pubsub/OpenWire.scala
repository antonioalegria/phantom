package com.antonioalegria.frogfish.pubsub

import org.apache.commons.lang.StringUtils
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms._
import java.net.URI

import com.antonioalegria.frogfish.util.Slf4jLogger

class OpenWire(uri: String) extends MessageListener with Slf4jLogger {
    val parsedUri  = new URI(uri)
    val host = parsedUri.getHost
    val port = parsedUri.getPort
    val user = userFromUri(parsedUri)
    val password = passwordFromUri(parsedUri)
    val namespace = namespaceFromUri(parsedUri)
    val timeout = 300
    val poll = 1
    var defaultTtl = 300
    val retries = 3
    val isAsync = false

    val factory    = new ActiveMQConnectionFactory(uri)
    val connection = factory.createConnection(user, password)
    connection.start
    val session    = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

    var subCallback: (String, String) => Any = _

    // strip initial and final '/'
    def namespaceFromUri(uri : URI) = {
        uri.getPath.length match {
            case 0 => ""
            case 1 => ""
            case _ => StringUtils.chomp(uri.getPath, "/").replace("/",".").substring(1) + "."
        }
    }

    def userFromUri(uri : URI) = uri.getUserInfo.split(":")(0)

    def passwordFromUri(uri : URI) = uri.getUserInfo.split(":")(1)

    def onMessage(msg: Message) {
        val text = msg match {
            case x:TextMessage => x.getText
            case _ => null
        }

        val dest = msg.getJMSDestination match {
            case x:Topic => x.getTopicName
            case x:Queue => x.getQueueName
            case _ => null
        }

        subCallback(dest, text)
    }

    def sub(dest: String, block: (String, String) => Any, selector: String = null) {
        val actualDest = namespace+dest
        val topic = session.createTopic(actualDest)
        val consumer = session.createConsumer(topic)
        subCallback = block
        consumer.setMessageListener(this)
    }

    def pub(dest: String, msg: String, ttl: Int = defaultTtl) {
        val actualDest = namespace+dest
        val topic = session.createTopic(actualDest)
        val producer = session.createProducer(topic)

        val txtMsg = session.createTextMessage()
        txtMsg.setText(msg)

        producer.send(txtMsg)
    }
}
