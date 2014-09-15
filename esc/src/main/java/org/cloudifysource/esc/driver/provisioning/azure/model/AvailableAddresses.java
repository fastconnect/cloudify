package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Available addresses in a subnet
 *
 */
@XmlType(name = "AvailableAddresses", propOrder = { "availableAddresses" })
public class AvailableAddresses {

	private List<String> availableAddresses;

	@XmlElement(name = "AvailableAddress")
	public List<String> getAvailableAddresses() {
		return availableAddresses;
	}

	public void setAvailableAddresses(List<String> availableAddresses) {
		this.availableAddresses = availableAddresses;
	}

}
