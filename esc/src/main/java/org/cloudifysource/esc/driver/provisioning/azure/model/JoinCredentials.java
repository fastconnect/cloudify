package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Credentials", propOrder = { "domain", "userNamer", "password" })
public class JoinCredentials {

	private String domain;
	private String userNamer;
	private String password;

	@XmlElement(name = "Domain")
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@XmlElement(name = "Username")
	public String getUserNamer() {
		return userNamer;
	}

	public void setUserNamer(String userNamer) {
		this.userNamer = userNamer;
	}

	@XmlElement(name = "Password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
