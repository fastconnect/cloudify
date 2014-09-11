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
@XmlType(name = "SubnetsNames")
public class Subnets implements Iterable<String> {

	private List<String> subnets = new ArrayList<String>();

	@Override
	public Iterator<String> iterator() {
		return subnets.iterator();
	}

	@XmlElement(name = "SubnetName")
	public List<String> getSubnets() {
		return subnets;
	}

	public void setSubnets(List<String> subnets) {
		this.subnets = subnets;
	}
}
