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
import java.util.Map.Entry;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
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
import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.AttachedTo;
import org.cloudifysource.esc.driver.provisioning.azure.model.Connection;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConnectionsToLocalNetwork;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.DomainJoin;
import org.cloudifysource.esc.driver.provisioning.azure.model.EndpointAcl;
import org.cloudifysource.esc.driver.provisioning.azure.model.Gateway;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.JoinCredentials;
import org.cloudifysource.esc.driver.provisioning.azure.model.LoadBalancerProbe;
import org.cloudifysource.esc.driver.provisioning.azure.model.LocalNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.LocalNetworkSiteRef;
import org.cloudifysource.esc.driver.provisioning.azure.model.LocalNetworkSites;
import org.cloudifysource.esc.driver.provisioning.azure.model.Rule;
import org.cloudifysource.esc.driver.provisioning.azure.model.Rules;
import org.cloudifysource.esc.driver.provisioning.azure.model.VpnConfiguration;
import org.cloudifysource.esc.driver.provisioning.storage.azure.AzureDeploymentContext;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutor;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutorFactory;
import org.cloudifysource.esc.util.Utils;

import com.google.common.base.Joiner;

/***************************************************************************************
 * A custom Cloud Driver implementation for provisioning machines on Azure.
 *
 * @author elip
 ***************************************************************************************/
public class MicrosoftAzureCloudDriver extends BaseProvisioningDriver {

	private static final Logger logger = Logger.getLogger(MicrosoftAzureCloudDriver.class.getName());
	private static final long CLEANUP_DEFAULT_TIMEOUT = 60 * 1000 * 15; // 15 minutes
	private static final long DEFAULT_COMMAND_TIMEOUT = 15 * 60 * 1000L; // 15 minutes
	private static final int DEFAULT_STOP_MANAGEMENT_TIMEOUT_IN_MINUTES = 30; // 30 minutes
	private int stopManagementMachinesTimeoutInMinutes = DEFAULT_STOP_MANAGEMENT_TIMEOUT_IN_MINUTES;

	private Lock driverPendingRequest = new ReentrantLock(true);

	protected static final String CLOUDIFY_MANAGER_NAME = "cfym";
	private static final String STRING_SEPERATOR = ",";

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";

	private static final String CLOUDIFY_REST_PORT_NAME = "CLOUDIFY_REST";
	private static final String CLOUDIFY_WEBUI_PORT_NAME = "CLOUDIFY_GUI";

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
	public static final String AZURE_AFFINITY_GROUP = "azure.affinity.group";

	public static final String AZURE_STORAGE_ACCOUNT = "azure.storage.account";
	public static final String AZURE_STORAGE_ACCOUNT_PREFIX = "azure.storage.account.prefix";
	public static final String AZURE_STORAGE_ACCOUNT_FILE_SERVICE = "azure.storage.account.file.service";
	public static final String AZURE_STORAGE_ACCOUNTS_DATA = "azure.storage.accounts.data";

	// extensions
	private static final String AZURE_EXTENSIONS = "azure.extensions";

	/**
	 * Optional. If set, the driver will create and attack a data disk with the defined size to the new VM.
	 */
	private static final String AZURE_STORAGE_DATADISK_SIZE = "azure.storage.datadisk.size";

	private static final String AZURE_AVAILABILITY_SET = "azure.availability.set";
	private static final String AZURE_AVAILABILITY_SET_MAX_MEMBERS = "azure.availability.set.max.members";
	private static final String AZURE_CLEANUP_ON_TEARDOWN = "azure.cleanup.on.teardown";

	// Networks properties
	private static final String AZURE_NETWORK_NAME = "azure.networksite.name";
	private static final String AZURE_NETWORK_ADDRESS_SPACE = "azure.address.space";
	private static final String AZURE_DNS_SERVERS = "azure.dns.servers";

	// -- VPN properties
	private static final String AZURE_VPN_LOCALSITE_NAME = "azure.vpn.localsite.name";
	private static final String AZURE_VPN_ADDRESS_SPACE = "azure.vpn.address.space";
	private static final String AZURE_VPN_SUBNET_ADDRESS_PREFIX = "azure.vpn.subnet.address.prefix";
	private static final String AZURE_VPN_GATEWAY_SUBNET_NAME = "GatewaySubnet";
	private static final String AZURE_VPN_GATEWAY_ADDRESS = "azure.vpn.gateway.address";
	private static final String AZURE_VPN_GATEWAY_TYPE = "azure.vpn.gateway.type";
	private static final String AZURE_VPN_GATEWAY_KEY = "azure.vpn.gateway.key";

	private static final String AZURE_GENERATE_ENDPOINTS = "azure.generate.endpoints";

	private static final String AZURE_CLOUD_SERVICE_CODE = "azure.cloud.service.code";

	private static String cloudServicePrefix = "cloudifycloudservice";

	private boolean generateEndpoints;

	private AtomicInteger serviceCounter = new AtomicInteger(1);

	private boolean cleanup;
	private String globalAvailabilitySet;

	// Azure Credentials
	private String subscriptionId;
	private String fileServiceStorageAccount;

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
	private Integer availabilitySetMaxMember;
	private Integer dataDiskSize;
	private String ipAddresses;

	private List<String> computeTemplateStorageAccountName;
	private List<String> computeTemplateDataStorageAccounts;

	private FileTransferModes fileTransferMode;
	private RemoteExecutionModes remoteExecutionMode;
	private ScriptLanguages scriptLanguage;

	private List<Map<String, String>> firewallPorts;
	private List<Map<String, String>> extensions;

	private static final int WEBUI_PORT = 8099;
	private static final int REST_PORT = 8100;

	private static final String DOMAIN = "domain";
	private static final String DOMAIN_USERNAME = "userName";
	private static final String DOMAIN_PASSWORD = "password";
	private static final String JOIN_DOMAIN = "joinDomain";
	private static final String MACHINE_OBJECT_OU = "machineObjectOU";

	private static final String ENDPOINT_NAME = "name";
	private static final String ENDPOINT_LOCALPORT = "localPort";
	private static final String ENDPOINT_PORT = "port";
	private static final String ENDPOINT_PROTOCOL = "protocol";

	private static final String ENDPOINT_LOADBALANCEDSET = "loadBalancedSet";
	private static final String ENDPOINT_PROBE_PORT = "probePort";
	private static final String ENDPOINT_PROBE_PROTOCOL = "probeProtocol";

	private static final String ENDPOINT_BASIC = "basic";
	private static final String ENDPOINT_LB = "lb";
	private static final String ENDPOINT_ACL = "acl";

	private static final String ENDPOINT_ORDER = "order";
	private static final String ENDPOINT_ACTION = "action";
	private static final String ENDPOINT_SUBNET = "subnet";
	private static final String ENDPOINT_DESCRIPTION = "description";

	private static final String ENDPOINT_ACTION_PERMIT = "permit";
	private static final String ENDPOINT_ACTION_DENY = "deny";

	// Commands template
	private static final String COMMAND_OPEN_FIREWALL_PORT = "netsh advfirewall firewall"
			+ " add rule name=\"%s\" dir=in action=allow protocol=%s localport=%d";
	private static final String COMMAND_ACTIVATE_SHARING = "netsh advfirewall firewall"
			+ " set rule group=\\\"File and Printer Sharing\\\" new enable=yes";

	private ComputeTemplate template;

	private MicrosoftAzureRestClient azureClient;

	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();

	/** Contains information to be shared with the storage driver */
	private AzureDeploymentContext azureDeploymentContext = null;

	public MicrosoftAzureCloudDriver() {
	}

	public AzureDeploymentContext getAzureContext() {
		return this.azureDeploymentContext;
	}

	private synchronized void initRestClient(
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
				.STOP_MANAGEMENT_TIMEOUT_IN_MINUTES), DEFAULT_STOP_MANAGEMENT_TIMEOUT_IN_MINUTES);

		// availability
		this.globalAvailabilitySet = (String) cloud.getCustom().get(AZURE_AVAILABILITY_SET);
		if (StringUtils.isNotBlank(globalAvailabilitySet)) {
			this.globalAvailabilitySet = globalAvailabilitySet.trim();
		} else {
			throw new IllegalArgumentException(AZURE_AVAILABILITY_SET + " property must be set");
		}
		String availability = (String) this.template.getCustom().get(AZURE_AVAILABILITY_SET);
		if (StringUtils.isNotBlank(availability)) {
			logger.fine("Using compute template configuration for " + AZURE_AVAILABILITY_SET + " settings");
			this.availabilitySet = availability.trim();
		} else {
			this.availabilitySet = this.globalAvailabilitySet;
		}

		// availability members limit
		Object asmm = cloud.getCustom().get(AZURE_AVAILABILITY_SET_MAX_MEMBERS);
		if (asmm == null) {
			logger.fine("Using compute template configuration for " + AZURE_AVAILABILITY_SET_MAX_MEMBERS + " settings");
			asmm = this.template.getCustom().get(AZURE_AVAILABILITY_SET_MAX_MEMBERS);
		}
		if (asmm != null) {
			if (asmm instanceof Integer) {
				this.availabilitySetMaxMember = (Integer) asmm;
			} else {
				throw new IllegalArgumentException(AZURE_AVAILABILITY_SET_MAX_MEMBERS + " must be an Integer");
			}
		} else {
			this.availabilitySetMaxMember = -1;
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

		// check cfy endpoints generation

		String generateEndPoints = (String) this.template.getCustom().get(AZURE_GENERATE_ENDPOINTS);
		if (StringUtils.isBlank(generateEndPoints)) {
			this.generateEndpoints = true;
		} else {
			this.generateEndpoints =
					Boolean.parseBoolean((String) this.template.getCustom().get(AZURE_GENERATE_ENDPOINTS));
		}

		if (this.generateEndpoints) {
			this.ensureEndpointForManagementMachine();
		}

		this.firewallPorts = (List<Map<String, String>>) this.template.getCustom().get(AZURE_FIREWALL_PORTS);

		this.cleanup = Boolean.parseBoolean((String) this.cloud.getCustom().get(AZURE_CLEANUP_ON_TEARDOWN));

		this.location = (String) this.cloud.getCustom().get(AZURE_AFFINITY_LOCATION);
		if (location == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_AFFINITY_LOCATION + "' must be set");
		}
		this.affinityGroup = (String) this.cloud.getCustom().get(AZURE_AFFINITY_GROUP);
		if (affinityGroup == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_AFFINITY_GROUP + "' must be set");
		}

		// storage accounts
		this.computeTemplateStorageAccountName = (List<String>) this.template.getCustom().get(AZURE_STORAGE_ACCOUNT);
		this.computeTemplateDataStorageAccounts = (List<String>) this.template.getCustom()
				.get(AZURE_STORAGE_ACCOUNTS_DATA);

		this.storageAccountName = (String) this.cloud.getCustom().get(AZURE_STORAGE_ACCOUNT_PREFIX);
		if (storageAccountName == null) {
			throw new IllegalArgumentException("Custom field '" + AZURE_STORAGE_ACCOUNT_PREFIX + "' must be set");
		}

		String storageAccountFileStr = (String) this.cloud.getCustom().get(AZURE_STORAGE_ACCOUNT_FILE_SERVICE);
		if (storageAccountFileStr != null && StringUtils.isNotBlank(storageAccountFileStr.trim())) {
			this.fileServiceStorageAccount = storageAccountFileStr;
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

		// extensions
		extensions = (List<Map<String, String>>) this.template.getCustom().get(AZURE_EXTENSIONS);

		// ipAddresses
		String ips = (String) this.template.getCustom().get(VM_IP_ADDRESSES);
		if (StringUtils.isNotBlank(ips)) {
			ipAddresses = ips;
		}

		// Prefixes
		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider().getManagementGroup() + CLOUDIFY_MANAGER_NAME;
		} else {
			FullServiceName fullServiceName = ServiceUtils.getFullServiceName(configuration.getServiceName());
			this.serverNamePrefix = this.cloud.getProvider().getMachineNamePrefix() + fullServiceName.getServiceName();
		}

		this.serverNamePrefix = serverNamePrefix.toUpperCase();

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
			List<Map<String, Object>> endpoints = (List<Map<String, Object>>) objects;

			if (endpoints == null) {
				endpoints = new ArrayList<Map<String, Object>>(1);
				this.template.getCustom().put(AZURE_ENDPOINTS, endpoints);
			}

			if (endpoints != null) {
				if (!doesEndpointsContainsPort(endpoints, remoteExecution.getDefaultPort())) {
					logger.info("No remote execution endpoint, the drive will create one.");
					String portStr = Integer.toString(remoteExecution.getDefaultPort());

					HashMap<String, String> newEndpoint = new HashMap<String, String>();
					newEndpoint.put("name", remoteExecution.name());
					newEndpoint.put("protocol", "TCP");
					newEndpoint.put("localPort", portStr);
					newEndpoint.put("port", portStr);

					Map<String, Object> newEndpointMap = new HashMap<String, Object>();
					newEndpointMap.put(ENDPOINT_BASIC, newEndpoint);
					endpoints.add(newEndpointMap);
				}
				if (!doesEndpointsContainsPort(endpoints, fileTransfer.getDefaultPort())) {
					logger.info("No file transfert endpoint, the drive will create one.");
					String portStr = Integer.toString(fileTransfer.getDefaultPort());
					HashMap<String, Object> newEndpoint = new HashMap<String, Object>();
					newEndpoint.put("name", fileTransfer.name());
					newEndpoint.put("protocol", "TCP");
					newEndpoint.put("localPort", portStr);
					newEndpoint.put("port", portStr);

					Map<String, Object> newEndpointMap = new HashMap<String, Object>();
					newEndpointMap.put(ENDPOINT_BASIC, newEndpoint);
					endpoints.add(newEndpointMap);

				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private boolean doesEndpointsContainsPort(List<Map<String, Object>> endpoints, int port2check)
			throws CloudProvisioningException {

		for (Map<String, Object> endpointMap : endpoints) {
			Object endPointBasicObject = endpointMap.get(ENDPOINT_BASIC);
			if (endPointBasicObject != null) {

				Map<String, String> endPointBasic = (Map<String, String>) endPointBasicObject;

				String port = endPointBasic.get("port");
				String localPort = endPointBasic.get("localPort");
				if (port2check == Integer.parseInt(localPort)) {
					if (!localPort.equals(port)) {
						throw new CloudProvisioningException("The endpoint '" + endPointBasic.get("name")
								+ "' should have the same value on localPort and port");
					}
					return true;
				}
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

		// underscore character in hostname might cause deployment to fail
		String serverName = this.serverNamePrefix + String.format("%03d", serviceCounter.getAndIncrement());
		final ComputeTemplate computeTemplate = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);

		return createServer(serverName.toUpperCase(), endTime, computeTemplate);
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

			// availability set
			desc.setAvailabilitySetName(this.availabilitySet);
			desc.setAvailabilitySetMaxMember(this.availabilitySetMaxMember);

			// verify whether a CS is set or not in the compute template
			String cloudServiceInCompute = (String) template.getCustom().get(AZURE_CLOUD_SERVICE);
			if (cloudServiceInCompute != null && !cloudServiceInCompute.trim().isEmpty()) {
				desc.setGenerateCloudServiceName(false);
			}
			else {
				// a cs will be created with a generated name
				logger.finest(String.format("No cloud service was specified in compute '%s'. ", this.cloudTemplateName));
				desc.setGenerateCloudServiceName(true);
			}

			// cloud service
			desc.setHostedServiceName(cloudServiceInCompute);

			desc.setAffinityGroup(affinityGroup);
			desc.setCustomData(deploymentCustomData);

			// main storage account
			desc.setStorageAccountName(this.storageAccountName);

			// storage account for file service
			if (fileServiceStorageAccount != null) {
				logger.finer("Storage account for file service :" + fileServiceStorageAccount);
				azureClient.createStorageAccount(affinityGroup, fileServiceStorageAccount, endTime);
			}

			// Data disk configuration
			if (this.dataDiskSize != null) {
				desc.setDataDiskSize(dataDiskSize);

				if (computeTemplateDataStorageAccounts != null && !computeTemplateDataStorageAccounts.isEmpty()) {
					desc.setDataStorageAccounts(computeTemplateDataStorageAccounts);
				}
			}

			// Storage Account for OS
			String osStorageAccountName = null;
			osStorageAccountName = this.createBalancedStorageAccountNameForOSDisk(endTime);
			logger.fine(String.format("Using '%s' as balanced storage account for OS disk", osStorageAccountName));
			desc.setOsStorageAccountName(osStorageAccountName);

			InputEndpoints inputEndpoints = createInputEndPoints();

			// network and subnet
			CloudNetwork cloudNetwork = this.cloud.getCloudNetwork();

			String subnetName = null;
			subnetName = cloudNetwork.getManagement().getNetworkConfiguration().getSubnets().get(0).getName();

			if (this.configuration.getNetwork() != null) {

				String networkTemplate = this.configuration.getNetwork().getTemplate();
				NetworkConfiguration networkConfiguration = cloudNetwork.getTemplates().get(networkTemplate);

				// check
				if (networkConfiguration == null) {
					logger.warning(String.format(
							"The specified network template '%s' doesn't exist in available network templates. "
									+ "Management subnet will be used instead.", networkTemplate));
				} else {
					// Use service's subnet
					subnetName = networkConfiguration.getSubnets().get(0).getName();
				}
			}

			desc.setInputEndpoints(inputEndpoints);
			desc.setPassword(password);
			desc.setSize(size);
			desc.setUserName(userName);
			desc.setNetworkName(networkName);
			desc.setSubnetName(subnetName);

			List<String> listFromSplitedString = MicrosoftAzureUtils.getListFromSplitedString(this.ipAddresses,
					STRING_SEPERATOR);
			desc.setIpAddresses(listFromSplitedString);

			logger.info("Launching a new virtual machine");

			boolean isWindows = isWindowsVM();
			if (isWindows) {
				// domain join, support for windows at this moment
				desc.setDomainJoin(getDomainJoin());

				// extensions for windows only
				desc.setExtensions(this.extensions);
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
				try {
					logger.info("Trying to open windows vm firewall ports...");
					openFirewallPorts(machineDetails);
					logger.info("Windows vm firewall ports operation finished");
				} catch (Exception e) {
					logger.log(Level.WARNING,
							"Failed opening windows vm firewall ports. This can be critical for Windows Vms if the "
									+ "required ports are not open.", e);
				}
			}

			machineDetails.setOpenFilesLimit(this.template.getOpenFilesLimit());

			// For storage driver
			if (this.azureDeploymentContext == null) {
				String cloudServiceName = roleAddressDetails.getCloudServiceName();
				String deploymentName = roleAddressDetails.getDeploymentName();
				logger.info(String.format("Create AzureDeploymentContext(%s,%s)/%s", cloudServiceName,
						deploymentName, serverName));
				this.setAzureDeploymentContext(new AzureDeploymentContext(cloudServiceName, deploymentName, azureClient));
			}
			return machineDetails;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}

	}

	/**
	 * Create storage accounts set in 'azure.storage.account' of the compute template options if needed. It returns the
	 * most empty storage account's name. If the properties is not set, the method simply returns the value of
	 * 'azure.storage.account.prefix' global custom property.
	 * 
	 * @return The most empty storage account defined in 'azure.storage.account' or the prefix in
	 *         'azure.storage.account.prefix' global custom property.
	 */
	private String createBalancedStorageAccountNameForOSDisk(long endTime) {
		String osStorageAccountName;
		try {
			if (this.computeTemplateStorageAccountName != null) {
				this.createAsyncStorageAccountsForOSDisks(endTime);
				osStorageAccountName = MicrosoftAzureUtils.getBalancedStorageAccount(
						this.computeTemplateStorageAccountName, azureClient);
				logger.fine("Configuration of storage accounts for os disks finished");
				driverPendingRequest.unlock();
			} else {
				osStorageAccountName = this.storageAccountName;
			}

		} catch (Exception e) {
			logger.warning("Failed selecting balanced storage account from the specified storage accounts : "
					+ this.computeTemplateStorageAccountName.toString());
			osStorageAccountName = this.storageAccountName;
			logger.warning("Selecting a storage account instead : " + osStorageAccountName);
		}
		return osStorageAccountName;
	}

	/**
	 * Creates Storage Accounts in a parallel.
	 */
	private void createAsyncStorageAccountsForOSDisks(long endTime) throws MicrosoftAzureException,
			InterruptedException, TimeoutException, ExecutionException {
		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;
		if (lockTimeout < 0) {
			throw new MicrosoftAzureException("Timeout. Abord request to configurate storage accounts");
		}
		logger.fine("Waiting for pending driver request lock for lock "
				+ driverPendingRequest.hashCode());

		boolean lockAcquired = driverPendingRequest.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
		if (!lockAcquired) {
			throw new TimeoutException("Failed to acquire lock for configurating storage accounts after + "
					+ lockTimeout + " milliseconds");
		}
		logger.fine("Configurating storage accounts for os disks");
		ExecutorService executorService =
				Executors.newFixedThreadPool(computeTemplateStorageAccountName.size());

		List<Future<?>> futures = new ArrayList<Future<?>>();

		for (String storage : this.computeTemplateStorageAccountName) {
			Future<?> f = executorService.submit(
					new StorageCallable(azureClient, affinityGroup, storage, endTime));
			futures.add(f);
		}

		for (Future<?> f : futures) {
			f.get();
		}

		executorService.shutdownNow();
	}

	// TODO replace/remove opening ports with this logic
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

		// automatically open ports for cloudify REST and WEBUI
		Integer restPort = this.cloud.getConfiguration().getComponents().getRest().getPort();
		String cmdFireWall = String.format(COMMAND_OPEN_FIREWALL_PORT, CLOUDIFY_REST_PORT_NAME, "TCP", restPort);
		remoteExecutor.execute(hostAddress, details, cmdFireWall, DEFAULT_COMMAND_TIMEOUT);

		Integer webuiPort = this.cloud.getConfiguration().getComponents().getWebui().getPort();
		cmdFireWall = String.format(COMMAND_OPEN_FIREWALL_PORT, CLOUDIFY_WEBUI_PORT_NAME, "TCP", webuiPort);
		remoteExecutor.execute(hostAddress, details, cmdFireWall, DEFAULT_COMMAND_TIMEOUT);

		String cmd = "";
		if (this.firewallPorts != null) {
			for (Map<String, String> firewallPortsMap : this.firewallPorts) {
				String name = firewallPortsMap.get("name");
				String protocol = firewallPortsMap.get("protocol");
				String port = firewallPortsMap.get("port");

				if (StringUtils.isBlank(name) || StringUtils.isBlank(protocol) ||
						StringUtils.isBlank(port)) {

					Joiner.MapJoiner mapJoiner = Joiner.on(", ").withKeyValueSeparator("=");
					String firewallPortString = mapJoiner.join(firewallPortsMap);

					logger.warning("Firewall port ignored, its properties aren't valid : " + firewallPortString);

					continue;
				}

				// Port could be a range à a simple one
				// 7001 or 7001-7010 ([7001..7010])
				if (port.contains("-")) {
					String[] portsRange = port.split("-");
					int portStart = Integer.parseInt(portsRange[0]);
					int portEnd = Integer.parseInt(portsRange[1]);
					if (portsRange.length == 2 && portStart <= portEnd) {
						// Opening from port portStart -> portEnd (with +1 increment)
						for (int i = portStart; i <= portEnd; i++) {
							cmd = String.format(COMMAND_OPEN_FIREWALL_PORT, name.trim() + i, protocol.trim(), i);
							remoteExecutor.execute(hostAddress, details, cmd, DEFAULT_COMMAND_TIMEOUT);
						}
					}
				}
				else {
					// No port defined : skip
					int portNumber = Integer.parseInt(port);
					cmd = String.format(COMMAND_OPEN_FIREWALL_PORT, name.trim(), protocol.trim(), portNumber);
					remoteExecutor.execute(hostAddress, details, cmd, DEFAULT_COMMAND_TIMEOUT);
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

			Map<String, NetworkConfiguration> networkTemplates = cloudNetwork.getTemplates();

			VpnConfiguration vpnConfiguration = this.getVpnConfiguration(cloudNetwork);

			List<String> addressSpaces = MicrosoftAzureUtils.getListFromSplitedString(addressSpace, STRING_SEPERATOR);
			azureClient.createVirtualNetworkSite(addressSpaces, affinityGroup, networkName, subnet.getName(),
					subnet.getRange(), networkTemplates, dnsServers, vpnConfiguration, endTime);

			azureClient.createStorageAccount(affinityGroup, storageAccountName, endTime);

		} catch (final Exception e) {
			logger.warning("Failed creating management services : " + e.getMessage());
			if (cleanup) {
				try {
					logger.info("Cleaning up any services that may have already been started.");
					cleanup();
				} catch (final CloudProvisioningException e1) {
					// catch this because we want to throw the original exception. not the one that happened on cleanup.
					logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
					logger.fine(ExceptionUtils.getFullStackTrace(e1));
				}
			}
			throw new CloudProvisioningException(e);
		}

		int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		return doStartManagementMachines(endTime, numberOfManagementMachines);
	}

	/**
	 *
	 * TODO remove fields strict verification and make them optional like it's mentioned in azure api site <br />
	 * Supports only one local site
	 *
	 */
	private VpnConfiguration getVpnConfiguration(CloudNetwork cloudNetwork) {

		VpnConfiguration vpnConfiguration = null;

		String localSiteName = cloudNetwork.getCustom().get(AZURE_VPN_LOCALSITE_NAME);
		String vpnGatewayAddress = cloudNetwork.getCustom().get(AZURE_VPN_GATEWAY_ADDRESS);
		String addressSpacesString = cloudNetwork.getCustom().get(AZURE_VPN_ADDRESS_SPACE);
		String vpnSubnetAddressPrefix = cloudNetwork.getCustom().get(AZURE_VPN_SUBNET_ADDRESS_PREFIX);
		String vpnGatewayType = cloudNetwork.getCustom().get(AZURE_VPN_GATEWAY_TYPE);
		String vpnGatewayKey = cloudNetwork.getCustom().get(AZURE_VPN_GATEWAY_KEY);
		List<String> addressSpacesList = MicrosoftAzureUtils.getListFromSplitedString(addressSpacesString, ",");

		if (StringUtils.isNotBlank(vpnGatewayAddress) && StringUtils.isNotBlank(vpnGatewayType)
				&& StringUtils.isNotBlank(vpnGatewayKey) && StringUtils.isNotBlank(vpnSubnetAddressPrefix)
				&& addressSpacesList != null && !addressSpacesList.isEmpty()) {

			LocalNetworkSites localNetworkSites = new LocalNetworkSites();
			LocalNetworkSite localNetworkSite = new LocalNetworkSite(localSiteName, vpnGatewayAddress,
					new AddressSpace(addressSpacesList));

			localNetworkSites.getLocalNetworkSites().add(localNetworkSite);
			org.cloudifysource.esc.driver.provisioning.azure.model.Subnet subnet =
					new org.cloudifysource.esc.driver.provisioning.azure.model.Subnet();
			subnet.setName(AZURE_VPN_GATEWAY_SUBNET_NAME);
			subnet.setAddressPrefix(Arrays.asList(vpnSubnetAddressPrefix));

			// gateway configuration
			ConnectionsToLocalNetwork connectionsToLocalNetwork = new ConnectionsToLocalNetwork();
			LocalNetworkSiteRef siteRef = new LocalNetworkSiteRef(localSiteName, new Connection());
			connectionsToLocalNetwork.setLocalNetworkSiteRefs(Arrays.asList(siteRef));

			Gateway gateway = new Gateway(connectionsToLocalNetwork);
			vpnConfiguration = new VpnConfiguration(localNetworkSites, subnet, gateway, vpnGatewayType, vpnGatewayKey);

		} else {
			logger.fine("VPN configuration is skipped");
		}

		return vpnConfiguration;
	}

	private void cleanup() throws CloudProvisioningException {
		cleanup(CLEANUP_DEFAULT_TIMEOUT);
	}

	private void cleanup(long endTime) throws CloudProvisioningException {

		boolean deletedNetwork = false;
		boolean deletedStorage = false;
		Exception first = null;

		try {
			deletedNetwork = azureClient.deleteVirtualNetworkSite(networkName, endTime);
			deletedNetwork = true;
		} catch (final Exception e) {
			first = e;
			logger.warning("Failed deleting virtual network site " + networkName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		try {
			deletedStorage = azureClient.deleteStorageAccount(storageAccountName, endTime);
			deletedStorage = true;
		} catch (final Exception e) {
			if (first == null) {
				first = e;
			}
			logger.warning("Failed deleting storage account " + storageAccountName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		// delete storage accounts
		// TODO refactor

		List<String> failedDeleteOsStorageAccounts = new ArrayList<String>();

		Set<String> storageAccountsForOsDisks = getAllStorageAccountsByDiskType(AZURE_STORAGE_ACCOUNT);
		if (!storageAccountsForOsDisks.isEmpty()) {
			logger.fine("Cleaning storage accounts of OS disks ");
			for (String storage : storageAccountsForOsDisks) {
				try {
					azureClient.deleteStorageAccount(storage, endTime);

				} catch (Exception e) {
					logger.warning(String.format(
							"Failed deleting os storage account '%s'. It might be already in use.",
							storage));
					if (first == null) {
						first = e;
					}

					failedDeleteOsStorageAccounts.add(storage);
				}
			}
		} else {
			logger.fine("Not attempting to delete storage account for os disks");
		}

		Set<String> storageAccountsForDataDisks = getAllStorageAccountsByDiskType(AZURE_STORAGE_ACCOUNTS_DATA);

		List<String> failedDeleteDataStorageAccounts = new ArrayList<String>();
		if (!storageAccountsForDataDisks.isEmpty()) {
			logger.fine("Cleaning storage accounts for data disks");
			for (String storage : storageAccountsForDataDisks) {
				try {
					azureClient.deleteStorageAccount(storage, endTime);

				} catch (Exception e) {
					logger.warning(String.format(
							"Failed deleting data storage account '%s'. It might be already in use.",
							storage));
					if (first == null) {
						first = e;
					}
					failedDeleteDataStorageAccounts.add(storage);
				}
			}
		} else {
			logger.fine("Not attempting to delete storage account for data disks");
		}

		boolean deleteFileShareStorage = true;

		if (this.fileServiceStorageAccount != null) {

			try {
				logger.fine("Cleaning storage account for file share service");
				azureClient.deleteStorageAccount(this.fileServiceStorageAccount, endTime);
			} catch (Exception e) {
				logger.warning("Failed cleaning storage account for file service share");
				deleteFileShareStorage = false;
			}

		} else {
			logger.fine("Not attempting to delete storage account for file service");
		}

		List<String> failedDeleteVolumesStorageAccounts = this.cleanStorageAccountsOfVolumes(endTime);

		if (deletedNetwork && deletedStorage && failedDeleteOsStorageAccounts.isEmpty()
				&& failedDeleteDataStorageAccounts.isEmpty() && failedDeleteVolumesStorageAccounts.isEmpty()
				&& deleteFileShareStorage) {
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
			StringBuilder msg = new StringBuilder();
			msg.append("Not attempting to delete affinity group since is has some active services :  \n");

			if (!deletedNetwork) {
				msg.append(String.format("- virtual network '%s' \n", networkName));
			}

			if (!deletedStorage) {
				msg.append(String.format(String.format("- main storage account '%s' \n", storageAccountName)));
			}

			if (!failedDeleteOsStorageAccounts.isEmpty()) {
				msg.append(String.format(String.format("- storage accounts for OS disks %s \n",
						failedDeleteOsStorageAccounts.toString())));
			}

			if (!failedDeleteDataStorageAccounts.isEmpty()) {
				msg.append(String.format(String.format("- storage accounts for data disks %s \n",
						failedDeleteDataStorageAccounts.toString())));
			}

			if (!failedDeleteVolumesStorageAccounts.isEmpty()) {
				msg.append(String.format(String.format("- storage accounts for volumes disks %s \n",
						failedDeleteVolumesStorageAccounts.toString())));
			}

			if (!deleteFileShareStorage) {
				msg.append(String.format(String.format("- storage account for file share %s \n",
						this.fileServiceStorageAccount)));
			}

			logger.warning(msg.toString());
		}

		if (first != null) {
			throw new CloudProvisioningException(first);
		}
	}

	@SuppressWarnings("unchecked")
	private Set<String> getAllStorageAccountsByDiskType(String property) {

		Set<String> storages = new HashSet<String>();

		Map<String, ComputeTemplate> computeTemplates = this.cloud.getCloudCompute().getTemplates();
		if (!computeTemplates.isEmpty()) {

			for (ComputeTemplate template : computeTemplates.values()) {
				List<String> templateStorages = (List<String>) template.getCustom().get(property);
				if (templateStorages != null && !templateStorages.isEmpty()) {
					storages.addAll(templateStorages);
				}
			}
		}
		return storages;
	}

	private List<String> cleanStorageAccountsOfVolumes(long endTime) throws CloudProvisioningException {

		List<String> failedDeletedStorageAccounts = new ArrayList<String>();

		Map<String, StorageTemplate> storageTemplates = cloud.getCloudStorage().getTemplates();
		if (storageTemplates == null || storageTemplates.isEmpty()) {
			logger.info("Skipping Cleaning volumes because no storage templates were defined in cloud configuration");
		} else {
			logger.fine("Cleaning storage accounts of volumes");
			for (Entry<String, StorageTemplate> entry : storageTemplates.entrySet()) {
				StorageTemplate storageTemplate = entry.getValue();

				@SuppressWarnings("unchecked")
				List<String> storageAccounts = (List<String>) storageTemplate.getCustom().get(AZURE_STORAGE_ACCOUNT);
				if (storageAccounts != null) {
					for (String storage : storageAccounts) {
						try {
							azureClient.deleteStorageAccount(storage, endTime);
						} catch (Exception e) {
							failedDeletedStorageAccounts.add(storage);
							logger.warning("Failed deleting storage account " + storage);
						}
					}
				}
			}
		}

		return failedDeletedStorageAccounts;
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
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {

		final long endTime =
				System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(stopManagementMachinesTimeoutInMinutes);
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

		if (cleanup) {
			logger.info("Cleaning up management services");
			cleanup(endTime);
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
			}
			else {
				if (disk.getName().contains(serverNamePrefix)) {
					try {
						logger.info("Detected a zombie OS Disk with name " + disk.getName() + ", Deleting it.");
						azureClient.deleteDisk(disk.getName(), true, endTime);
					} catch (final Exception e) {
						throw new CloudProvisioningException(e);
					}
				}
			}

			// delete disks (volumes) created by the Storage Driver
			List<String> prefixes = this.getStorageAccountsPrefixesInStorageTemplates(cloud.getCloudStorage()
					.getTemplates());

			if (MicrosoftAzureUtils.stringContainsAny(disk.getMediaLink(), prefixes)) {
				try {
					logger.info("Detected a volume Disk with name " + disk.getName() + ", Deleting it.");
					azureClient.deleteDisk(disk.getName(), true, endTime);
				} catch (final Exception e) {
					throw new CloudProvisioningException(e);
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
			throw new CloudProvisioningException(exceptionOnStopMachines.get(0).getMessage(),
					exceptionOnStopMachines.get(0));
		}
	}

	private List<String> getStorageAccountsPrefixesInStorageTemplates(Map<String, StorageTemplate> storageTemplates) {

		if (storageTemplates != null && !storageTemplates.isEmpty()) {
			List<String> prefixes = new ArrayList<String>();

			for (Entry<String, StorageTemplate> entry : storageTemplates.entrySet()) {
				StorageTemplate storageTemplate = entry.getValue();
				prefixes.add(storageTemplate.getNamePrefix());
			}

			return prefixes;
		}

		return null;
	}

	@Override
	public String getCloudName() {
		return "azure";
	}

	@Override
	public void close() {
		if (azureClient != null) {
			azureClient.destroy();
		}
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

	private boolean stopManagementMachine(final String hostedServiceName, final String deploymentName,
			final long endTime) throws CloudProvisioningException, TimeoutException {
		try {
			azureClient.deleteVirtualMachineByDeploymentName(hostedServiceName, deploymentName, endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		} catch (InterruptedException e) {
			throw new CloudProvisioningException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private InputEndpoints createInputEndPoints() throws MicrosoftAzureException {

		InputEndpoints inputEndpoints = new InputEndpoints();

		// Add End Point for each port
		Object objects = this.template.getCustom().get(AZURE_ENDPOINTS);
		if (objects != null) {
			List<Map<String, Object>> endpoints = (List<Map<String, Object>>) objects;
			if (endpoints != null) {
				for (Map<String, Object> endpointMap : endpoints) {

					try {

						// set endpoint basic information
						if (endpointMap.get(ENDPOINT_BASIC) != null) {
							Map<String, String> endPointBasic = (Map<String, String>) endpointMap.get(ENDPOINT_BASIC);
							InputEndpoint inputEndpoint = getEndPointBasic(endPointBasic);

							// set lb endpoint
							Object endPointLbObject = endpointMap.get(ENDPOINT_LB);
							if (endPointLbObject != null) {
								Map<String, String> endPointLb = (Map<String, String>) endPointLbObject;
								this.setEndPointLb(endPointLb, inputEndpoint);
							}

							// set Acl rules
							Object endPointAclObject = endpointMap.get(ENDPOINT_ACL);
							if (endPointAclObject != null) {
								List<Map<String, String>> endPointAcl = (List<Map<String, String>>) endPointAclObject;
								setEndPointAcl(endPointAcl, inputEndpoint);
							}

							inputEndpoints.getInputEndpoints().add(inputEndpoint);
						}

					} catch (Exception e) {
						throw new MicrosoftAzureException("Failed processing endpoints, please check template "
								+ cloudTemplateName, e);
					}
				}
			}

		}

		// Open WEBUI and REST ports for management machines
		if (this.generateEndpoints) {
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
		}
		return inputEndpoints;

	}

	private InputEndpoint getEndPointBasic(Map<String, String> endpointMap) throws MicrosoftAzureException {

		String name = endpointMap.get(ENDPOINT_NAME);
		String protocol = endpointMap.get(ENDPOINT_PROTOCOL);
		String portStr = endpointMap.get(ENDPOINT_PORT);
		String localPortStr = endpointMap.get(ENDPOINT_LOCALPORT);

		// skip endPoint, strict verification
		if (StringUtils.isNotBlank(localPortStr) && StringUtils.isNotBlank(protocol)
				&& StringUtils.isNotBlank(name)) {

			InputEndpoint endpoint = new InputEndpoint();

			if (StringUtils.isNotBlank(portStr)) {
				endpoint.setPort(Integer.parseInt(portStr));
			} // otherwise, public port number will be generated by azure

			endpoint.setLocalPort(Integer.parseInt(localPortStr));
			endpoint.setName(name);
			endpoint.setProtocol(protocol);

			return endpoint;

		} else {
			String endPointValuesError = String.format("Failed provisioning VM, please check"
					+ " endPoint required elements in compute template '%s' : [name: '%s', protocol: '%s', "
					+ "localPort: '%s']", this.configuration.getCloudTemplate(), name, protocol, localPortStr);
			logger.severe(endPointValuesError);
			throw new MicrosoftAzureException(endPointValuesError);
		}

	}

	private void setEndPointLb(Map<String, String> endpointMap, InputEndpoint endpoint)
			throws MicrosoftAzureException {

		// manage load balancer, protocol, port probe are required
		String loadBalancedSet = endpointMap.get(ENDPOINT_LOADBALANCEDSET);
		String probePort = endpointMap.get(ENDPOINT_PROBE_PORT);
		String probeProtocol = endpointMap.get(ENDPOINT_PROBE_PROTOCOL);

		if (StringUtils.isNotBlank(loadBalancedSet) && StringUtils.isNotBlank(probePort)
				&& StringUtils.isNotBlank(probeProtocol)) {

			if (loadBalancedSet.trim().length() > 15 || loadBalancedSet.trim().length() < 3) {
				String loadbanacedNameLenghtError =
						String.format(
								"Failed provisioning VM,"
										+ " please check load balancer name lenght in compute template '%s'. It should be"
										+ " between 3 and 15 characters",
								this.configuration.getCloudTemplate(), loadBalancedSet);
				logger.severe(loadbanacedNameLenghtError);
				throw new MicrosoftAzureException(loadbanacedNameLenghtError);
			}

			LoadBalancerProbe lbp = new LoadBalancerProbe(probePort.trim(), probeProtocol.trim());
			endpoint.setLoadBalancedEndpointSetName(loadBalancedSet);
			endpoint.setLoadBalancerProbe(lbp);
		}

	}

	private void setEndPointAcl(List<Map<String, String>> endpointRulesList, InputEndpoint endpoint)
			throws MicrosoftAzureException {

		if (endpointRulesList != null) {

			EndpointAcl acl = new EndpointAcl();
			Rules rules = new Rules();
			for (Map<String, String> rule : endpointRulesList) {

				String order = String.valueOf(rule.get(ENDPOINT_ORDER));
				String action = rule.get(ENDPOINT_ACTION);
				String subnet = rule.get(ENDPOINT_SUBNET);
				String description = rule.get(ENDPOINT_DESCRIPTION);

				if (StringUtils.isNotBlank(order) && StringUtils.isNotBlank(action)
						&& StringUtils.isNotBlank(subnet)) {

					// check action value (should be : permit or deny)
					if (!action.equalsIgnoreCase(ENDPOINT_ACTION_DENY)
							&& !action.equalsIgnoreCase(ENDPOINT_ACTION_PERMIT)) {

						String actionError =
								String.format(
										"Failed provisioning VM,"
												+ " please check action value for endpoint '%' in compute template '%s'. Possible values are: permit, deny",
										endpoint.getName(), this.configuration.getCloudTemplate());
						logger.severe(actionError);
						throw new MicrosoftAzureException(actionError);
					}

					try {
						int parseInt = Integer.parseInt(order);
						rules.getRules().add(new Rule(parseInt, action, subnet, description));

					} catch (Exception e) {
						String invalidValue =
								String.format(
										"Invalid value for ACL order "
												+ " please check endpoint '%' in compute template '%s'. Possible values are: permit, deny",
										endpoint.getName(), this.configuration.getCloudTemplate());
						logger.severe(invalidValue);
						throw new MicrosoftAzureException(invalidValue);
					}

				} else {
					String actionError =
							String.format(
									"Failed provisioning VM,"
											+ " please check required values for endpoint ACL '%' in compute template '%s'.",
									endpoint.getName(), this.configuration.getCloudTemplate());
					logger.severe(actionError);
					throw new MicrosoftAzureException(actionError);
				}
			}
			acl.setRules(rules);
			endpoint.setEndpointAcl(acl);
		}

	}

	private Boolean isWindowsVM() {
		return RemoteExecutionModes.WINRM.equals(this.remoteExecutionMode)
				|| ScriptLanguages.WINDOWS_BATCH.equals(this.scriptLanguage);
	}

	@Override
	public Object getComputeContext() {
		return this;
	}

	@Override
	public void onServiceUninstalled(long duration, TimeUnit unit) {
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
			String machineObjectOU = domainJoinMap.get(MACHINE_OBJECT_OU);

			if (StringUtils.isNotBlank(domain) && StringUtils.isNotBlank(userName)
					&& StringUtils.isNotBlank(password) && StringUtils.isNotBlank(joinDomain)) {

				JoinCredentials jc = new JoinCredentials();
				jc.setDomain(domain);
				jc.setUserNamer(userName);
				jc.setPassword(password);

				domainJoin = new DomainJoin();
				domainJoin.setCredentials(jc);
				domainJoin.setJoinDomain(joinDomain);
				if (StringUtils.isNotEmpty(machineObjectOU)) {
					domainJoin.setMachineObjectOU(machineObjectOU);
				}
			}
		}
		return domainJoin;
	}

	public void setAzureDeploymentContext(AzureDeploymentContext azureDeploymentContext) {
		this.azureDeploymentContext = azureDeploymentContext;
	}

}
