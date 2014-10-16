package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "CreateGatewayParameters")
public class CreateGatewayParameters {

	private String gatewayType;

	@XmlElement(name = "GatewayType")
	public String getGatewayType(){
		return gatewayType;
	}

	public void setGatewayType(String gatewayType) {
		this.gatewayType = gatewayType;
	}

}
