/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.RemoteExecutionModes;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.domain.cloud.network.Subnet;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.azure.client.CreatePersistentVMRoleDeploymentDescriptor;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.client.RoleDetails;
import org.cloudifysource.esc.driver.provisioning.azure.model.AttachedTo;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.DomainJoin;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.JoinCredentials;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutor;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutorFactory;
import org.cloudifysource.esc.util.Utils;

/***************************************************************************************
 * A custom Cloud Driver implementation for provisioning machines on Azure.
 *
 * @author elip
 ***************************************************************************************/
public class MicrosoftAzureCloudDriver extends BaseProvisioningDriver {

	// TODO set dynamic value for manager name if necessary
	protected static final String CLOUDIFY_MANAGER_NAME = "CFYM";

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";

	// Custom template DSL properties
	private static final String AZURE_PFX_FILE = "azure.pfx.file";
	private static final String AZURE_PFX_PASSWORD = "azure.pfx.password";
	private static final String AZURE_ENDPOINTS = "azure.endpoints";
	private static final String AZURE_FIREWALL_PORTS = "azure.firewall.ports";

	private static final String AZURE_CLOUD_SERVICE = "azure.cloud.service";
	private static final String VM_IP_ADDRESSES = "azure.network.ipAddresses";
	private static final String AZURE_DOMAIN_JOIN = "azure.domain.join";

	// Custom cloud DSL properties
	private static final String AZURE_WIRE_LOG = "azure.wireLog";
	private static final String AZURE_DEPLOYMENT_SLOT = "azure.deployment.slot";
	private static final String AZURE_DEPLOYMENT_CUSTOMDATA = "azure.deployment.customdata";
	private static final String AZURE_AFFINITY_LOCATION = "azure.affinity.location";
	private static final String AZURE_AFFINITY_GROUP = "azure.affinity.group";
	private static final String AZURE_STORAGE_ACCOUNT = "azure.storage.account";

	/**
	 * Optional. If set, the driver will create and attack a data disk with the defined size to the new VM.
	 */
	private static final String AZURE_STORAGE_DATADISK_SIZE = "azure.storage.datadisk.size";

	private static final String AZURE_AVAILABILITY_SET = "azure.availability.set";
	private static final String AZURE_CLEANUP_ON_TEARDOWN = "azure.cleanup.on.teardown";

	// Networks properties
	private static final String AZURE_NETWORK_NAME = "azure.networksite.name";
	private static final String AZURE_NETWORK_ADDRESS_SPACE = "azure.address.space";
	private static final String AZURE_DNS_SERVERS = "azure.dns.servers";

	private static final String AZURE_CLOUD_SERVICE_CODE = "azure.cloud.service.code";

	private static String cloudServicePrefix = "cloudifycloudservice";

	private AtomicInteger availabilitySetCounter = new AtomicInteger(1);
	private AtomicInteger serviceCounter = new AtomicInteger(1);

	private boolean cleanup;

	// Azure Credentials
	private String subscriptionId;

	// Arguments for all machines
	private String location;
	private String addressSpace;
	private String networkName;
	private String affinityGroup;
	private String storageAccountName;
	private Map<String, String> dnsServers;

	// Arguments per template
	private String deploymentSlot;
	private String deploymentCustomData;
	private String imageName;
	private String userName;
	private String password;
	private String size;
	private String pathToPfxFile;
	private String pfxPassword;
	private String availabilitySet;
	private Integer dataDiskSize;

	private FileTransferModes fileTransferMode;
	private RemoteExecutionModes remoteExecutionMode;
	private ScriptLanguages scriptLanguage;

	private List<Map<String, String>> firewallPorts;

	private static final int WEBUI_PORT = 8099;
	private static final int REST_PORT = 8100;

	private static final String DOMAIN = "domain";
	private static final String DOMAIN_USERNAME = "userName";
	private static final String DOMAIN_PASSWORD = "password";
	private static final String JOIN_DOMAIN = "joinDomain";

	// Commands template
	private static final String COMMAND_OPEN_FIREWALL_PORT = "netsh advfirewall firewall"
			+ " add rule name=\"%s\" dir=in action=allow protocol=%s localport=%d";
	private static final String COMMAND_ACTIVATE_SHARING = "netsh advfirewall firewall"
			+ " set rule group=\\\"File and Printer Sharing\\\" new enable=yes";
	private static final long DEFAULT_COMMAND_TIMEOUT = 15 * 60 * 1000; // 2 minutes

	private static final int DEFAULT_STOP_MANAGEMENT_TIMEOUT = 15;

	private int stopManagementMachinesTimeoutInMinutes = DEFAULT_STOP_MANAGEMENT_TIMEOUT;
	private ComputeTemplate template;

	private static final Logger logger = Logger.getLogger(MicrosoftAzureCloudDriver.class.getName());
	private static final long CLEANUP_TIMEOUT = 60 * 1000 * 5; // five minutes

	private static MicrosoftAzureRestClient azureClient;

	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();

	public MicrosoftAzureCloudDriver() {
	}

	private static synchronized void initRestClient(
			final String subscriptionId, final String pathToPfxFile,
			final String pfxPassword, final boolean enableWireLog) {
		if (azureClient == null) {
			logger.fine("Initializing Azure REST client");
			azureClient = new MicrosoftAzureRestClient(subscriptionId,
					pathToPfxFile, pfxPassword, CLOUDIFY_AFFINITY_PREFIX,
					cloudServicePrefix, CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
			if (enableWireLog) {
				azureClient.setLoggingFilter(logger);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setConfig(ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		super.setConfig(configuration);

		this.verifyManagementNetworkConfiguration(configuration.getCloud());

		this.stopManagementMachinesTimeoutInMinutes = Utils.getInteger(cloud.getCustom().get(CloudifyConstants
				.STOP_MANAGEMENT_TIMEOUT_IN_MINUTES), DEFAULT_STOP_MANAGEMENT_TIMEOUT);

		// null value for code causes problem while deploying, therefore it is set to an empty string if it's the case
		String availability = (String) this.template.getCustom().get(AZURE_AVAILABILITY_SET);
		if (StringUtils.isNotBlank(availability)) {
			this.availabilitySet = ((String) this.template.getCustom().get(AZURE_AVAILABILITY_SET)).trim();
		}

		this.deploymentSlot = (String) this.template.getCustom().get(AZURE_DEPLOYMENT_SLOT);
		this.deploymentCustomData = (String) this.template.getCustom().get(AZURE_DEPLOYMENT_CUSTOMDATA);
		if (StringUtils.isBlank(deploymentSlot)) {
			deploymentSlot = "Staging";
		} else {
			if (!deploymentSlot.equals("Staging") && !deploymentSlot.equals("Production")) {
				throw new IllegalArgumentException(AZURE_DEPLOYMENT_SLOT + " property must be either 'Staging' or " +
						"'Production'");
			}
		}

		this.imageName = this.template.getImageId();
		this.userName = this.template.getUsername();
		this.password = this.template.getPassword();
		this.size = this.template.getHardwareId();
		this.fileTransferMode = this.template.getFileTransfer();
		this.remoteExecutionMode = this.template.getRemoteExecution();
		this.scriptLanguage = this.template.getScriptLanguage();

		this.ensureEndpointForManagementMachine();

		if (isWindowsVM()) {
			// [Windows] Handling firewall ports for manager machine (8100 & 8099)
			if (this.management) {
				this.firewallPorts = (List<Map<String, String>>) this.template.getCustom().get(AZURE_FIREWALL_PORTS);
				boolean cloudifyWebuiPort = false;
				boolean cloudifyRestApiPort = false;
				String port;
				if (firewallPorts != null) {
					for (Map<String, String> firewallPort : firewallPorts) {
						port = firewallPort.get("port");
						if (!port.contains("-")) {
							int p = Integer.parseInt(port);
							if (p == WEBUI_PORT)
								cloudifyWebuiPort = true;
							if (p == REST_PORT)
								cloudifyRestApiPort = true;
						}
					}
				}
				if (firewallPorts == null || !(cloudifyWebuiPort && cloudifyRestApiPort)) {
					throw new IllegalArgumentException("Custom field '"
							+ AZURE_FIREWALL_PORTS + "' must be set at least with " + WEBUI_PORT + " and " + REST_PORT);
				}
			}
		}

		this.cleanup = Boolean.parseBoolean((String) this.cloud.getCustom().get(AZURE_CLEANUP_ON_TEARDOWN));

		this.location = (String) this.cloud.getCustom().get(AZURE_AFFINITY_LOCATION);
		if (location == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_AFFINITY_LOCATION + "' must be set");
		}
		this.affinityGroup = (String) this.cloud.getCustom().get(AZURE_AFFINITY_GROUP);
		if (affinityGroup == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_AFFINITY_GROUP + "' must be set");
		}

		this.storageAccountName = (String) this.cloud.getCustom().get(AZURE_STORAGE_ACCOUNT);
		String computeTemplateStorageAccountName = (String) this.template.getCustom().get(AZURE_STORAGE_ACCOUNT);
		if (StringUtils.isNotBlank(computeTemplateStorageAccountName)) {
			this.storageAccountName = computeTemplateStorageAccountName;
		}
		if (storageAccountName == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_STORAGE_ACCOUNT + "' must be set");
		}

		// Data disk size
		this.dataDiskSize = (Integer) this.template.getCustom().get(AZURE_STORAGE_DATADISK_SIZE);

		// Network
		Map<String, String> networkCustom = this.cloud.getCloudNetwork().getCustom();
		this.addressSpace = (String) networkCustom.get(AZURE_NETWORK_ADDRESS_SPACE);
		if (addressSpace == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_NETWORK_ADDRESS_SPACE + "' must be set");
		}

		String dnsServer = networkCustom.get(AZURE_DNS_SERVERS);
		dnsServers = MicrosoftAzureUtils.parseDnsServersStringToMap(dnsServer);

		// Prefixes
		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup() + CLOUDIFY_MANAGER_NAME;
		} else {
			FullServiceName fullServiceName = ServiceUtils.getFullServiceName(configuration.getServiceName());
			this.serverNamePrefix = this.cloud.getProvider().getMachineNamePrefix() + fullServiceName.getServiceName();
		}

	}

	/**
	 * Add configuration for FileTransfer endpoint (22/445) or winrm endpoint (22/445) if doesn't exist.
	 *
	 * @throws CloudProvisioningException
	 */
	private void ensureEndpointForManagementMachine() throws CloudProvisioningException {
		if (management) {
			final String managementMachineTemplate = this.cloud.getConfiguration().getManagementMachineTemplate();
			final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);
			final FileTransferModes fileTransfer = template.getFileTransfer();
			final RemoteExecutionModes remoteExecution = template.getRemoteExecution();

			// Ensure that WinRM endpoint exists.
			Object objects = this.template.getCustom().get(AZURE_ENDPOINTS);
			@SuppressWarnings("unchecked")
			List<Map<String, String>> endpoints = (List<Map<String, String>>) objects;
			if (endpoints == null) {
				endpoints = new ArrayList<Map<String, String>>(1);
				this.template.getCustom().put(AZURE_ENDPOINTS, endpoints);
			}

			if (endpoints != null) {
				if (!doesEndpointsContainsPort(endpoints, remoteExecution.getDefaultPort())) {
					logger.warning("Missing remote execution endpoint, the drive will create one.");
					String portStr = Integer.toString(remoteExecution.getDefaultPort());
					HashMap<String, String> newEndpoint = new HashMap<String, String>();
					newEndpoint.put("name", remoteExecution.name());
					newEndpoint.put("protocol", "TCP");
					newEndpoint.put("localPort", portStr);
					newEndpoint.put("port", portStr);
					endpoints.add(newEndpoint);
				}
				if (!doesEndpointsContainsPort(endpoints, fileTransfer.getDefaultPort())) {
					logger.warning("Missing file transfert endpoint, the drive will create one.");
					String portStr = Integer.toString(fileTransfer.getDefaultPort());
					HashMap<String, String> newEndpoint = new HashMap<String, String>();
					newEndpoint.put("name", fileTransfer.name());
					newEndpoint.put("protocol", "TCP");
					newEndpoint.put("localPort", portStr);
					newEndpoint.put("port", portStr);
					endpoints.add(newEndpoint);
				}
			}
		}
	}

	private boolean doesEndpointsContainsPort(List<Map<String, String>> endpoints, int port2check)
			throws CloudProvisioningException {
		for (Map<String, String> endpointMap : endpoints) {
			String port = endpointMap.get("port");
			String localPort = endpointMap.get("localPort");
			if (port2check == Integer.parseInt(localPort)) {
				if (!localPort.equals(port)) {
					throw new CloudProvisioningException("The endpoint '" + endpointMap.get("name")
							+ "' should have the same value on localPort and port");
				}
				return true;
			}
		}
		return false;
	}

	private void verifyManagementNetworkConfiguration(Cloud cloud) throws CloudProvisioningException {
		if (cloud.getCloudNetwork() != null || cloud.getCloudNetwork().getManagement() != null) {
			NetworkConfiguration netConfig = cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
			if (netConfig.getSubnets() != null && !netConfig.getSubnets().isEmpty()) {
				if (netConfig.getSubnets().size() != 1) {
					logger.warning("Management network can only be configured with 1 subnet");
				} else {
					return;
				}
			} else {
				throw new CloudProvisioningException("Management network missing subnet configuration");
			}
		} else {
			throw new CloudProvisioningException("Missing management network configuration");
		}
	}

	@Override
	protected void initDeployer(Cloud cloud) {
		this.template = cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
		this.subscriptionId = this.cloud.getUser().getUser();

		String pfxFile = (String) this.template.getCustom().get(AZURE_PFX_FILE);
		if (pfxFile == null && management) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_PFX_FILE + "' must be set");
		}
		this.pathToPfxFile = this.template.getAbsoluteUploadDir() + File.separator + pfxFile;

		this.pfxPassword = (String) this.template.getCustom().get(AZURE_PFX_PASSWORD);
		if (pfxPassword == null && management) {
			throw new IllegalArgumentException("Custom field '" + AZURE_PFX_PASSWORD + "' must be set");
		}

		final String wireLog = (String) this.cloud.getCustom().get(AZURE_WIRE_LOG);
		boolean enableWireLog = false;
		if (wireLog != null) {
			enableWireLog = Boolean.parseBoolean(wireLog);
		}

		// reset cloudserviceName
		// null value for code causes problem while deploying, therefore it is set to an empty string if it's the case
		String cloudServiceCode = (String) this.cloud.getCustom().get(AZURE_CLOUD_SERVICE_CODE);
		if (cloudServiceCode == null || cloudServiceCode.trim().isEmpty()) {
			cloudServiceCode = "";
		}

		if (this.management) {
			cloudServicePrefix = this.cloud.getProvider().getManagementGroup() + cloudServiceCode;
		} else {
			FullServiceName fullServiceName = ServiceUtils.getFullServiceName(configuration.getServiceName());
			cloudServicePrefix = this.cloud.getProvider().getMachineNamePrefix() + cloudServiceCode +
					fullServiceName.getServiceName();
		}

		initRestClient(this.subscriptionId, this.pathToPfxFile, this.pfxPassword, enableWireLog);

		// set virtual network name for rest client
		this.networkName = (String) cloud.getCloudNetwork().getCustom().get(AZURE_NETWORK_NAME);
		if (networkName == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_NETWORK_NAME + "' must be set");
		}
		azureClient.setVirtualNetwork(this.networkName);
	}

	@Override
	protected void handleProvisioningFailure(int numberOfManagementMachines, int numberOfErrors,
			Exception firstCreationException, MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {

		logger.severe("Of the required " + numberOfManagementMachines + " management machines, " + numberOfErrors
				+ " failed to start.");

		try {
			logger.warning("Failed to start management machines. cleaning up any services that might have already been started.");
			stopManagementMachines();
		} catch (CloudProvisioningException e) {
			// catch any exceptions here.
			// otherwise they will end up as the exception thrown to the CLI./
			// thats not what we want in this case since we want the exception that failed the bootstrap command.
			logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		} catch (TimeoutException e) {
			logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		throw new CloudProvisioningException(
				"One or more management machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	@Override
	public MachineDetails startMachine(ProvisioningContext context, long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		// Create storage if required
		try {
			azureClient.createStorageAccount(affinityGroup, storageAccountName, endTime);
		} catch (Exception e) {
			throw new CloudProvisioningException(e);
		}

		// Add a subnet for the service instance if required
		CloudNetwork cloudNetwork = cloud.getCloudNetwork();
		if (this.configuration.getNetwork() != null) {
			String networkTemplate = this.configuration.getNetwork().getTemplate();
			NetworkConfiguration networkConfiguration = cloudNetwork.getTemplates().get(networkTemplate);
			List<Subnet> subnets = networkConfiguration.getSubnets();
			Subnet subnet = subnets.get(0);
			if (subnets.isEmpty()) {
				throw new CloudProvisioningException("No subnet configured for template network '" + networkTemplate
						+ "'");
			}
			try {
				String networkName = cloud.getCloudNetwork().getCustom().get(AZURE_NETWORK_NAME);
				String subnetName = subnet.getName();
				String subnetRange = subnet.getRange();
				azureClient.addSubnetToVirtualNetwork(networkName, subnetName, subnetRange, endTime);
			} catch (Exception e) {
				throw new CloudProvisioningException(e);
			}
		}

		// underscore character in hostname might cause deployment to fail
		String serverName = this.serverNamePrefix + String.format("%03d", serviceCounter.getAndIncrement());
		final ComputeTemplate computeTemplate = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
		return createServer(serverName, endTime, computeTemplate);
	}

	protected MachineDetails createServer(String serverName, long endTime, ComputeTemplate template)
			throws CloudProvisioningException, TimeoutException {

		MachineDetails machineDetails = new MachineDetails();
		CreatePersistentVMRoleDeploymentDescriptor desc;
		RoleDetails roleAddressDetails;
		try {

			desc = new CreatePersistentVMRoleDeploymentDescriptor();
			desc.setRoleName(serverName);
			desc.setDeploymentSlot(deploymentSlot);
			desc.setImageName(imageName);

			// verify availability set and avoid name concatenation if itsn't
			String availabilitySetName = null;
			if (StringUtils.isNotBlank(availabilitySet)) {
				availabilitySetName = this.cloud.getProvider().getManagementGroup() + availabilitySet +
						String.format("%03d", availabilitySetCounter.getAndIncrement());
			}

			desc.setAvailabilitySetName(availabilitySetName);

			// verify whether a CS is set or not in the compute template
			String cloudServiceInCompute = (String) template.getCustom().get(AZURE_CLOUD_SERVICE);
			if (cloudServiceInCompute != null && !cloudServiceInCompute.trim().isEmpty()) {

				HostedServices hostedServices = azureClient.listHostedServices();

				// is specified cs exist on azure ?
				if (hostedServices.contains(cloudServiceInCompute)) {

					Deployment deployment = azureClient.listDeploymentsBySlot(cloudServiceInCompute,
							deploymentSlot, endTime);

					// is there any deployment in the existing CS/slot
					if (deployment != null) {
						desc.setDeploymentName(deployment.getName());
						// use add role
						desc.setAddToExistingDeployment(true);

						// create a new deployment with the already existing CS
					} else {
						desc.setDeploymentName(cloudServiceInCompute);
					}

					desc.setHostedServiceName(cloudServiceInCompute);

				} else {
					// a cs/deployment will be created with specified name
					logger.warning(String.format("The cloud service '%s' doesn't exist on azure. "
							+ "It will be created.", cloudServiceInCompute));
					desc.setGenerateCloudServiceName(false);
					desc.setAddToExistingDeployment(false);
					desc.setCloudServiceName(cloudServiceInCompute);
				}
			} else {
				// a cs will be created with a generated name
				logger.fine(String.format("No cloud service was specified in compute '%s'. "
						+ "It will be created with a generic name.", this.cloudTemplateName));
				desc.setAddToExistingDeployment(false);
				desc.setGenerateCloudServiceName(true);
			}

			desc.setAffinityGroup(affinityGroup);
			desc.setCustomData(deploymentCustomData);

			// Data disk configuration
			if (this.dataDiskSize != null) {
				desc.setDataDiskSize(dataDiskSize);
			}

			InputEndpoints inputEndpoints = createInputEndPoints();

			CloudNetwork cloudNetwork = this.cloud.getCloudNetwork();

			String subnetName = null;
			if (this.configuration.getNetwork() != null) {
				// Use service's subnet
				String networkTemplate = this.configuration.getNetwork().getTemplate();
				NetworkConfiguration networkConfiguration = cloudNetwork.getTemplates().get(networkTemplate);
				subnetName = networkConfiguration.getSubnets().get(0).getName();
			} else {
				// use management's subnet
				subnetName = cloudNetwork.getManagement().getNetworkConfiguration().getSubnets().get(0).getName();
			}

			desc.setInputEndpoints(inputEndpoints);
			desc.setPassword(password);
			desc.setSize(size);
			desc.setStorageAccountName(storageAccountName);
			desc.setUserName(userName);
			desc.setNetworkName(networkName);
			desc.setSubnetName(subnetName);
			desc.setIpAddresses(this.getIpAddressesList(this.template.getCustom()));

			logger.info("Launching a new virtual machine");

			boolean isWindows = isWindowsVM();
			if (isWindows) {
				// domain join, support for windows at this moment
				desc.setDomainJoin(getDomainJoin());
			}

			roleAddressDetails = azureClient.createVirtualMachineDeployment(desc, isWindows, endTime);

			machineDetails.setPrivateAddress(roleAddressDetails.getPrivateIp());
			machineDetails.setPublicAddress(roleAddressDetails.getPublicIp());
			machineDetails.setMachineId(roleAddressDetails.getId());
			machineDetails.setAgentRunning(false);
			machineDetails.setCloudifyInstalled(false);
			machineDetails.setInstallationDirectory(this.template.getRemoteDirectory());
			machineDetails.setRemoteDirectory(this.template.getRemoteDirectory());
			machineDetails.setRemotePassword(password);
			machineDetails.setRemoteUsername(userName);
			machineDetails.setRemoteExecutionMode(this.remoteExecutionMode);
			machineDetails.setFileTransferMode(this.fileTransferMode);
			machineDetails.setScriptLangeuage(this.scriptLanguage);

			if (isWindows) {
				// TODO remove this/ use other way to open firewall bootstrap ? customdata ?
				// Open firewall ports needed for the template
				openFirewallPorts(machineDetails);
			}

			machineDetails.setOpenFilesLimit(this.template.getOpenFilesLimit());
			return machineDetails;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}

	}

	// TODO replace/remove opening ports with this logic
	@Deprecated
	private void openFirewallPorts(MachineDetails machineDetails) throws InstallerException, TimeoutException,
			InterruptedException {

		final RemoteExecutor remoteExecutor =
				RemoteExecutorFactory.createRemoteExecutorProvider(RemoteExecutionModes.WINRM);

		// InstallationDetails : Needed to executed remote command
		String localBootstrapScript = this.template.getAbsoluteUploadDir();
		InstallationDetails details = new InstallationDetails();
		details.setUsername(userName);
		details.setPassword(password);
		details.setLocalDir(localBootstrapScript);

		// Activate sharing on remote machine
		String hostAddress = null;
		if (this.management) {
			hostAddress = machineDetails.getPublicAddress();
		} else {
			hostAddress = machineDetails.getPrivateAddress();
		}

		remoteExecutor.execute(hostAddress, details, COMMAND_ACTIVATE_SHARING, DEFAULT_COMMAND_TIMEOUT);

		// Remote command to target : open all defined ports
		String cmd = "";
		if (this.firewallPorts != null) {
			for (Map<String, String> firewallPortsMap : this.firewallPorts) {
				String name = firewallPortsMap.get("name");
				String protocol = firewallPortsMap.get("protocol");

				// Port could be a range à a simple one
				// 7001 or 7001-7010 ([7001..7010])
				String port = firewallPortsMap.get("port");
				if (!"".equals(port) && port.contains("-")) {
					String[] portsRange = port.split("-");
					int portStart = Integer.parseInt(portsRange[0]);
					int portEnd = Integer.parseInt(portsRange[1]);
					if (portsRange.length == 2 && portStart <= portEnd) {
						// Opening from port portStart -> portEnd (with +1 increment)
						for (int i = portStart; i <= portEnd; i++) {
							cmd = String.format(COMMAND_OPEN_FIREWALL_PORT, name + i, protocol, i);
							remoteExecutor.execute(hostAddress, details, cmd, DEFAULT_COMMAND_TIMEOUT);
						}
					}
				}
				else {
					// No port defined : skip
					if (!"".equals(port)) {
						int portNumber = Integer.parseInt(port);
						cmd = String.format(COMMAND_OPEN_FIREWALL_PORT, name, protocol, portNumber);
						remoteExecutor.execute(hostAddress, details, cmd, DEFAULT_COMMAND_TIMEOUT);
					}
				}
			}
		}

	}

	@Override
	public MachineDetails[] startManagementMachines(ManagementProvisioningContext context, long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		try {
			azureClient.createAffinityGroup(affinityGroup, location, endTime);

			CloudNetwork cloudNetwork = this.cloud.getCloudNetwork();
			NetworkConfiguration networkConfiguration = cloudNetwork.getManagement().getNetworkConfiguration();
			Subnet subnet = networkConfiguration.getSubnets().get(0);
			azureClient.createVirtualNetworkSite(addressSpace, affinityGroup, networkName, subnet.getName(),
					subnet.getRange(), dnsServers, endTime);

			azureClient.createStorageAccount(affinityGroup, storageAccountName, endTime);
		} catch (final Exception e) {
			logger.warning("Failed creating management services : " + e.getMessage());
			if (cleanup) {
				try {
					logger.info("Cleaning up any services that may have already been started.");
					cleanup();
				} catch (final CloudProvisioningException e1) {
					// we catch this because we want to throw the original exception. not the one that happened on
					// cleanup.
					logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
					logger.fine(ExceptionUtils.getFullStackTrace(e1));
				}
			}
			throw new CloudProvisioningException(e);
		}

		int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		return doStartManagementMachines(endTime, numberOfManagementMachines);

	}

	private void cleanup() throws CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + CLEANUP_TIMEOUT;

		boolean deletedNetwork = false;
		boolean deletedStorage = false;
		Exception first = null;

		try {
			deletedNetwork = azureClient.deleteVirtualNetworkSite(networkName, endTime);
		} catch (final Exception e) {
			first = e;
			logger.warning("Failed deleting virtual network site " + networkName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		try {
			deletedStorage = azureClient.deleteStorageAccount(storageAccountName, endTime);
		} catch (final Exception e) {
			if (first == null) {
				first = e;
			}
			logger.warning("Failed deleting storage account " + storageAccountName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		if (deletedNetwork && deletedStorage) {
			try {
				azureClient.deleteAffinityGroup(affinityGroup, endTime);
			} catch (final Exception e) {
				if (first == null) {
					first = e;
				}
				logger.warning("Failed deleting affinity group " + affinityGroup + " : " + e.getMessage());
				logger.fine(ExceptionUtils.getFullStackTrace(e));
			}
		} else {
			logger.info("Not trying to delete affinity group since either virtual network " + networkName
					+ " , or storage account " + storageAccountName + " depend on it.");
		}
		if (first != null) {
			throw new CloudProvisioningException(first);
		}
	}

	/*********
	 * Checks if a stop request for this machine was already requested recently.
	 *
	 * @param ip
	 *            the IP address of the machine.
	 * @return true if there was a recent request, false otherwise.
	 */
	protected boolean isStopRequestRecent(final String ip) {
		// TODO - move this to the adapter!
		final Long previousRequest = stoppingMachines.get(ip);
		if (previousRequest != null
				&& System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT) {
			logger.fine("Machine " + ip + " is already stopping. Ignoring this shutdown request");
			return true;
		}

		// TODO - add a task thkat cleans up this map
		stoppingMachines.put(ip, System.currentTimeMillis());
		return false;
	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration,
			final TimeUnit unit) throws InterruptedException, TimeoutException,
			CloudProvisioningException {

		if (isStopRequestRecent(machineIp)) {
			return false;
		}

		long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		boolean connectToPrivateIp = this.cloud.getConfiguration().isConnectToPrivateIp();
		try {
			azureClient.deleteVirtualMachineByIp(machineIp, connectToPrivateIp, endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public void stopManagementMachines() throws TimeoutException,
			CloudProvisioningException {

		final long endTime = System.currentTimeMillis()
				+ TimeUnit.MINUTES.toMillis(stopManagementMachinesTimeoutInMinutes);
		boolean success = false;

		ExecutorService service = Executors.newCachedThreadPool();
		try {
			stopManagementMachines(endTime, service);
			success = true;
		} finally {
			if (!success) {
				if (cleanup) {
					logger.warning("Failed to shutdown management machines. no cleanup attempt will be made.");
				}
			}
			service.shutdown();
		}

		scanLeakingNodes();

		if (cleanup) {
			logger.info("Cleaning up management services");
			cleanup();
		}
	}

	@Deprecated
	// TODO methods searches all hosted services/deployment, it should look for only ones that are managed/newly
	// deployed, prefixes ?
	private void scanLeakingNodes() {
		try {
			HostedServices hostedServices = azureClient.listHostedServices();
			if (!hostedServices.getHostedServices().isEmpty()) {
				logger.warning("Found running cloud services. Scanning for leaking nodes...");
				for (HostedService hostedService : hostedServices) {
					logger.fine("Searching for deployments in cloud service " + hostedService.getServiceName());
					HostedService serviceWithDeployments =
							azureClient.getHostedService(hostedService.getServiceName(), true);
					if (serviceWithDeployments.getDeployments().getDeployments().size() > 0) {
						Deployment deployment = serviceWithDeployments.getDeployments().getDeployments().get(0);
						if (deployment != null) {
							logger.info("Found : " + deployment.getRoleList().getRoles().get(0).getRoleName());
						}
						throw new CloudProvisioningException(
								"There are still running instances, please shut them down and try again.");
					}
				}
			}
		} catch (final Exception e) {
			logger.warning("Failed Retrieving running virtual machines : " + e.getMessage());
			// nothing to do here...
		}
	}

	/**
	 * @param endTime
	 * @param service
	 * @throws TimeoutException
	 * @throws CloudProvisioningException
	 */
	private void stopManagementMachines(final long endTime, final ExecutorService service) throws TimeoutException,
			CloudProvisioningException {

		List<Future<?>> futures = new ArrayList<Future<?>>();

		Disks disks;
		try {
			disks = azureClient.listDisks();
		} catch (MicrosoftAzureException e1) {
			throw new CloudProvisioningException(e1);
		}

		// Create a set of proceeded because now a VM can have multiple attached disks
		Set<String> proceeded = new HashSet<String>();
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if (attachedTo != null) { // protect against zombie disks
				String roleName = attachedTo.getRoleName();
				if (roleName.startsWith(this.serverNamePrefix)) {
					final String deploymentName = attachedTo.getDeploymentName();
					final String hostedServiceName = attachedTo.getHostedServiceName();
					if (!proceeded.contains(hostedServiceName + deploymentName)) {
						StopManagementMachineCallable task =
								new StopManagementMachineCallable(deploymentName, hostedServiceName, endTime);
						futures.add(service.submit(task));
						proceeded.add(hostedServiceName + deploymentName);
					}
				}
			} else {
				if (disk.getName().contains(serverNamePrefix)) {
					try {
						logger.info("Detected a zombie OS Disk with name " + disk.getName() + ", Deleting it.");
						azureClient.deleteDisk(disk.getName(), endTime);
					} catch (final Exception e) {
						throw new CloudProvisioningException(e);
					}
				}
			}
		}

		// block until all tasks stop execution
		List<Throwable> exceptionOnStopMachines = new ArrayList<Throwable>();
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (final Exception e) {
				if (e instanceof InterruptedException) {
					exceptionOnStopMachines.add(e);
				} else {
					ExecutionException executionException = (ExecutionException) e;
					Throwable rootCause = ExceptionUtils.getRootCause(executionException);
					// print exception messages to the cli as they happen.
					// otherwise they are only shown in a log file.
					// this serves as a better user experience (users may not be aware of the file).
					logger.warning(rootCause.getMessage());
					exceptionOnStopMachines.add(rootCause);
				}
			}
		}
		if (!(exceptionOnStopMachines.isEmpty())) {
			if (logger.isLoggable(Level.FINEST)) {
				for (Throwable e : exceptionOnStopMachines) {
					logger.finest(ExceptionUtils.getFullStackTrace(e));
				}
			}
			throw new CloudProvisioningException(
					exceptionOnStopMachines.get(0).getMessage(),
					exceptionOnStopMachines.get(0));
		}

	}

	@Override
	public String getCloudName() {
		return "azure";
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	/**
	 *
	 * @author elip
	 *
	 */
	private class StopManagementMachineCallable implements Callable<Boolean> {
		private final String deploymentName;
		private final String hostedServiceName;
		private final long endTime;

		public StopManagementMachineCallable(final String deploymentName,
				final String hostedServiceName, final long endTime) {
			this.deploymentName = deploymentName;
			this.hostedServiceName = hostedServiceName;
			this.endTime = endTime;
		}

		@Override
		public Boolean call() throws CloudProvisioningException, TimeoutException {
			return stopManagementMachine(hostedServiceName, deploymentName, endTime);
		}
	}

	private boolean stopManagementMachine(final String hostedServiceName,
			final String deploymentName, final long endTime)
			throws CloudProvisioningException, TimeoutException {
		try {
			azureClient.deleteVirtualMachineByDeploymentName(hostedServiceName, deploymentName, endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		} catch (InterruptedException e) {
			throw new CloudProvisioningException(e);
		}

	}

	private InputEndpoints createInputEndPoints() throws MicrosoftAzureException {

		InputEndpoints inputEndpoints = new InputEndpoints();

		// Add End Point for each port
		Object objects = this.template.getCustom().get(AZURE_ENDPOINTS);
		List<Map<String, String>> endpoints = (List<Map<String, String>>) objects;
		if (endpoints != null) {
			for (Map<String, String> endpointMap : endpoints) {
				String name = endpointMap.get("name");
				String protocol = endpointMap.get("protocol");
				String portStr = endpointMap.get("port");
				String localPortStr = endpointMap.get("localPort");

				// skip endPoint, strict verification
				if (StringUtils.isNotBlank(localPortStr) && StringUtils.isNotBlank(protocol)
						&& StringUtils.isNotBlank(name)) {

					InputEndpoint endpoint = new InputEndpoint();

					if (StringUtils.isNotBlank(portStr)) {
						endpoint.setPort(Integer.parseInt(portStr));
					} // otherwise, port number will be generated by azure

					endpoint.setLocalPort(Integer.parseInt(localPortStr));
					endpoint.setName(name);
					endpoint.setProtocol(protocol);
					inputEndpoints.getInputEndpoints().add(endpoint);

				} else {
					String endPointValuesError = String.format("Failed provisioning VM, please check"
							+ " endPoint required elements in compute template '%s' : [name: '%s', protocol: '%s', "
							+ "localPort: '%s']", this.configuration.getCloudTemplate(), name, protocol, localPortStr);
					logger.severe(endPointValuesError);
					throw new MicrosoftAzureException(endPointValuesError);
				}

			}
		}

		// TODO change this behavior
		// open WEBUI and REST ports for management machines
		if (this.management) {

			InputEndpoint webuiEndpoint = new InputEndpoint();
			webuiEndpoint.setLocalPort(WEBUI_PORT);
			webuiEndpoint.setPort(WEBUI_PORT);
			webuiEndpoint.setName("Webui");
			webuiEndpoint.setProtocol("TCP");
			inputEndpoints.getInputEndpoints().add(webuiEndpoint);

			InputEndpoint restEndpoint = new InputEndpoint();
			restEndpoint.setLocalPort(REST_PORT);
			restEndpoint.setPort(REST_PORT);
			restEndpoint.setName("Rest");
			restEndpoint.setProtocol("TCP");
			inputEndpoints.getInputEndpoints().add(restEndpoint);
		}
		return inputEndpoints;
	}

	private Boolean isWindowsVM() {
		return RemoteExecutionModes.WINRM.equals(this.remoteExecutionMode)
				|| ScriptLanguages.WINDOWS_BATCH.equals(this.scriptLanguage);
	}

	@Override
	public Object getComputeContext() {
		return null;
	}

	@Override
	public void onServiceUninstalled(long duration, TimeUnit unit) {
		final long endTime = System.currentTimeMillis() + CLEANUP_TIMEOUT;

		// Remove subnet
		if (this.configuration.getNetwork() != null) {
			CloudNetwork cloudNetwork = cloud.getCloudNetwork();
			String networkTemplate = this.configuration.getNetwork().getTemplate();
			NetworkConfiguration networkConfiguration = cloudNetwork.getTemplates().get(networkTemplate);
			List<Subnet> subnets = networkConfiguration.getSubnets();
			Subnet subnet = subnets.get(0);
			try {
				logger.info("Delete the subnet '" + subnet.getName() + "' from network '" + this.networkName + "'");
				azureClient.removeSubnetByName(this.networkName, subnet.getName(), endTime);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Couldn't remove subnet '" + subnet.getName() + "' from network '"
						+ this.networkName + "'", e);
			}
		}
	}

	private List<String> getIpAddressesList(Map<String, Object> map) {
		List<String> ipAddressesList = null;

		if (map != null && !map.isEmpty()) {
			if (map.get(VM_IP_ADDRESSES) != null) {
				String ipAddressesString = (String) map.get(VM_IP_ADDRESSES);
				if (ipAddressesString != null && !ipAddressesString.trim().isEmpty()) {
					String[] split = ipAddressesString.split(",");
					ipAddressesList = Arrays.asList(split);
				}
			}

		}
		return ipAddressesList;
	}

	@SuppressWarnings("unchecked")
	private DomainJoin getDomainJoin() {

		DomainJoin domainJoin = null;

		Map<String, String> domainJoinMap = (Map<String, String>) this.template.getCustom().get(AZURE_DOMAIN_JOIN);
		if (domainJoinMap != null && !domainJoinMap.isEmpty()) {
			String domain = domainJoinMap.get(DOMAIN);
			String userName = domainJoinMap.get(DOMAIN_USERNAME);
			String password = domainJoinMap.get(DOMAIN_PASSWORD);
			String joinDomain = domainJoinMap.get(JOIN_DOMAIN);

			if (StringUtils.isNotBlank(domain) && StringUtils.isNotBlank(userName)
					&& StringUtils.isNotBlank(password) && StringUtils.isNotBlank(joinDomain)) {

				JoinCredentials jc = new JoinCredentials();
				jc.setDomain(domain);
				jc.setUserNamer(userName);
				jc.setPassword(password);

				domainJoin = new DomainJoin();
				domainJoin.setCredentials(jc);
				domainJoin.setJoinDomain(joinDomain);
			}
		}
		return domainJoin;
	}

}
