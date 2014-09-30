package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author mourouvi (fastconnect)
 *
 */

@XmlType(propOrder = { "type", "configurationSetType", "computerName", "adminPassword", "domainJoin", "winRM",
		"adminUsername" })
public class WindowsProvisioningConfigurationSet extends ConfigurationSet {

	private String configurationSetType = ConfigurationSet.WINDOWS_PROVISIONING_CONFIGURATION;
	private String adminUsername;
	private String adminPassword;
	private String computerName;
	private DomainJoin domainJoin;
	private WinRM winRM;

	@XmlAttribute(name = "type")
	public String getType() {
		return "WindowsProvisioningConfigurationSet";
	}

	public void setType(String type) {

	}

	@XmlElement(name = "ConfigurationSetType")
	public String getConfigurationSetType() {
		return configurationSetType;
	}

	@XmlElement(name = "AdminUsername")
	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(final String adminUsername) {
		this.adminUsername = adminUsername;
	}

	@XmlElement(name = "AdminPassword")
	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	@XmlElement(name = "ComputerName")
	public String getComputerName() {
		return computerName;
	}

	public void setComputerName(String computerName) {
		this.computerName = computerName;
	}

	@XmlElement(name = "WinRM")
	public WinRM getWinRM() {
		return winRM;
	}

	public void setWinRM(WinRM winRM) {
		this.winRM = winRM;
	}

	@XmlElement(name = "DomainJoin")
	public DomainJoin getDomainJoin() {
		return domainJoin;
	}

	public void setDomainJoin(DomainJoin domainJoin) {
		this.domainJoin = domainJoin;
	}
}
