package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CreateGatewayParameters")
public class CreateGatewayParameters {

	private String gatewayType;

	public CreateGatewayParameters() {
	}

	public CreateGatewayParameters(String gatewayType) {
		super();
		this.gatewayType = gatewayType;
	}

	@XmlElement(name = "gatewayType")
	public String getGatewayType() {
		return gatewayType;
	}

	public void setGatewayType(String gatewayType) {
		this.gatewayType = gatewayType;
	}

}
