package uk.me.hardill.tv2mqtt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;


public class TVSerialInterface implements SerialPortEventListener{
	
	private TVState state = null;
	
	private boolean waitOnReply = false;
	private Object lock = new Object();
	
	private SerialPort port = null;
	private InputStream input = null;
	//private PushbackInputStream input = null;
	private OutputStream output = null;
	private StringBuffer buffer = new StringBuffer();
	
	Pattern pat = Pattern.compile("(\\w) (\\d+?) (OK|NG)(\\w+)x");
	
	public TVSerialInterface(String serialPort, TVState state) {
		this.state = state;
		try {
			port = (SerialPort) CommPortIdentifier.getPortIdentifier(serialPort).open("test", 2000);
			System.err.println( port.getName() + " opened");
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			port.addEventListener(this);
			port.notifyOnDataAvailable(true);
			input = port.getInputStream();
			output = port.getOutputStream();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		} catch (NoSuchPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void serialEvent(SerialPortEvent spe) {
		switch (spe.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
        	System.err.println("sent");
            break;
        case SerialPortEvent.DATA_AVAILABLE:
        	//System.err.println("data available");
        	int c = 0;
        	synchronized (lock) {
	            try {
					do {
						c = input.read();
						buffer.append((char) c);
						if (c == 'x') {
							decode(buffer.toString().trim());
							waitOnReply = false;
							lock.notify();
							buffer = new StringBuffer();
						}
					} while (input.available() > 0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
            break;
		}
	}
	
	private void decode(String response) {
		char command = response.charAt(0);
		int i = 0;
		response = response.trim();
		Matcher matcher = pat.matcher(response);
		if (matcher.matches()) {
			if (matcher.group(3).equals("OK")){
				command = matcher.group(1).charAt(0);
				switch (command) {
				case 'a':
					//power
					i = Integer.parseInt(matcher.group(4));
					if (i == 1) {
						state.setOn(true);
					} else {
						state.setOn(false);
					}
					break;
				case 'b':
					//input
					String input = matcher.group(4);
					i = Integer.parseInt(input, 16);
					switch (i) {
					case 0x00:
						state.setInput(TVState.INPUTS.DIGITAL);
						break;
					case 0x10:
						state.setInput(TVState.INPUTS.ANALOUG);
						break;
					case 0x20:
						state.setInput(TVState.INPUTS.AV1);
						break;
					case 0x21:
						state.setInput(TVState.INPUTS.AV2);
						break;
					case 0x22:
						state.setInput(TVState.INPUTS.AV3);
						break;
					case 0x23:
						state.setInput(TVState.INPUTS.AV4);
						break;
					case 0x30:
					case 0x31:
					case 0x32:
						state.setInput(TVState.INPUTS.SVIDEO);
						break;
					case 0x40:
					case 0x41:
					case 0x42:
						state.setInput(TVState.INPUTS.COMPONENT);
						break;
					case 0x60:
						state.setInput(TVState.INPUTS.RGB);
						break;
					case 0x80:
					case 0x81:
					case 0x82:
						state.setInput(TVState.INPUTS.DVI);
						break;
					case 0x90:
						state.setInput(TVState.INPUTS.HDMI1);
						break;
					case 0x91:
						state.setInput(TVState.INPUTS.HDMI2);
						break;
					case 0x92:
						state.setInput(TVState.INPUTS.HDMI3);
						break;
					default:
						break;
					}
					break;
				case 'c':
					//aspect ratio
					break;
				case 'd':
					//screen mute
					break;
				case 'e':
					//mute
					i = Integer.parseInt(matcher.group(4));
					if (i == 1) {
						state.setMute(true);
					} else {
						state.setMute(false);
					}
					break;
				case 'f':
					//volume
					String temp = matcher.group(4);
					int level = (Integer.parseInt(temp,16) * 100) / 0x64;
					state.setVolume(level);
					break;
				default:
					break;
				}
				
				synchronized (state) {
					state.notifyAll();
				}
			} else {
				// there has been an error returned
				//System.err.println("error - " + response);
			}
		}
		
	}

	/**
	 * Blocks (and retries) until it gets a response
	 * 
	 * @param cmd command to send to tv
	 * 
	 */
	public synchronized int send(String cmd) {
		synchronized (lock) {
			waitOnReply = true;
		}
		
		do {
			send0(cmd);
			try {
				synchronized (lock) {
					lock.wait(1000);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while(waitOnReply);
		
		return 0;
	}

	private void send0(String cmd) {
		try {
			output.write(cmd.getBytes());
			output.write(13);
			output.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
