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
import java.util.List;
import java.util.Map;
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
import org.cloudifysource.dsl.internal.CloudifyConstants;
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
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
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

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static String CLOUDIFY_CLOUD_SERVICE_PREFIX = "cloudifycloudservice";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";
	private static final String CLOUDIFY_MANAGER_DEFAULT_NAME = "CFYM";

	// Custom template DSL properties
	private static final String AZURE_PFX_FILE = "azure.pfx.file";
	private static final String AZURE_PFX_PASSWORD = "azure.pfx.password";
	private static final String AZURE_ENDPOINTS = "azure.endpoints";
	private static final String AZURE_FIREWALL_PORTS = "azure.firewall.ports";

	private static final String VM_IP_ADDRESSES = "ipAddresses";

	// Custom cloud DSL properties
	private static final String AZURE_WIRE_LOG = "azure.wireLog";
	private static final String AZURE_DEPLOYMENT_SLOT = "azure.deployment.slot";
	private static final String AZURE_DEPLOYMENT_NAME = "azure.deployment.name";
	private static final String AZURE_AFFINITY_LOCATION = "azure.affinity.location";
	private static final String AZURE_NETOWRK_ADDRESS_SPACE = "azure.address.space";
	private static final String AZURE_AFFINITY_GROUP = "azure.affinity.group";
	private static final String AZURE_NETWORK_NAME = "azure.networksite.name";
	private static final String AZURE_STORAGE_ACCOUNT = "azure.storage.account";
	private static final String AZURE_AVAILABILITY_SET = "azure.availability.set";
	private static final String AZURE_CLEANUP_ON_TEARDOWN = "azure.cleanup.on.teardown";

	private static final String CDISCOUNT_CLOUD_SERVICE_CODE = "cdiscount.cloud.service.code";
	private static final String CDISCOUNT_AVAILABILITY_CODE = "cdiscount.availability.code";

	private AtomicInteger availabilitySetCounter = new AtomicInteger(1);

	private boolean cleanup;

	// Azure Credentials
	private String subscriptionId;

	// Arguments for all machines
	private String location;
	private String addressSpace;
	private String networkName;
	private String affinityGroup;
	private String storageAccountName;

	// Arguments per template
	private String deploymentSlot;
	private String deploymentName;
	private String imageName;
	private String userName;
	private String password;
	private String size;
	private String pathToPfxFile;
	private String pfxPassword;
	private String availabilitySet;
	private FileTransferModes fileTransferMode;
	private RemoteExecutionModes remoteExecutionMode;
	private ScriptLanguages scriptLanguage;

	private List<Map<String, String>> endpoints;
	private List<Map<String, String>> firewallPorts;

	private static final int WEBUI_PORT = 8099;
	private static final int REST_PORT = 8100;
	private static final int SSH_PORT = 22;

	// Commands template
	String COMMAND_OPEN_FIREWALL_PORT = "netsh firewall set portopening";
	String COMMAND_ACTIVATE_SHARING =
			"netsh advfirewall firewall set rule group=\\\"File and Printer Sharing\\\" new enable=yes";
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
					CLOUDIFY_CLOUD_SERVICE_PREFIX,
					CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
			if (enableWireLog) {
				azureClient.setLoggingFilter(logger);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setConfig(ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		super.setConfig(configuration);

		this.stopManagementMachinesTimeoutInMinutes = Utils.getInteger(cloud.getCustom().get(CloudifyConstants
				.STOP_MANAGEMENT_TIMEOUT_IN_MINUTES), DEFAULT_STOP_MANAGEMENT_TIMEOUT);

		// cdis custom
		String availabilityCode = (String) this.cloud.getCustom().get(CDISCOUNT_AVAILABILITY_CODE);
		String availability = (String) this.template.getCustom().get(AZURE_AVAILABILITY_SET);

		if (availability != null && !availability.trim().isEmpty()) {
			this.availabilitySet = availabilityCode + (String) this.template.getCustom().get(AZURE_AVAILABILITY_SET);
		} else {
			this.availabilitySet = null;
		}

		this.deploymentSlot = (String) this.template.getCustom().get(AZURE_DEPLOYMENT_SLOT);
		this.deploymentName = (String) this.template.getCustom().get(AZURE_DEPLOYMENT_NAME);
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

		boolean isWindowsVM = isWindowsVM();
		if (isWindowsVM) {
			// Test endpoints / firewall port for winrm at least 5985
			this.endpoints = (List<Map<String, String>>) this.template.getCustom().get(AZURE_ENDPOINTS);
			boolean winRMEndpoint = false;
			for (Map<String, String> endpointMap : endpoints) {
				String endPointPort = endpointMap.get("port");
				if (!endPointPort.contains("-")
						&& RemoteExecutionModes.WINRM.getDefaultPort() == Integer.parseInt(endPointPort))
					winRMEndpoint = true;
			}
			if (endpoints == null || !winRMEndpoint) {
				throw new IllegalArgumentException("Custom field '"
						+ AZURE_ENDPOINTS + "' must be set at least with WinRM port "
						+ RemoteExecutionModes.WINRM.getDefaultPort());
			}

			// handeling firewall ports for manager machine (8100 & 8099)
			if (this.management) {
				this.firewallPorts = (List<Map<String, String>>) this.template.getCustom().get(AZURE_FIREWALL_PORTS);
				boolean cloudifyWebuiPort = false;
				boolean cloudifyRestApiPort = false;
				String port;
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
		this.addressSpace = (String) this.cloud.getCustom().get(AZURE_NETOWRK_ADDRESS_SPACE);
		if (addressSpace == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_NETOWRK_ADDRESS_SPACE + "' must be set");
		}
		this.affinityGroup = (String) this.cloud.getCustom().get(AZURE_AFFINITY_GROUP);
		if (affinityGroup == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_AFFINITY_GROUP + "' must be set");
		}
		this.networkName = (String) this.cloud.getCustom().get(AZURE_NETWORK_NAME);
		if (networkName == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_NETWORK_NAME + "' must be set");
		}
		this.storageAccountName = (String) this.cloud.getCustom().get(AZURE_STORAGE_ACCOUNT);
		if (storageAccountName == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_STORAGE_ACCOUNT + "' must be set");
		}

		// cdis custom
		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup() + CLOUDIFY_MANAGER_DEFAULT_NAME;

		} else {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup() + configuration.getServiceName();
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
		// cdiscount configuration
		String cloudServiceCode = (String) this.cloud.getCustom().get(CDISCOUNT_CLOUD_SERVICE_CODE);

		if (this.management) {
			CLOUDIFY_CLOUD_SERVICE_PREFIX = this.cloud.getProvider().getManagementGroup() +
					cloudServiceCode + CLOUDIFY_MANAGER_DEFAULT_NAME;

		} else {
			CLOUDIFY_CLOUD_SERVICE_PREFIX = this.cloud.getProvider().getManagementGroup() + cloudServiceCode +
					configuration.getServiceName();
		}

		initRestClient(this.subscriptionId, this.pathToPfxFile, this.pfxPassword, enableWireLog);
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
		// underscore character in hostname might cause deployment to fail
		String serverName = serverNamePrefix + "-role";
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
			desc.setDeploymentName(deploymentName);
			desc.setImageName(imageName);

			// verify availability set and avoid name concatenation if itsn't
			String availabilitySetName = null;
			if (availabilitySet != null && !availabilitySet.trim().isEmpty()) {
				availabilitySetName = this.cloud.getProvider().getManagementGroup() + availabilitySet +
						String.format("%03d", availabilitySetCounter.getAndIncrement());
			}

			desc.setAvailabilitySetName(availabilitySetName);
			desc.setAffinityGroup(affinityGroup);

			InputEndpoints inputEndpoints = createInputEndPoints();

			desc.setInputEndpoints(inputEndpoints);
			desc.setNetworkName(networkName);
			desc.setPassword(password);
			desc.setSize(size);
			desc.setStorageAccountName(storageAccountName);
			desc.setUserName(userName);
			desc.setIpAddresses(this.getIpAddressesList(this.template.getOptions()));
			desc.setSubnetName("subnet-" + networkName); // TODO to be enhance once the driver supports network

			logger.info("Launching a new virtual machine");
			boolean isWindows = isWindowsVM();
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
				// Open firewall ports needed for the template
				openFirewallPorts(machineDetails);
			}

			machineDetails.setOpenFilesLimit(this.template.getOpenFilesLimit());
			return machineDetails;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}

	}

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
		remoteExecutor.execute(machineDetails.getPublicAddress(), details, COMMAND_ACTIVATE_SHARING,
				DEFAULT_COMMAND_TIMEOUT);

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
							cmd = COMMAND_OPEN_FIREWALL_PORT + " " + protocol + " " + i + " " + (name + i);
							remoteExecutor.execute(machineDetails.getPublicAddress(), details, cmd,
									DEFAULT_COMMAND_TIMEOUT);
						}
					}
				}
				else {
					// No port defined : skip
					if (!"".equals(port)) {
						int portNumber = Integer.parseInt(port);
						cmd = COMMAND_OPEN_FIREWALL_PORT + " " + protocol + " " + portNumber + " " + name;
						remoteExecutor
								.execute(machineDetails.getPublicAddress(), details, cmd, DEFAULT_COMMAND_TIMEOUT);
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
			azureClient.createVirtualNetworkSite(addressSpace, affinityGroup, networkName, endTime);
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
			disks = azureClient.listOSDisks();
		} catch (MicrosoftAzureException e1) {
			throw new CloudProvisioningException(e1);
		}
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if (attachedTo != null) { // protect against zombie disks
				String roleName = attachedTo.getRoleName();
				if (roleName.startsWith(this.serverNamePrefix)) {
					final String diskName = disk.getName();
					final String deploymentName = attachedTo.getDeploymentName();
					final String hostedServiceName = attachedTo.getHostedServiceName();
					StopManagementMachineCallable task = new StopManagementMachineCallable(
							deploymentName, hostedServiceName, diskName, endTime);
					futures.add(service.submit(task));
				}
			} else {
				if (disk.getName().contains(serverNamePrefix)) {
					try {
						logger.info("Detected a zombie OS Disk with name " + disk.getName() + ", Deleting it.");
						azureClient.deleteOSDisk(disk.getName(), endTime);
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
				final String hostedServiceName, final String diskName,
				final long endTime) {
			this.deploymentName = deploymentName;
			this.hostedServiceName = hostedServiceName;
			this.endTime = endTime;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public Boolean call() throws CloudProvisioningException,
				TimeoutException {

			return stopManagementMachine(hostedServiceName, deploymentName,
					endTime);

		}
	}

	private boolean stopManagementMachine(final String hostedServiceName,
			final String deploymentName, final long endTime)
			throws CloudProvisioningException, TimeoutException {

		try {
			azureClient.deleteVirtualMachineByDeploymentName(hostedServiceName,
					deploymentName, endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		} catch (InterruptedException e) {
			throw new CloudProvisioningException(e);
		}

	}

	private InputEndpoints createInputEndPoints() {

		InputEndpoints inputEndpoints = new InputEndpoints();

		// Add End Point for each port
		if (this.endpoints != null) {
			for (Map<String, String> endpointMap : this.endpoints) {
				String name = endpointMap.get("name");
				String protocol = endpointMap.get("protocol");

				// Port could be a range à a simple one
				// 7001 or 7001-7010 ([7001..7010])
				String port = endpointMap.get("port");
				if (!"".equals(port) && port.contains("-")) {
					String[] portsRange = port.split("-");
					int portStart = Integer.parseInt(portsRange[0]);
					int portEnd = Integer.parseInt(portsRange[1]);
					if (portsRange.length == 2 && portStart <= portEnd) {
						// Opening from port portStart -> portEnd (with +1 increment)
						InputEndpoint endpoint = null;
						for (int i = portStart; i <= portEnd; i++) {
							endpoint = new InputEndpoint();
							endpoint.setLocalPort(i);
							endpoint.setPort(i);
							endpoint.setName(name + i);
							endpoint.setProtocol(protocol);
							inputEndpoints.getInputEndpoints().add(endpoint);
						}
					}
				}
				else {
					// No port defined : skip
					if (!"".equals(port)) {
						int portNumber = Integer.parseInt(port);
						InputEndpoint endpoint = new InputEndpoint();
						endpoint.setLocalPort(portNumber);
						endpoint.setPort(portNumber);
						endpoint.setName(name);
						endpoint.setProtocol(protocol);
						inputEndpoints.getInputEndpoints().add(endpoint);
					}
				}

			}
		}

		// open the SSH port linux vm's only
		if (!isWindowsVM()) {
			InputEndpoint sshEndpoint = new InputEndpoint();
			sshEndpoint.setLocalPort(SSH_PORT);
			sshEndpoint.setPort(SSH_PORT);
			sshEndpoint.setName("SSH");
			sshEndpoint.setProtocol("TCP");
			inputEndpoints.getInputEndpoints().add(sshEndpoint);
		}

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
		// TODO Auto-generated method stub
	}

	private List<String> getIpAddressesList(Map<String, Object> templateOptions) {
		List<String> ipAddressesList = null;

		if (templateOptions != null && !templateOptions.isEmpty()) {
			if (templateOptions.get(VM_IP_ADDRESSES) != null) {
				String ipAddressesString = (String) templateOptions.get(VM_IP_ADDRESSES);
				if (ipAddressesString != null && !ipAddressesString.trim().isEmpty()) {
					String[] split = ipAddressesString.split(",");
					ipAddressesList = Arrays.asList(split);
				}
			}

		}
		return ipAddressesList;
	}
}
