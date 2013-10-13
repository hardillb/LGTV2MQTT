package uk.me.hardill.tv2mqtt;

public class TVState {
	
	public enum INPUTS {ANALOUG, DIGITAL, AV1, AV2, AV3, AV4, HDMI1, HDMI2, HDMI3, COMPONENT, RGB, SVIDEO, DVI}
	
	private boolean on = false;
	private int channel = 0;
	private INPUTS input = INPUTS.RGB;
	private int volume = 0;
	private boolean mute = false;
	
	public boolean isOn() {
		return on;
	}
	public void setOn(boolean on) {
		this.on = on;
	}
	public int getChannel() {
		return channel;
	}
	public void setChannel(int channel) {
		this.channel = channel;
	}
	public INPUTS getInput() {
		return input;
	}
	public void setInput(INPUTS input) {
		this.input = input;
	}
	public int getVolume() {
		return volume;
	}
	public void setVolume(int volume) {
		this.volume = volume;
	}
	
	public boolean isMute() {
		return mute;
	}
	public void setMute(boolean mute) {
		this.mute = mute;
	}
	
	public String toString() {
		if (!on) {
			return "off";
		} else {
			StringBuffer status = new StringBuffer("on:");
			status.append(input.name());
			if (input == INPUTS.ANALOUG || input == INPUTS.DIGITAL) {
				status.append(":").append(channel);
			}
			if (mute) {
				status.append(":").append("mute");
			} else {
				status.append(":").append(volume);
			}
			return status.toString();
		}
	}
}
