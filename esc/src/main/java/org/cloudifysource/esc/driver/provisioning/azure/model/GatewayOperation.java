package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "GatewayOperation")
@XmlType(propOrder = { "id", "status", "statusCode", "data", "error" })
@Deprecated
// not working
public class GatewayOperation extends Operation {

	private String data;

	@XmlElement(name = "Data")
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
