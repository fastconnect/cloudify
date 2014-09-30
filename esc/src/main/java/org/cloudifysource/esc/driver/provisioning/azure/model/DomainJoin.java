package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "DomainJoin", propOrder = { "credentials", "joinDomain", "machineObjectOU" })
public class DomainJoin {

	private JoinCredentials credentials;
	private String joinDomain;
	private String machineObjectOU;

	@XmlElement(name = "Credentials")
	public JoinCredentials getCredentials() {
		return credentials;
	}

	public void setCredentials(JoinCredentials credentials) {
		this.credentials = credentials;
	}

	@XmlElement(name = "JoinDomain")
	public String getJoinDomain() {
		return joinDomain;
	}

	public void setJoinDomain(String joinDomain) {
		this.joinDomain = joinDomain;
	}

	public String getMachineObjectOU() {
		return machineObjectOU;
	}

	@XmlElement(name = "MachineObjectOU")
	public void setMachineObjectOU(String machineObjectOU) {
		this.machineObjectOU = machineObjectOU;
	}

}
