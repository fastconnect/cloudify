package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "AddressSpace")
public class AddressSpace {

	private List<String> addressPrefix = new ArrayList<String>();

	public AddressSpace() {
	}

	public AddressSpace(List<String> addressPrefix) {
		this.addressPrefix = addressPrefix;
	}

	@XmlElement(name = "AddressPrefix")
	public List<String> getAddressPrefix() {
		return addressPrefix;
	}

	public void setAddressPrefix(final List<String> addressPrefix) {
		this.addressPrefix = addressPrefix;
	}

}
