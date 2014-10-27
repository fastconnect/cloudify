package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionReference")
public class BGInfoResourceExtensionReference extends ResourceExtensionReference {

	public BGInfoResourceExtensionReference() {
		setName("BGInfo");
		setPublisher("Microsoft.Compute");
		setReferenceName("BGInfo");
		setVersion("1.*");
	}

}
