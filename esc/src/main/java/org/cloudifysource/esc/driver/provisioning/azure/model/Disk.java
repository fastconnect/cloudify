/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author elip
 * 
 */
@XmlRootElement(name = "Disk")
@XmlType(propOrder = { "os", "label", "affinityGroup", "attachedTo", "mediaLink", "name" })
public class Disk {

	private String os;
	private String label;
	private String affinityGroup;
	private AttachedTo attachedTo;
	private String name;
	private String mediaLink;

	@XmlElement(name = "OS")
	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	@XmlElement(name = "Label")
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@XmlElement(name = "AffinityGroup")
	public String getAffinityGroup() {
		return affinityGroup;
	}

	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}

	@XmlElement(name = "AttachedTo")
	public AttachedTo getAttachedTo() {
		return attachedTo;
	}

	public void setAttachedTo(final AttachedTo attachedTo) {
		this.attachedTo = attachedTo;
	}

	@XmlElement(name = "MediaLink")
	public String getMediaLink() {
		return mediaLink;
	}

	public void setMediaLink(String mediaLink) {
		this.mediaLink = mediaLink;
	}

	@XmlElement(name = "Name")
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

}
