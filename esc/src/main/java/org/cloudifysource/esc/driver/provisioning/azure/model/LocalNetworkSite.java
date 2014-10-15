package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LocalNetworkSite")
public class LocalNetworkSite {

	private String vpnGatewayAddress;
	private AddressSpace addressSpace;

	@XmlElement(name = "VPNGatewayAddress")
	public String getVpnGatewayAddress() {
		return vpnGatewayAddress;
	}

	public void setVpnGatewayAddress(String vpnGatewayAddress) {
		this.vpnGatewayAddress = vpnGatewayAddress;
	}

	@XmlElement(name = "AddressPrefix")
	public AddressSpace getAddressSpace() {
		return addressSpace;
	}

	public void setAddressSpace(AddressSpace addressSpace) {
		this.addressSpace = addressSpace;
	}

}
