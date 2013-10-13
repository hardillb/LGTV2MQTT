package uk.me.hardill.tv2mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class Status implements Runnable {

	private TVSerialInterface serialInterface = null;
	private TVState state = null;
	private boolean stop = false;
	private MqttTopic topic;
	
	public Status(MqttTopic topic, TVSerialInterface serialInterface, TVState state) {
		this.topic = topic;
		this.serialInterface = serialInterface;
		this.state = state;
	}
	
	public void stop() {
		stop = true;
	}
	
	public void run() {
		
		while (!stop) {
			
			//System.err.println("About to check tv state");
			
			//these will trigger more notifies but not a problem as nothing should be waiting
			serialInterface.send("kl 00 00"); // turns off annoying on screen display
			serialInterface.send("ka 00 ff"); // check power status
			serialInterface.send("kf 00 ff"); // check volume
			serialInterface.send("xb 00 ff"); // check input
			
			//System.err.println(state.toString());
			
			MqttMessage message = new MqttMessage();
			message.setPayload(state.toString().getBytes());
			
			message.setRetained(true);
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (MqttException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//pause for 5 seconds or until there is a change
			synchronized (state) {
				try {
					state.wait(1000 * 5);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
