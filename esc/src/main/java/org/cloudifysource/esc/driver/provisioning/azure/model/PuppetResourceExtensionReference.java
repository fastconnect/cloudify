package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionReference")
public class PuppetResourceExtensionReference extends ResourceExtensionReference {

	private String key = "PuppetEnterpriseAgentPrivateConfigParameter";

	public PuppetResourceExtensionReference() {

	}

	public PuppetResourceExtensionReference(String puppetMasterServerEncoded) {

		setName("PuppetEnterpriseAgent");
		setPublisher("PuppetLabs");
		setReferenceName("PuppetEnterpriseAgent");
		setVersion("3.*");

		ResourceExtensionParameterValues resourceExtensionParameterValues = new ResourceExtensionParameterValues();

		ResourceExtensionParameterValue parameterValue = new ResourceExtensionParameterValue();
		parameterValue.setKey(key);
		parameterValue.setValue(puppetMasterServerEncoded);
		resourceExtensionParameterValues.setResourceExtensionParameterValues(Arrays.asList(parameterValue));
		setResourceExtensionParameterValues(resourceExtensionParameterValues);
	}
}
