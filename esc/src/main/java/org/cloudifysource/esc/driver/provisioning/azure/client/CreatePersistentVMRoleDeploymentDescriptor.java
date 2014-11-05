/**
 *
 */
package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.util.List;
import java.util.Map;

import org.cloudifysource.esc.driver.provisioning.azure.model.DomainJoin;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.ResourceExtensionReferences;

/************************************************************************************************
 * * A POJO holding all necessary properties for create a new vm to an existing virtual network. * *
 * 
 * @author elip * *
 ************************************************************************************************/

public class CreatePersistentVMRoleDeploymentDescriptor {

	private String deploymentName;
	private String deploymentSlot;
	private String imageName;
	private String storageAccountName;
	private String osStorageAccountName;
	private String userName;
	private String password;
	private String size;
	private String networkName;
	private String availabilitySetName;
	private String roleName;
	private String affinityGroup;
	private String hostedServiceName;
	private List<String> ipAddresses;
	private String availableIp = null;
	private String subnetName;
	private DomainJoin domainJoin;
	private String customData;
	private Integer dataDiskSize;

	private List<String> dataStorageAccounts;

	private boolean generateCloudServiceName;

	private List<Map<String, String>> extensions;
	private ResourceExtensionReferences extensionReferences = new ResourceExtensionReferences();

	public String getHostedServiceName() {
		return hostedServiceName;
	}

	public void setHostedServiceName(final String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}

	public String getAffinityGroup() {
		return affinityGroup;
	}

	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName;
	}

	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(final String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public String getAvailabilitySetName() {
		return availabilitySetName;
	}

	public void setAvailabilitySetName(final String availabilitySetName) {
		this.availabilitySetName = availabilitySetName;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(final String networkName) {
		this.networkName = networkName;
	}

	private InputEndpoints inputEndpoints;

	public String getRoleName() {
		return roleName;
	}

	public String getDeploymentSlot() {
		return deploymentSlot;
	}

	public void setDeploymentSlot(final String deploymentSlot) {
		this.deploymentSlot = deploymentSlot;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(final String imageName) {
		this.imageName = imageName;
	}

	public String getStorageAccountName() {
		return storageAccountName;
	}

	public void setStorageAccountName(final String storageAccountName) {
		this.storageAccountName = storageAccountName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getSize() {
		return size;
	}

	public void setSize(final String size) {
		this.size = size;
	}

	public InputEndpoints getInputEndpoints() {
		return inputEndpoints;
	}

	public void setInputEndpoints(final InputEndpoints inputEndpoints) {
		this.inputEndpoints = inputEndpoints;
	}

	@Override
	public String toString() {
		return "CreatePersistentVMRoleDeploymentDescriptor [deploymentName="
				+ deploymentName + ", deploymentSlot=" + deploymentSlot
				+ ", imageName=" + imageName + ", storageAccountName="
				+ storageAccountName + ", size=" + size + ", networkName="
				+ networkName + ", availabilitySetName=" + availabilitySetName
				+ ", roleName=" + roleName + ", affinityGroup=" + affinityGroup
				+ ", hostedServiceName=" + hostedServiceName
				+ ", inputEndpoints=" + inputEndpoints + "]";
	}

	public List<String> getIpAddresses() {
		return ipAddresses;
	}

	public void setIpAddresses(List<String> ipAddresses) {
		this.ipAddresses = ipAddresses;
	}

	public String getSubnetName() {
		return subnetName;
	}

	public void setSubnetName(String subnetName) {
		this.subnetName = subnetName;
	}

	public String getAvailableIp() {
		return availableIp;
	}

	public void setAvailableIp(String availableIp) {
		this.availableIp = availableIp;
	}

	public void setCustomData(String customData) {
		this.customData = customData;
	}

	public String getCustomData() {
		return customData;
	}

	public Integer getDataDiskSize() {
		return dataDiskSize;
	}

	public void setDataDiskSize(int dataDiskSize) {
		this.dataDiskSize = dataDiskSize;
	}

	public DomainJoin getDomainJoin() {
		return domainJoin;
	}

	public void setDomainJoin(DomainJoin domainJoin) {
		this.domainJoin = domainJoin;
	}

	public boolean isGenerateCloudServiceName() {
		return generateCloudServiceName;
	}

	public void setGenerateCloudServiceName(boolean generateCloudServiceName) {
		this.generateCloudServiceName = generateCloudServiceName;
	}

	public ResourceExtensionReferences getExtensionReferences() {
		return extensionReferences;
	}

	public void setExtensionReferences(ResourceExtensionReferences extensionReferences) {
		this.extensionReferences = extensionReferences;
	}

	public List<Map<String, String>> getExtensions() {
		return extensions;
	}

	public void setExtensions(List<Map<String, String>> extensions) {
		this.extensions = extensions;
	}

	public List<String> getDataStorageAccounts() {
		return dataStorageAccounts;
	}

	public void setDataStorageAccounts(List<String> dataStorageAccounts) {
		this.dataStorageAccounts = dataStorageAccounts;
	}

	public String getOsStorageAccountName() {
		return osStorageAccountName;
	}

	public void setOsStorageAccountName(String osStorageAccountName) {
		this.osStorageAccountName = osStorageAccountName;
	}

}
