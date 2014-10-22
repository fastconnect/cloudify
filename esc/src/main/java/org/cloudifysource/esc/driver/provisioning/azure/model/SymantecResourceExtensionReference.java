package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionReference")
public class SymantecResourceExtensionReference extends ResourceExtensionReference {

	public SymantecResourceExtensionReference() {

		setName("SymantecEndpointProtection");
		setPublisher("Symantec");
		setReferenceName("SymantecEndpointProtection");
		setVersion("12.*");
	}
}
