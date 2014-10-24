/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved * * Licensed under the Apache License, Version
 * 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of
 * the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in
 * writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions
 * and * limitations under the License. *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.CustomScriptResourceExtensionReference;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.LinuxProvisioningConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Listener;
import org.cloudifysource.esc.driver.provisioning.azure.model.Listeners;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.OSVirtualHardDisk;
import org.cloudifysource.esc.driver.provisioning.azure.model.PersistentVMRole;
import org.cloudifysource.esc.driver.provisioning.azure.model.PuppetResourceExtensionReference;
import org.cloudifysource.esc.driver.provisioning.azure.model.ResourceExtensionReferences;
import org.cloudifysource.esc.driver.provisioning.azure.model.RestartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleList;
import org.cloudifysource.esc.driver.provisioning.azure.model.SubnetNames;
import org.cloudifysource.esc.driver.provisioning.azure.model.SymantecResourceExtensionReference;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;
import org.cloudifysource.esc.driver.provisioning.azure.model.WinRM;
import org.cloudifysource.esc.driver.provisioning.azure.model.WindowsProvisioningConfigurationSet;

import com.sun.jersey.core.util.Base64;

/*****************************************************************************************
 * this class is used for creating object instances representing the azure domain model.
 * 
 * @author elip
 * 
 ******************************************************************************************/

public class MicrosoftAzureRequestBodyBuilder {

	private static final String UTF_8 = "UTF-8";
	private static final int UUID_LENGTH = 8;

	private final static String EXTENSION_NAME = "name";
	private final static String EXTENSION_VALUE = "value";

	private final static String PUPPET_MASTER_SERVER_KEY = "PUPPET_MASTER_SERVER";
	private final static String EXTENSION_PUPPET_NAME = "puppet";
	private final static String EXTENSION_SYMANTEC_NAME = "symantec";
	private final static String EXTENSION_CUSTOM_SCRIPT_NAME = "customScript";

	private final static String EXTENSION_CUSTOM_SCRIPT_FILEURIS = "fileUris";
	private final static String EXTENSION_CUSTOM_SCRIPT_COMMAND_TO_EXECUTE = "commandToExecute";

	private final static String EXTENSION_CUSTOM_SCRIPT_STORAGE_ACCOUNT_KEY = "storageAccount";
	private final static String EXTENSION_CUSTOM_SCRIPT_STORAGE_CONTAINER_KEY = "container";
	private final static String EXTENSION_CUSTOM_SCRIPT_FILES_KEY = "files";
	private final static String EXTENSION_CUSTOM_SCRIPT_ARGUMENTS = "arguments";

	private String affinityPrefix;
	private String cloudServicePrefix;
	private String storagePrefix;
	private AtomicInteger cloudServiceAtomicInteger = new AtomicInteger(001);

	public MicrosoftAzureRequestBodyBuilder(final String affinityPrefix,
			final String cloudServicePrefix, final String storagePrefix) {
		this.affinityPrefix = affinityPrefix;
		this.cloudServicePrefix = cloudServicePrefix;
		this.storagePrefix = storagePrefix;
	}

	/**
	 * 
	 * @param name
	 *            - the affinity group name.
	 * @param location
	 *            - the affinity group location.
	 * @return - an object representing a body of the create affinity request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/gg715317.aspx" >Create Affinity
	 *         Group</a>
	 */

	public CreateAffinityGroup buildCreateAffinity(final String name, final String location) {

		CreateAffinityGroup affinityGroup = new CreateAffinityGroup();
		affinityGroup.setName(name);
		String affinityGroupName = affinityPrefix + generateRandomUUID(UUID_LENGTH);
		try {
			affinityGroup.setLabel(new String(Base64.encode(affinityGroupName), UTF_8));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		affinityGroup.setLocation(location);

		return affinityGroup;
	}

	/**
	 * 
	 * @param affinityGroup
	 *            - the affinity group to be associated with the cloud service
	 * @param cloudServiceNameOverride
	 * @return - an object representing a body of the create cloud service request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/gg441304.aspx" >Create Hosted
	 *         Service</a>
	 */
	public CreateHostedService buildCreateCloudService(final String affinityGroup, String cloudServiceNameOverride) {

		CreateHostedService hostedService = new CreateHostedService();
		hostedService.setAffinityGroup(affinityGroup);

		String name = null;
		String padding = null;

		// name will be generated
		if (cloudServiceNameOverride == null) {
			padding = String.format("%03d", cloudServiceAtomicInteger.getAndIncrement());
			name = cloudServicePrefix;

			// exact service name
		} else {
			padding = "";
			name = cloudServiceNameOverride;
		}

		try {
			hostedService.setLabel(new String(Base64.encode(name + padding), UTF_8));

		} catch (UnsupportedEncodingException e) {
			// ignore
		}

		hostedService.setServiceName(name + padding);
		return hostedService;
	}

	/**
	 * 
	 * @param addressPrefix
	 *            - CIDR notation address space range
	 * @param affinityGroupName
	 *            - the affinity group associated with the network
	 * @param networkName
	 *            - the name of the network to create.
	 * @return - an object representing a body of the set network configuration request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/jj157181.aspx" >Set Network
	 *         Configuration</a>
	 */
	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(
			final String addressPrefix, final String affinityGroupName,
			final String networkName) {

		GlobalNetworkConfiguration networkConfiguration = new GlobalNetworkConfiguration();

		VirtualNetworkConfiguration virtualNetworkConfiguration = new VirtualNetworkConfiguration();

		VirtualNetworkSites virtualNetworkSites = new VirtualNetworkSites();

		VirtualNetworkSite virtualNetworkSite = new VirtualNetworkSite();

		AddressSpace addressSpace = new AddressSpace();
		addressSpace.getAddressPrefix().add(addressPrefix);

		virtualNetworkSite.setAddressSpace(addressSpace);
		virtualNetworkSite.setAffinityGroup(affinityGroupName);
		virtualNetworkSite.setName(networkName);

		virtualNetworkSites.getVirtualNetworkSites().add(virtualNetworkSite);
		virtualNetworkConfiguration.setVirtualNetworkSites(virtualNetworkSites);
		networkConfiguration.setVirtualNetworkConfiguration(virtualNetworkConfiguration);

		return networkConfiguration;
	}

	/**
	 * 
	 * @param sites
	 *            - virtual network sites to deploy.
	 * @return - an object representing a body of the set network configuration request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/jj157181.aspx" >Set Network
	 *         Configuration</a>
	 */
	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(
			final List<VirtualNetworkSite> sites) {

		GlobalNetworkConfiguration networkConfiguration = new GlobalNetworkConfiguration();

		VirtualNetworkConfiguration virtualNetworkConfiguration = new VirtualNetworkConfiguration();

		VirtualNetworkSites virtualNetworkSites = new VirtualNetworkSites();

		virtualNetworkSites.setVirtualNetworkSites(sites);
		virtualNetworkConfiguration.setVirtualNetworkSites(virtualNetworkSites);
		networkConfiguration.setVirtualNetworkConfiguration(virtualNetworkConfiguration);

		return networkConfiguration;
	}

	public GlobalNetworkConfiguration buildGlobalNetworkConfiguration(VirtualNetworkConfiguration vnetConfiguration) {
		GlobalNetworkConfiguration networkConfiguration = new GlobalNetworkConfiguration();
		networkConfiguration.setVirtualNetworkConfiguration(vnetConfiguration);
		return networkConfiguration;
	}

	/**
	 * 
	 * @param desc
	 *            .
	 * @return - an object representing a body of the create virtual machine deployment request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/jj157194.aspx" >Create Virtual Machine
	 *         Deployment</a>
	 */
	public Deployment buildDeployment(final CreatePersistentVMRoleDeploymentDescriptor desc,
			final boolean isWindows) {

		String deploymentSlot = desc.getDeploymentSlot();
		String imageName = desc.getImageName();
		String storageAccountName = desc.getStorageAccountName();
		String userName = desc.getUserName();
		String password = desc.getPassword();
		String networkName = desc.getNetworkName();
		String size = desc.getSize();
		String deploymentName = desc.getDeploymentName();
		InputEndpoints endPoints = desc.getInputEndpoints();
		String roleName = desc.getRoleName();
		String customData = desc.getCustomData();

		Deployment deployment = new Deployment();
		deployment.setDeploymentSlot(deploymentSlot);
		deployment.setDeploymentName(roleName);
		deployment.setVirtualNetworkName(networkName);
		deployment.setLabel(deploymentName);
		deployment.setName(deploymentName);

		RoleList roleList = new RoleList();

		Role role = new Role();
		role.setRoleType("PersistentVMRole");
		role.setRoleName(roleName);
		role.setRoleSize(size);
		role.setProvisionGuestAgent(true);

		OSVirtualHardDisk osVirtualHardDisk = new OSVirtualHardDisk();
		osVirtualHardDisk.setSourceImageName(imageName);

		StringBuilder mediaLinkBuilder = new StringBuilder();
		mediaLinkBuilder.append("https://");
		mediaLinkBuilder.append(storageAccountName);
		mediaLinkBuilder.append(".blob.core.windows.net/vhds/");
		mediaLinkBuilder.append(desc.getHostedServiceName());
		mediaLinkBuilder.append("-");
		mediaLinkBuilder.append(role.getRoleName());
		mediaLinkBuilder.append("-");
		mediaLinkBuilder.append(generateRandomUUID(7));
		mediaLinkBuilder.append(".vhd");
		osVirtualHardDisk.setMediaLink(mediaLinkBuilder.toString());
		role.setOsVirtualHardDisk(osVirtualHardDisk);

		ConfigurationSets configurationSets = new ConfigurationSets();

		if (isWindows) {
			// Windows Specific : roleName de la forme cloudify_manager_roled57f => ROLED57F (15 car limit size limit)
			String[] computerNameArray = roleName.split("_");
			String computerName =
					(computerNameArray.length > 1 ? computerNameArray[2] : computerNameArray[0]).toUpperCase();

			WindowsProvisioningConfigurationSet windowsProvisioningSet = new WindowsProvisioningConfigurationSet();
			windowsProvisioningSet.setAdminUsername(userName);
			windowsProvisioningSet.setAdminPassword(password);
			windowsProvisioningSet.setComputerName(computerName); // (not optional) Windows ComputerName
			configurationSets.getConfigurationSets().add(windowsProvisioningSet);

			// Set WinRM : HTTP without Certificate
			WinRM winRM = new WinRM();
			Listeners listeners = new Listeners();

			Listener listener = new Listener();
			listener.setCertificateThumbprint(null); // Configure for Secure Winrm command (?)
			listener.setProtocol("Https");
			listeners.getListeners().add(listener);

			winRM.setListeners(listeners);
			windowsProvisioningSet.setWinRM(winRM);

			if (StringUtils.isNotBlank(customData)) {
				try {
					windowsProvisioningSet.setCustomData(new String(Base64.encode(customData), UTF_8));
				} catch (UnsupportedEncodingException e) {
					// ignore
				}
			}

			// domain join (windows only)
			windowsProvisioningSet.setDomainJoin(desc.getDomainJoin());
		}
		else {
			LinuxProvisioningConfigurationSet linuxProvisioningSet = new LinuxProvisioningConfigurationSet();
			// linuxProvisioningSet.setDisableSshPasswordAuthentication(true);
			linuxProvisioningSet.setHostName(roleName);
			linuxProvisioningSet.setUserName(userName);
			linuxProvisioningSet.setUserPassword(password);
			if (StringUtils.isNotBlank(customData)) {
				try {
					linuxProvisioningSet.setCustomData(new String(Base64.encode(customData), UTF_8));
				} catch (UnsupportedEncodingException e) {
					// ignore
				}
			}
			configurationSets.getConfigurationSets().add(linuxProvisioningSet);
		}

		// availability set
		role.setAvailabilitySetName(desc.getAvailabilitySetName());

		// extensions
		role.setResourceExtensionReferences(desc.getExtensionReferences());

		// NetworkConfiguration
		NetworkConfigurationSet networkConfiguration = new NetworkConfigurationSet();

		// static ip configuration
		// TODO verify subnet availability
		SubnetNames subnetNames = new SubnetNames();
		subnetNames.getSubnets().add(desc.getSubnetName());
		networkConfiguration.setSubnetNames(subnetNames);

		String ip = desc.getAvailableIp();
		networkConfiguration.setStaticVirtualNetworkIPAddress(ip);

		networkConfiguration.setInputEndpoints(endPoints);
		configurationSets.getConfigurationSets().add(networkConfiguration);
		role.setConfigurationSets(configurationSets);
		roleList.getRoles().add(role);
		deployment.setRoleList(roleList);

		return deployment;
	}

	/**
	 * 
	 * @param affinityGroupName
	 *            - the affinity group associated with this storage account.
	 * @param storageAccountName
	 *            - the storage account name.
	 * 
	 * @return - an object representing a body of the create storage service input request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/hh264518.aspx" >Create Storage
	 *         Service</a>
	 */
	public CreateStorageServiceInput buildCreateStorageAccount(
			final String affinityGroupName, final String storageAccountName) {

		CreateStorageServiceInput storageAccount = new CreateStorageServiceInput();
		storageAccount.setAffinityGroup(affinityGroupName);
		try {
			storageAccount.setLabel(new String(Base64.encode(storagePrefix), UTF_8));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		storageAccount.setServiceName(storageAccountName);

		return storageAccount;
	}

	/**
	 * 
	 * @return - an object representing a body of the restart role request. <br>
	 *         see <a href= "http://msdn.microsoft.com/en-us/library/windowsazure/jj157197" >Restart Role</a>
	 */
	public RestartRoleOperation buildRestartRoleOperation() {
		return new RestartRoleOperation();
	}

	private static String generateRandomUUID(final int length) {
		return UUIDHelper.generateRandomUUID(length);
	}

	/**
	 * Build a PersistentVMRole from the deploymentDescriptor
	 * 
	 * @param deplyomentDesc
	 * @param isWindows
	 * @return PersistentVMRole
	 */
	public PersistentVMRole buildPersistentVMRole(CreatePersistentVMRoleDeploymentDescriptor deplyomentDesc,
			boolean isWindows) {

		Deployment deployment = this.buildDeployment(deplyomentDesc, isWindows);
		Role role = deployment.getRoleList().getRoles().get(0);

		PersistentVMRole persistentVMRole = new PersistentVMRole();
		persistentVMRole.setAvailabilitySetName(role.getAvailabilitySetName());
		persistentVMRole.setConfigurationSets(role.getConfigurationSets());
		persistentVMRole.setOsVirtualHardDisk(role.getOsVirtualHardDisk());

		persistentVMRole.setRoleName(role.getRoleName());
		persistentVMRole.setRoleSize(role.getRoleSize());
		persistentVMRole.setRoleType(role.getRoleType());
		return persistentVMRole;
	}

	public ResourceExtensionReferences buildResourceExtensionReferences(List<Map<String, String>> extensions,
			boolean isWindows) {

		ResourceExtensionReferences extensionReferences = new ResourceExtensionReferences();

		if (extensions != null && !extensions.isEmpty()) {

			// windows support only
			if (isWindows) {

				for (Map<String, String> extentionMap : extensions) {
					String extensionName = extentionMap.get(EXTENSION_NAME);
					String extensionValue = extentionMap.get(EXTENSION_VALUE);
					if (StringUtils.isNotBlank(extensionName)) {

						// puppet, value is required
						if (StringUtils.isNotBlank(extensionValue)) {

							if (extensionName.equals(EXTENSION_PUPPET_NAME)) {
								PuppetResourceExtensionReference puppetReference =
										this.buildPuppetResourceExtensionReference(extensionValue);
								extensionReferences.getResourceExtensionReferences().add(puppetReference);
							}
						}

						// symantec,value isn't required
						if (extensionName.equals(EXTENSION_SYMANTEC_NAME)) {
							SymantecResourceExtensionReference symantecReference =
									new SymantecResourceExtensionReference();

							extensionReferences.getResourceExtensionReferences().add(symantecReference);
						}

						// custom script
						if (extensionName.equals(EXTENSION_CUSTOM_SCRIPT_NAME)) {
							CustomScriptResourceExtensionReference customScriptReference =
									buildCustomScriptResourceExtensionReference(extentionMap);

							extensionReferences.getResourceExtensionReferences().add(customScriptReference);
						}
					}
				}
			}
		}
		return extensionReferences;
	}

	private PuppetResourceExtensionReference buildPuppetResourceExtensionReference(String puppetMasterServer) {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(PUPPET_MASTER_SERVER_KEY, puppetMasterServer);
		String jsonValueEncoded = this.getBase64String(jsonObject.toString());
		PuppetResourceExtensionReference puppetRef = new PuppetResourceExtensionReference(jsonValueEncoded);
		return puppetRef;
	}

	private CustomScriptResourceExtensionReference buildCustomScriptResourceExtensionReference(
			Map<String, String> extentionMap) {

		CustomScriptResourceExtensionReference customScriptResourceExtensionReference = null;

		String storageAccount = extentionMap.get(EXTENSION_CUSTOM_SCRIPT_STORAGE_ACCOUNT_KEY);
		String container = extentionMap.get(EXTENSION_CUSTOM_SCRIPT_STORAGE_CONTAINER_KEY);
		String files = extentionMap.get(EXTENSION_CUSTOM_SCRIPT_FILES_KEY);
		String arguments = extentionMap.get(EXTENSION_CUSTOM_SCRIPT_ARGUMENTS);

		if (StringUtils.isNotBlank(storageAccount) && StringUtils.isNotBlank(container)
				&& StringUtils.isNotBlank(files)) {

			// file URI
			JSONObject jsonObject = new JSONObject();
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("https://");
			stringBuilder.append(storageAccount);
			stringBuilder.append(".blob.core.windows.net/");
			stringBuilder.append(container);
			stringBuilder.append("/");
			stringBuilder.append(files);

			List<String> uriList = Arrays.asList(stringBuilder.toString());
			jsonObject.put(EXTENSION_CUSTOM_SCRIPT_FILEURIS, uriList);

			// reset builder
			stringBuilder.setLength(0);
			stringBuilder.append("powershell -ExecutionPolicy Unrestricted ");
			stringBuilder.append("-file ");
			stringBuilder.append(files);

			// script arguments
			if (StringUtils.isNotBlank(arguments)) {
				stringBuilder.append(" ");
				stringBuilder.append(arguments);
			}

			jsonObject.put(EXTENSION_CUSTOM_SCRIPT_COMMAND_TO_EXECUTE, stringBuilder.toString());
			String jsonValueEncoded = this.getBase64String(jsonObject.toString());

			customScriptResourceExtensionReference = new CustomScriptResourceExtensionReference(null, jsonValueEncoded);
		}

		return customScriptResourceExtensionReference;
	}

	public String getBase64String(String string) {

		String base64String = null;
		if (string != null) {
			try {
				base64String = new String(Base64.encode(string), UTF_8);
			} catch (UnsupportedEncodingException e) {
			}
		}
		return base64String;
	}
}
