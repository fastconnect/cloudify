package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Subnet")
public class Subnet {
	private String name;
	private List<String> addressPrefix = new ArrayList<String>();

	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "AddressPrefix")
	public List<String> getAddressPrefix() {
		return addressPrefix;
	}

	public void setAddressPrefix(List<String> addressPrefix) {
		this.addressPrefix = addressPrefix;
	}

}
