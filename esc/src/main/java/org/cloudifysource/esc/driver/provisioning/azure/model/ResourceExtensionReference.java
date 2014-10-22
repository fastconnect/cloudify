package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "referenceName", "publisher", "name", "version",
		"resourceExtensionParameterValues", "state" })
public class ResourceExtensionReference {

	private String referenceName;
	private String publisher;
	private String name;
	private String version;
	private ResourceExtensionParameterValues resourceExtensionParameterValues;
	private String state = "Enable";

	public ResourceExtensionReference() {
	}

	public ResourceExtensionReference(String referenceName, String publisher, String name, String version,
			ResourceExtensionParameterValues resourceExtensionParameterValues, String state) {
		this.referenceName = referenceName;
		this.publisher = publisher;
		this.name = name;
		this.version = version;
		this.resourceExtensionParameterValues = resourceExtensionParameterValues;
		this.state = state;
	}

	@XmlElement(name = "ReferenceName")
	public String getReferenceName() {
		return referenceName;
	}

	public void setReferenceName(String referenceName) {
		this.referenceName = referenceName;
	}

	@XmlElement(name = "Publisher")
	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "Version")
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@XmlElement(name = "ResourceExtensionParameterValues")
	public ResourceExtensionParameterValues getResourceExtensionParameterValues() {
		return resourceExtensionParameterValues;
	}

	public void setResourceExtensionParameterValues(ResourceExtensionParameterValues resourceExtensionParameterValues) {
		this.resourceExtensionParameterValues = resourceExtensionParameterValues;
	}

	@XmlElement(name = "State")
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
