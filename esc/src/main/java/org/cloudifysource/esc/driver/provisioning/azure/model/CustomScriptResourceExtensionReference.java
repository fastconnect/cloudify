package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.StringUtils;

@XmlType(name = "ResourceExtensionReference")
public class CustomScriptResourceExtensionReference extends ResourceExtensionReference {

	private final String privateKey = "CustomScriptExtensionPrivateConfigParameter";
	private final String publicKey = "CustomScriptExtensionPublicConfigParameter";

	public CustomScriptResourceExtensionReference() {
	}

	public CustomScriptResourceExtensionReference(String privateValue, String publicValue) {

		setName("CustomScriptExtension");
		setPublisher("Microsoft.Compute");
		setReferenceName("CustomScriptExtension");
		setVersion("1.*");

		ResourceExtensionParameterValues resourceExtensionParameterValues = new ResourceExtensionParameterValues();

		if (StringUtils.isNotBlank(privateValue)) {
			ResourceExtensionParameterValue privateParameterValue = new ResourceExtensionParameterValue();
			privateParameterValue.setKey(privateKey);
			privateParameterValue.setValue(privateValue);
			privateParameterValue.setType("Private");
			resourceExtensionParameterValues.getResourceExtensionParameterValues().add(privateParameterValue);
		}

		if (StringUtils.isNotBlank(publicValue)) {
			ResourceExtensionParameterValue publicParameterValue = new ResourceExtensionParameterValue();
			publicParameterValue.setKey(publicKey);
			publicParameterValue.setValue(publicValue);
			publicParameterValue.setType("Public");
			resourceExtensionParameterValues.getResourceExtensionParameterValues().add(publicParameterValue);
		}

		setResourceExtensionParameterValues(resourceExtensionParameterValues);
	}
}
