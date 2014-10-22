package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionParameterValues")
public class ResourceExtensionParameterValues implements Iterable<ResourceExtensionParameterValue> {

	private List<ResourceExtensionParameterValue> resourceExtensionParameterValues;

	@Override
	public Iterator<ResourceExtensionParameterValue> iterator() {
		return getResourceExtensionParameterValues().iterator();
	}

	@XmlElement(name = "ResourceExtensionParameterValue")
	public List<ResourceExtensionParameterValue> getResourceExtensionParameterValues() {
		return resourceExtensionParameterValues;
	}

	public void setResourceExtensionParameterValues(
			List<ResourceExtensionParameterValue> resourceExtensionParameterValues) {
		this.resourceExtensionParameterValues = resourceExtensionParameterValues;
	}

}
