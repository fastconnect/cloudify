package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "VirtualNetworkSite", propOrder = { "addressSpace", "subnets", "dnsServersRef", "gateway" })
public class VirtualNetworkSite {

	private String location;
	private String name;
	private String affinityGroup;
	private AddressSpace addressSpace;
	private Subnets subnets;
	private DnsServersRef dnsServersRef;
	private Gateway gateway;

	@XmlAttribute(name = "Location")
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@XmlAttribute(name = "AffinityGroup")
	public String getAffinityGroup() {
		return affinityGroup;
	}

	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}

	@XmlElement(name = "AddressSpace")
	public AddressSpace getAddressSpace() {
		return addressSpace;
	}

	public void setAddressSpace(final AddressSpace addressSpace) {
		this.addressSpace = addressSpace;
	}

	@XmlElement(name = "Subnets")
	public Subnets getSubnets() {
		return subnets;
	}

	public void setSubnets(Subnets subnets) {
		this.subnets = subnets;
	}

	@XmlElement(name = "DnsServersRef")
	public DnsServersRef getDnsServersRef() {
		return dnsServersRef;
	}

	public void setDnsServersRef(DnsServersRef dnsServersRef) {
		this.dnsServersRef = dnsServersRef;
	}

	@XmlElement(name = "Gateway")
	public Gateway getGateway() {
		return gateway;
	}

	public void setGateway(Gateway gateway) {
		this.gateway = gateway;
	}

	/**
	 * Tests whether a subnet exist in a virtual network site
	 * 
	 * @param subnetName
	 *            .
	 * @return True if the subnet exists, false otherwise
	 */
	public boolean isSubnetExist(final String subnetName) {
		for (Subnet sub : this.getSubnets()) {
			if (sub.getName().equals(subnetName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds a LocalNetworkSiteRef by reference
	 * 
	 * @param networkReference
	 * @return LocalNetworkSiteRef object if found, null otherwise
	 */
	public LocalNetworkSiteRef getLocalNetworkSiteRef(String networkReference) {

		if (this.gateway != null) {
			ConnectionsToLocalNetwork connectionsToLocalNetwork = gateway.getConnectionsToLocalNetwork();
			if (connectionsToLocalNetwork != null && connectionsToLocalNetwork.getLocalNetworkSiteRefs() != null) {
				for (LocalNetworkSiteRef siteRef : connectionsToLocalNetwork.getLocalNetworkSiteRefs()) {
					if (siteRef.getName().equals(networkReference)) {
						return siteRef;
					}
				}
			}
		}

		return null;
	}

}
