package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType(name = "Role", propOrder = {
		"roleName", "roleType", "configurationSets", "resourceExtensionReferences", "availabilitySetName",
		"dataVirtualHardDisks", "osVirtualHardDisk", "roleSize", "provisionGuestAgent" })
public class Role {

	private String roleName;
	private String roleType;
	private ConfigurationSets configurationSets;
	private ResourceExtensionReferences resourceExtensionReferences;
	private String availabilitySetName;
	private DataVirtualHardDisks dataVirtualHardDisks;
	private OSVirtualHardDisk osVirtualHardDisk;
	private String roleSize;
	private Boolean provisionGuestAgent;

	@XmlElement(name = "RoleName")
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}

	@XmlElement(name = "RoleType")
	public String getRoleType() {
		return roleType;
	}

	public void setRoleType(final String roleType) {
		this.roleType = roleType;
	}

	@XmlElement(name = "ConfigurationSets")
	public ConfigurationSets getConfigurationSets() {
		return configurationSets;
	}

	public void setConfigurationSets(final ConfigurationSets configurationSets) {
		this.configurationSets = configurationSets;
	}

	@XmlElement(name = "OSVirtualHardDisk")
	public OSVirtualHardDisk getOsVirtualHardDisk() {
		return osVirtualHardDisk;
	}

	public void setOsVirtualHardDisk(OSVirtualHardDisk osVirtualHardDisk) {
		this.osVirtualHardDisk = osVirtualHardDisk;
	}

	@XmlElement(name = "RoleSize")
	public String getRoleSize() {
		return roleSize;
	}

	public void setRoleSize(final String roleSize) {
		this.roleSize = roleSize;
	}

	@XmlElement(name = "AvailabilitySetName")
	public String getAvailabilitySetName() {
		return availabilitySetName;
	}

	public void setAvailabilitySetName(final String availabilitySetName) {
		this.availabilitySetName = availabilitySetName;
	}

	@XmlElement(name = "ProvisionGuestAgent")
	public Boolean getProvisionGuestAgent() {
		return provisionGuestAgent;
	}

	public void setProvisionGuestAgent(Boolean provisionGuestAgent) {
		this.provisionGuestAgent = provisionGuestAgent;
	}

	@XmlElement(name = "DataVirtualHardDisks")
	public DataVirtualHardDisks getDataVirtualHardDisks() {
		return dataVirtualHardDisks;
	}

	public void setDataVirtualHardDisks(DataVirtualHardDisks dataVirtualHardDisks) {
		this.dataVirtualHardDisks = dataVirtualHardDisks;
	}

	@XmlElement(name = "ResourceExtensionReferences")
	public ResourceExtensionReferences getResourceExtensionReferences() {
		return resourceExtensionReferences;
	}

	public void setResourceExtensionReferences(ResourceExtensionReferences resourceExtensionReferences) {
		this.resourceExtensionReferences = resourceExtensionReferences;
	}

	/**
	 * Finds a ResourceExtensionReference by reference name
	 * 
	 * @param referenceName
	 * @return ResourceExtensionReference object if found, null otherwise
	 */
	public ResourceExtensionReference getResourceExtensionReferenceByName(String referenceName) {

		if (resourceExtensionReferences != null
				&& !resourceExtensionReferences.getResourceExtensionReferences().isEmpty()) {
			for (ResourceExtensionReference extensionReference : resourceExtensionReferences
					.getResourceExtensionReferences()) {
				if (extensionReference.getReferenceName().equals(referenceName)) {
					return extensionReference;
				}
			}
		}
		return null;
	}

	public boolean isDiskAttached(String diskName) {

		boolean isContains = false;
		if (this.dataVirtualHardDisks != null) {
			isContains = this.dataVirtualHardDisks.isContainsDataDisk(diskName);
		}

		return isContains;
	}

	public DataVirtualHardDisk getAttachedDataDiskByName(String diskName) {

		for (DataVirtualHardDisk disk : this.dataVirtualHardDisks) {

			if (disk.getDiskName().equals(diskName)) {
				return disk;
			}
		}
		return null;
	}

	public DataVirtualHardDisk getAttachedDataDiskByLun(int lun) {

		DataVirtualHardDisk disk = null;
		if (this.dataVirtualHardDisks != null) {
			disk = this.dataVirtualHardDisks.getDataDiskByLun(lun);
		}
		return disk;
	}

}
