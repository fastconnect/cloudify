package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents response of testing an (private) IP availability
 */
@XmlRootElement(name = "AddressAvailabilityResponse")
@XmlType(propOrder = { "isAvailable", "availableAddresses" })
public class AddressAvailability {

	private boolean isAvailable;
	private AvailableAddresses availableAddresses;

	@XmlElement(name = "IsAvailable")
	public boolean isIsAvailable() {
		return isAvailable;
	}

	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	@XmlElement(name = "AvailableAddresses")
	public AvailableAddresses getAvailableAddresses() {
		return availableAddresses;
	}

	public void setAvailableAddresses(AvailableAddresses availableAddresses) {
		this.availableAddresses = availableAddresses;
	}

}
