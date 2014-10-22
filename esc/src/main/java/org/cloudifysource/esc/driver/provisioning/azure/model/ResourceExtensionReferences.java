package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionReferences")
public class ResourceExtensionReferences implements Iterable<ResourceExtensionReference> {

	private List<ResourceExtensionReference> resourceExtensionReferences = new ArrayList<ResourceExtensionReference>();

	@Override
	public Iterator<ResourceExtensionReference> iterator() {
		return getResourceExtensionReferences().iterator();
	}

	@XmlElement(name = "ResourceExtensionReference")
	public List<ResourceExtensionReference> getResourceExtensionReferences() {
		return resourceExtensionReferences;
	}

	public void setResourceExtensionReferences(List<ResourceExtensionReference> resourceExtensionReferences) {
		this.resourceExtensionReferences = resourceExtensionReferences;
	}

}
