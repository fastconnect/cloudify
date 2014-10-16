package org.cloudifysource.esc.driver.provisioning.azure.model;

/**
 * basic vpn configuration object
 *
 */
public class VpnConfiguration {

	private LocalNetworkSites localNetworkSites;
	private Subnet subnet;
	private Gateway gateway;
	private String gatewayType = "DynamicRouting";
	private String gatewaykey;

	public VpnConfiguration() {
	}

	public VpnConfiguration(LocalNetworkSites localNetworkSites, Subnet subnet, Gateway gateway, String gatewayType,
			String gatewaykey) {
		this.localNetworkSites = localNetworkSites;
		this.subnet = subnet;
		this.gateway = gateway;
		this.gatewayType = gatewayType;
		this.setGatewaykey(gatewaykey);
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

	public String getGatewayType() {
		return gatewayType;
	}

	public void setGatewayType(String gatewayType) {
		this.gatewayType = gatewayType;
	}

	public String getGatewaykey() {
		return gatewaykey;
	}

	public void setGatewaykey(String gatewaykey) {
		this.gatewaykey = gatewaykey;
	}

}
