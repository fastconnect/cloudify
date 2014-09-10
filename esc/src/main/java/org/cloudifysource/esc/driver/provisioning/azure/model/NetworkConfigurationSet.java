package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */
@XmlType(propOrder = { "configurationSetType", "inputEndpoints" })
public class NetworkConfigurationSet extends ConfigurationSet {

	private String configurationSetType = ConfigurationSet.NETWORK_PROVISIONING_CONFIGURATION;
	private InputEndpoints inputEndpoints;
	private Subnets subnetNames;
	private String staticVirtualNetworkIPAddress;

	@XmlElement(name = "InputEndpoints")
	public InputEndpoints getInputEndpoints() {
		return inputEndpoints;
	}

	public void setInputEndpoints(final InputEndpoints inputEndpoints) {
		this.inputEndpoints = inputEndpoints;
	}

	@XmlElement(name = "ConfigurationSetType")
	public String getConfigurationSetType() {
		return configurationSetType;
	}

	public void setConfigurationSetType(final String configurationSetType) {
		this.configurationSetType = configurationSetType;
	}

	@XmlElement(name = "SubnetNames")
	public Subnets getSubnetNames() {
		return subnetNames;
	}

	public void setSubnetNames(Subnets subnetNames) {
		this.subnetNames = subnetNames;
	}

	@XmlElement(name = "StaticVirtualNetworkIPAddress")
	public String getStaticVirtualNetworkIPAddress() {
		return staticVirtualNetworkIPAddress;
	}

	public void setStaticVirtualNetworkIPAddress(String staticVirtualNetworkIPAddress) {
		this.staticVirtualNetworkIPAddress = staticVirtualNetworkIPAddress;
	}
}
