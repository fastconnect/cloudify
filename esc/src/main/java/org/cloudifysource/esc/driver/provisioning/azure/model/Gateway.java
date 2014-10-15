package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Gateway")
public class Gateway {

	private ConnectionsToLocalNetwork connectionsToLocalNetwork;

	public Gateway() {
	}

	public Gateway(ConnectionsToLocalNetwork connectionsToLocalNetwork) {
		this.connectionsToLocalNetwork = connectionsToLocalNetwork;
	}

	@XmlElement(name = "ConnectionsToLocalNetwork")
	public ConnectionsToLocalNetwork getConnectionsToLocalNetwork() {
		return connectionsToLocalNetwork;
	}

	public void setConnectionsToLocalNetwork(ConnectionsToLocalNetwork connectionsToLocalNetwork) {
		this.connectionsToLocalNetwork = connectionsToLocalNetwork;
	}

}
