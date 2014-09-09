package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "VirtualNetworkSite", propOrder = { "addressSpace", "subnets" })
public class VirtualNetworkSite {

	private String name;
	private String affinityGroup;
	private AddressSpace addressSpace;
	private Subnets subnets;

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

}
