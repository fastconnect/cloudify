package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "DataVirtualHardDisk")
@XmlType(name = "DataVirtualHardDisk", propOrder = { "hostCaching", "diskLabel", "diskName", "lun",
		"logicalDiskSizeInGB", "mediaLink" })
public class DataVirtualHardDisk {
	private String hostCaching;
	private String diskLabel;
	private String diskName;
	private int lun;
	private Integer logicalDiskSizeInGB;
	private String mediaLink;

	@XmlElement(name = "HostCaching")
	public String getHostCaching() {
		return hostCaching;
	}

	public void setHostCaching(String hostCaching) {
		this.hostCaching = hostCaching;
	}

	@XmlElement(name = "DiskLabel")
	public String getDiskLabel() {
		return diskLabel;
	}

	public void setDiskLabel(String diskLabel) {
		this.diskLabel = diskLabel;
	}

	@XmlElement(name = "DiskName")
	public String getDiskName() {
		return diskName;
	}

	public void setDiskName(String diskName) {
		this.diskName = diskName;
	}

	@XmlElement(name = "Lun")
	public int getLun() {
		return lun;
	}

	public void setLun(int lun) {
		this.lun = lun;
	}

	@XmlElement(name = "LogicalDiskSizeInGB")
	public Integer getLogicalDiskSizeInGB() {
		return logicalDiskSizeInGB;
	}

	public void setLogicalDiskSizeInGB(Integer logicalDiskSizeInGB) {
		this.logicalDiskSizeInGB = logicalDiskSizeInGB;
	}

	@XmlElement(name = "MediaLink")
	public String getMediaLink() {
		return mediaLink;
	}

	public void setMediaLink(String mediaLink) {
		this.mediaLink = mediaLink;
	}
}
