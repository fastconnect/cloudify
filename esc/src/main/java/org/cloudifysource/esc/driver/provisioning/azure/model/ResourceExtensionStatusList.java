package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ResourceExtensionStatusList")
public class ResourceExtensionStatusList implements Iterable<ResourceExtensionStatus> {

	private List<ResourceExtensionStatus> resourceExtensionStatusList = new ArrayList<ResourceExtensionStatus>();

	@Override
	public Iterator<ResourceExtensionStatus> iterator() {
		return getResourceExtensionStatusList().iterator();
	}

	@XmlElement(name = "ResourceExtensionStatus")
	public List<ResourceExtensionStatus> getResourceExtensionStatusList() {
		return resourceExtensionStatusList;
	}

	public void setResourceExtensionStatusList(List<ResourceExtensionStatus> resourceExtensionStatusList) {
		this.resourceExtensionStatusList = resourceExtensionStatusList;
	}

}
