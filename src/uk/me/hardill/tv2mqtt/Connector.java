package uk.me.hardill.tv2mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class Connector {

	private String location = "";
	private String server = "";
	private String port = "";
	private MqttClient client = null;
	private MqttTopic statusTopic = null;
	
	private Status status;
	private TVState state;
	private TVSerialInterface tvInterface;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connector con = new Connector(args);
		con.start();
	}
	
	public Connector(String args[]) {
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("--location") || args[i].equals("-l")) {
				location = args[++i];
			} else if (args[i].equals("--server") || args[i].equals("-s")) {
				server = args[++i];
			} else if (args[i].equals("--port") || args[i].equals("-p")) {
				port = args[++i];
			} 
		}
		try {
			client = new MqttClient("tcp://" + server + ":1883", "tv2mqtt-" + location);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() {
		
		try {
			client.connect();
			client.subscribe("TV/" + location + "/Commands");
			statusTopic = client.getTopic("TV/" + location + "/Status");
			
			state = new TVState();
			tvInterface = new TVSerialInterface(port, state);
			Thread t = new Thread(new Status(statusTopic,tvInterface,state));
			t.start();
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		client.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage msg) throws Exception {
				
				String messageBody = new String(msg.getPayload());
				if (messageBody.equals("off")) {
					tvInterface.send("ka 00 00");
				} else if (messageBody.equals("on")) {
					tvInterface.send("ka 00 01");
				} else if (messageBody.equals("rgb")) {
					tvInterface.send("xb 00 60");
				} else if (messageBody.equals("hdmi")) {
					tvInterface.send("xb 00 90");
				}
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void connectionLost(Throwable exception) {
				// TODO Auto-generated method stub
				
			}
		});
	}

}
