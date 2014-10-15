package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LocalNetworkSiteRef")
public class LocalNetworkSiteRef {

	public LocalNetworkSiteRef() {
	}

	public LocalNetworkSiteRef(String name, Connection connection) {
		this.name = name;
		this.connection = connection;
	}

	private String name;
	private Connection connection;

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "Connection")
	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

}
