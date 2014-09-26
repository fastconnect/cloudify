package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "Subnets")
public class Subnets implements Iterable<Subnet> {

	private List<Subnet> subnets = new ArrayList<Subnet>();

	@Override
	public Iterator<Subnet> iterator() {
		return subnets.iterator();
	}

	@XmlElement(name = "Subnet")
	public List<Subnet> getSubnets() {
		return subnets;
	}

	public void setSubnets(List<Subnet> subnets) {
		this.subnets = subnets;
	}

	public boolean contains(String subnetName) {
		for (Subnet subnet : subnets) {
			if (subnet.getName().equals(subnetName)) {
				return true;
			}
		}
		return false;
	}

	public Subnet getSubnet(String subnetName) {
		for (Subnet subnet : subnets) {
			if (subnet.getName().equals(subnetName)) {
				return subnet;
			}
		}
		return null;
	}
}
