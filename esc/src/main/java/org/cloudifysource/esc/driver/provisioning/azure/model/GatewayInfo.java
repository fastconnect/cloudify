package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * ignoring other fields VIPAddress, lastevent...
 */
@XmlRootElement(name = "Gateway")
public class GatewayInfo {

	private final static String GATEWAY_STATE_PROVISIONED = "Provisioned";
	private final static String GATEWAY_STATE_NOT_PROVISIONED = "NotProvisioned";

	private String state;

	@XmlElement(name = "State")
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public boolean isReadyForProvisioning() {

		if (state != null) {
			if (state.equals(GATEWAY_STATE_NOT_PROVISIONED)) {
				return true;
			}
		}
		return false;
	}

	public boolean isReadyToConnect() {
		if (state != null) {
			if (state.equals(GATEWAY_STATE_PROVISIONED)) {
				return true;
			}
		}
		return false;
	}

}
