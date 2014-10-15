package org.cloudifysource.esc.driver.provisioning.azure.model;

/**
 * basic vpn configuration object
 *
 */
public class VpnConfiguration {

	private LocalNetworkSites localNetworkSites;
	private Subnet subnet;
	private Gateway gateway;

	public VpnConfiguration(LocalNetworkSites localNetworkSites, Subnet subnet, Gateway gateway) {
		this.localNetworkSites = localNetworkSites;
		this.subnet = subnet;
		this.gateway = gateway;
	}

	public VpnConfiguration() {
	}

	public LocalNetworkSites getLocalNetworkSites() {
		return localNetworkSites;
	}

	public void setLocalNetworkSites(LocalNetworkSites localNetworkSites) {
		this.localNetworkSites = localNetworkSites;
	}

	public Subnet getSubnet() {
		return subnet;
	}

	public void setSubnet(Subnet subnet) {
		this.subnet = subnet;
	}

	public Gateway getGateway() {
		return gateway;
	}

	public void setGateway(Gateway gateway) {
		this.gateway = gateway;
	}

}
