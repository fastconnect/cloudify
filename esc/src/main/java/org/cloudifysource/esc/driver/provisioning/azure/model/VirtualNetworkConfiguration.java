package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */

@XmlType(name = "VirtualNetworkConfiguration", propOrder = { "dns", "localNetworkSites", "virtualNetworkSites" })
public class VirtualNetworkConfiguration {

	private Dns dns;
	private LocalNetworkSites localNetworkSites;
	private VirtualNetworkSites virtualNetworkSites;

	@XmlElement(name = "Dns")
	public Dns getDns() {
		return dns;
	}

	public void setDns(Dns dns) {
		this.dns = dns;
	}

	@XmlElement(name = "VirtualNetworkSites")
	public VirtualNetworkSites getVirtualNetworkSites() {
		return virtualNetworkSites;
	}

	public void setVirtualNetworkSites(final VirtualNetworkSites virtualNetworkSites) {
		this.virtualNetworkSites = virtualNetworkSites;
	}

	@XmlElement(name = "LocalNetworkSites")
	public LocalNetworkSites getLocalNetworkSites() {
		return localNetworkSites;
	}

	public void setLocalNetworkSites(LocalNetworkSites localNetworkSites) {
		this.localNetworkSites = localNetworkSites;
	}

	/**
	 * Get the network site configuration specified with networkName
	 * 
	 * @param networkName
	 * @return VirtualNetworkSite object if found, null otherwise.
	 */
	public VirtualNetworkSite getVirtualNetworkSiteConfigurationByName(String networkName) {

		VirtualNetworkSite virtualNetworkSite = null;

		for (VirtualNetworkSite vns : this.getVirtualNetworkSites()) {
			if (vns.getName().equals(networkName)) {
				return virtualNetworkSite = vns;
			}
		}

		return virtualNetworkSite;
	}

	/**
	 * Get the local network site configuration specified with networkName
	 * 
	 * @param localNetworkSiteName
	 * @return LocalNetworkSite object if found, null otherwise.
	 */
	public LocalNetworkSite getLocalNetworkSiteConfigurationByName(String localNetworkSiteName) {

		LocalNetworkSite localNetworkSite = null;

		for (LocalNetworkSite lns : this.getLocalNetworkSites()) {
			if (lns.getName().equals(localNetworkSiteName)) {
				return localNetworkSite = lns;
			}
		}

		return localNetworkSite;
	}
}
