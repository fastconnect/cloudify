package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */

@XmlType(name = "VirtualNetworkConfiguration", propOrder = { "virtualNetworkSites" })
public class VirtualNetworkConfiguration {

	private VirtualNetworkSites virtualNetworkSites;

	@XmlElement(name = "VirtualNetworkSites")
	public VirtualNetworkSites getVirtualNetworkSites() {
		return virtualNetworkSites;
	}

	public void setVirtualNetworkSites(final VirtualNetworkSites virtualNetworkSites) {
		this.virtualNetworkSites = virtualNetworkSites;
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
}
