package com.ibm.hardillb.tv2mqtt;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

public class Status implements Runnable {

	private TVSerialInterface serialInterface = null;
	private TVState state = null;
	private boolean stop = false;
	private TopicPublisher publisher;
	private TopicSession session;
	
	public Status(TopicSession session, TopicPublisher publisher, TVSerialInterface serialInterface, TVState state) {
		this.publisher = publisher;
		this.session = session;
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
			
			try {
				TextMessage message = session.createTextMessage(state.toString());
				//publisher.send(message,DeliveryMode.NON_PERSISTENT,50,5000);
				publisher.send(message);
				
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
