package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LocalNetworkSite")
public class LocalNetworkSite {

	private String name;
	private String vpnGatewayAddress;
	private AddressSpace addressSpace;

	public LocalNetworkSite() {
	}

	public LocalNetworkSite(String name, String vpnGatewayAddress, AddressSpace addressSpace) {
		this.name = name;
		this.vpnGatewayAddress = vpnGatewayAddress;
		this.addressSpace = addressSpace;
	}

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "VPNGatewayAddress")
	public String getVpnGatewayAddress() {
		return vpnGatewayAddress;
	}

	public void setVpnGatewayAddress(String vpnGatewayAddress) {
		this.vpnGatewayAddress = vpnGatewayAddress;
	}

	@XmlElement(name = "AddressSpace")
	public AddressSpace getAddressSpace() {
		return addressSpace;
	}

	public void setAddressSpace(AddressSpace addressSpace) {
		this.addressSpace = addressSpace;
	}

}
