package com.ibm.hardillb.tv2mqtt;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.jms.JmsTopic;
import com.ibm.msg.client.jms.JmsTopicConnectionFactory;
import com.ibm.msg.client.mqtt.MQTTConstants;

public class Connector {

	private TVState state = new TVState();
	private TVSerialInterface interface1  = null;
	private Status status;
	
	private TopicSubscriber subscriber = null;
	private TopicConnection jmsCon = null;
	private TopicSession session = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connector con = new Connector(args[0], args[1]);
		con.start();
	}
	
	public Connector(String port, String location) {
		init(port, location);
	}
	
	public void start() {
		try {
			jmsCon.start();
			Thread statusThread = new Thread(status);
			statusThread.start();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stop() {
		try {
			status.stop();
			jmsCon.stop();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void init(String port, String location) {
		JmsFactoryFactory factory = null;
		JmsTopicConnectionFactory jmsTopicConnectionFactory = null;
		JmsTopic topicInbound = null;
		JmsTopic topicOutbound = null;
		
		try {
			factory = JmsFactoryFactory.getInstance(MQTTConstants.MQTT_PROVIDER_NAME);
			
			jmsTopicConnectionFactory = factory.createTopicConnectionFactory();
			
			jmsTopicConnectionFactory.setStringProperty(MQTTConstants.MQTT_CONNECTION_URL, "tcp://192.168.1.5:1883");
			
			topicInbound = factory.createTopic("TV/" + location + "/Commands");
			topicOutbound = factory.createTopic("TV/" + location + "/Status");
			
			jmsCon = jmsTopicConnectionFactory.createTopicConnection();
			jmsCon.setClientID("TVControl-" + location);
			
			session =  jmsCon.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
			TopicSession session2 =  jmsCon.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
			
			subscriber = session.createSubscriber(topicInbound);
			TopicPublisher publisher = session2.createPublisher(topicOutbound);
			interface1 = new TVSerialInterface(port,state);
			status = new Status(session2, publisher, interface1, state);
			
			subscriber.setMessageListener(new MessageListener(){
				
				public void onMessage(Message message) {
					String messageBody = "";
					if (message instanceof TextMessage) {
						TextMessage txtMessage = (TextMessage) message;
						try {
							messageBody = txtMessage.getText();
						} catch (JMSException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (message instanceof BytesMessage) {
						BytesMessage bytesMessage = (BytesMessage) message;
						byte buffer[] = null;
						try {
							buffer = new byte[(int)bytesMessage.getBodyLength()];
							bytesMessage.readBytes(buffer);
						} catch (JMSException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						messageBody = new String(buffer);
					}
					//System.err.println("message = '" + messageBody + "'");
					if (messageBody.equals("off")) {
						interface1.send("ka 00 00");
					} else if (messageBody.equals("on")) {
						interface1.send("ka 00 01");
					} else if (messageBody.equals("rgb")) {
						interface1.send("xb 00 60");
					} else if (messageBody.equals("hdmi")) {
						interface1.send("xb 00 90");
					}
					//state.notify();
				}
				
			});
			
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	

}
