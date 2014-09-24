package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "PersistentVMRole")
public class PersistentVMRole extends Role {

	@XmlTransient
	@Override
	public String getAvailabilitySetName() {
		return super.getAvailabilitySetName();
	}

}
