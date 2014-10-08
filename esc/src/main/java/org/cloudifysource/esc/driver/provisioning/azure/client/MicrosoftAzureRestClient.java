/******************************************************************************
 *
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved * * Licensed under the Apache License, Version
 * 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of
 * the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in
 * writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions
 * and * limitations under the License. *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.azure.model.AddressAvailability;
import org.cloudifysource.esc.driver.provisioning.azure.model.AddressSpace;
import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.AttachedTo;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.ConfigurationSets;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateAffinityGroup;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateHostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.CreateStorageServiceInput;
import org.cloudifysource.esc.driver.provisioning.azure.model.DataVirtualHardDisk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployments;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.Error;
import org.cloudifysource.esc.driver.provisioning.azure.model.GlobalNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Operation;
import org.cloudifysource.esc.driver.provisioning.azure.model.PersistentVMRole;
import org.cloudifysource.esc.driver.provisioning.azure.model.RestartRoleOperation;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstance;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.Subnet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Subnets;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/********************************************************************************
 * A REST client implementation for the Azure REST API. this client is designed for using azure infrastructure as an
 * IaaS. each VM is provisioned onto a separate cloud service that belong to the same virtual network site. this way all
 * VM's are assigned public and private IP. and all VM's can be either a back end of a front end of you application.
 * authentication is achieved by using self-signed certificates (OpenSSL, makecert)
 *
 * @author elip
 ********************************************************************************/

public class MicrosoftAzureRestClient {

	private static final int HTTP_NOT_FOUND = 404;
	private static final int HTTP_OK = 200;
	private static final int HTTP_CREATED = 201;
	private static final int HTTP_ACCEPTED = 202;

	private static final char BAD_CHAR = 65279;

	private String affinityPrefix;
	private String cloudServicePrefix;
	private String storagePrefix;

	private Lock pendingRequest = new ReentrantLock(true);

	private MicrosoftAzureRequestBodyBuilder requestBodyBuilder;

	// Azure Management Service API End Point
	private static final String CORE_MANAGEMENT_END_POINT = "https://management.core.windows.net/";

	// Header names and values
	private static final String X_MS_VERSION_HEADER_NAME = "x-ms-version";
	private static final String X_MS_VERSION_HEADER_VALUE = "2014-06-01";

	private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
	private static final String CONTENT_TYPE_HEADER_VALUE = "application/xml";

	private static final String FAILED = "Failed";
	private static final String SUCCEEDED = "Succeeded";
	private static final String IN_PROGRESS = "InProgress";

	private static final int MAX_RETRIES = 5;
	private static final long DEFAULT_POLLING_INTERVAL = 5 * 1000; // 5 seconds
	private static final long ESTIMATED_TIME_TO_START_VM = 5 * 60 * 1000; // 5 minutes

	private WebResource resource;
	private Client client;

	private String subscriptionId;

	private MicrosoftAzureSSLHelper sslHelper;
	private String virtualNetwork;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public MicrosoftAzureRestClient(final String subscriptionId,
			final String pathToPfx, final String pfxPassword,
			final String affinityPrefix, final String cloudServicePrefix,
			final String storagePrefix) {
		this.subscriptionId = subscriptionId;
		this.affinityPrefix = affinityPrefix;
		this.cloudServicePrefix = cloudServicePrefix;
		this.storagePrefix = storagePrefix;
		this.init(pathToPfx, pfxPassword, affinityPrefix, cloudServicePrefix,
				storagePrefix);
	}

	public MicrosoftAzureRestClient() {

	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(final String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getAffinityPrefix() {
		return affinityPrefix;
	}

	public void setAffinityPrefix(final String affinityPrefix) {
		this.affinityPrefix = affinityPrefix;
	}

	public String getCloudServicePrefix() {
		return cloudServicePrefix;
	}

	public void setCloudServicePrefix(final String cloudServicePrefix) {
		this.cloudServicePrefix = cloudServicePrefix;
	}

	public String getStoragePrefix() {
		return storagePrefix;
	}

	public void setStoragePrefix(final String storagePrefix) {
		this.storagePrefix = storagePrefix;
	}

	/**
	 *
	 * @param logger
	 *            - the logger to add to the client
	 */
	public void setLoggingFilter(final Logger logger) {
		this.client.addFilter(new LoggingFilter(logger));
	}

	private void init(final String pathToPfx, final String pfxPassword,
			final String affinityPrefix, final String cloudServicePrefix,
			final String storagePrefix) {
		try {
			this.sslHelper = new MicrosoftAzureSSLHelper(pathToPfx, pfxPassword);
			this.client = createClient(sslHelper.createSSLContext());
			this.resource = client.resource(CORE_MANAGEMENT_END_POINT);
			this.requestBodyBuilder = new MicrosoftAzureRequestBodyBuilder(
					affinityPrefix, cloudServicePrefix, storagePrefix);
		} catch (final Exception e) {
			throw new RuntimeException("Failed initializing rest client : " + e.getMessage(), e);
		}
	}

	private Client createClient(final SSLContext context) {
		ClientConfig config = new DefaultClientConfig();
		config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, context));
		Client httpClient = Client.create(config);
		httpClient.setConnectTimeout(CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		httpClient.setReadTimeout(CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		return httpClient;
	}

	/**
	 *
	 * @param affinityGroup
	 *            - the affinity group for the cloud service.
	 * @param endTime
	 *            .
	 *
	 * @return - the newly created cloud service name.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public String createCloudService(final String affinityGroup, String cloudServiceNameOverride,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		logger.fine(getThreadIdentity() + "Creating cloud service");
		CreateHostedService createHostedService =
				requestBodyBuilder.buildCreateCloudService(affinityGroup, cloudServiceNameOverride);

		String serviceName = null;
		try {
			String xmlRequest = MicrosoftAzureModelUtils.marshall(createHostedService, false);
			ClientResponse response = doPost("/services/hostedservices", xmlRequest);
			String requestId = extractRequestId(response);
			waitForRequestToFinish(requestId, endTime);
			serviceName = createHostedService.getServiceName();
			logger.info("Cloud service created : " + serviceName);
		} catch (final Exception e) {
			logger.warning("Failed to create cloud service : " + e.getMessage());
			if (e instanceof MicrosoftAzureException) {
				throw (MicrosoftAzureException) e;
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
		}
		return serviceName;
	}

	/**
	 * this method creates a storage account with the given name, or does nothing if the account exists.
	 *
	 * @param affinityGroup
	 *            - the affinity group for the storage account.
	 * @param storageAccountName
	 *            - the name for the storage account to create.
	 * @param endTime
	 *            .
	 *
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createStorageAccount(final String affinityGroup, final String storageAccountName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		CreateStorageServiceInput createStorageServiceInput =
				requestBodyBuilder.buildCreateStorageAccount(affinityGroup, storageAccountName);

		if (storageExists(storageAccountName)) {
			logger.info("Using an already existing storage account : " + storageAccountName);
			return;
		}

		logger.info("Creating a storage account : " + storageAccountName);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(createStorageServiceInput, false);
		ClientResponse response = doPost("/services/storageservices", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);

		logger.fine("Created a storage account : " + storageAccountName);

	}

	/**
	 * this method creates a virtual network with the given name, or does nothing if the network exists.
	 *
	 * @param addressSpace
	 *            - CIDR notation specifying the address space for the virtual network.
	 * @param affinityGroup
	 *            - the affinity group for this virtual network
	 * @param networkSiteName
	 *            - the name for the network to create
	 * @param endTime
	 *            .
	 *
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createVirtualNetworkSite(final String addressSpace, final String affinityGroup,
			final String networkSiteName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		VirtualNetworkConfiguration virtualNetworkConfiguration = getVirtualNetworkConfiguration();
		VirtualNetworkSites virtualNetworkSites = virtualNetworkConfiguration.getVirtualNetworkSites();
		if (virtualNetworkSites != null && virtualNetworkSites.contains(networkSiteName)) {
			logger.info("Using an already existing virtual netowrk site : " + networkSiteName);
			return;
		} else {
			if (virtualNetworkSites == null) {
				virtualNetworkSites = new VirtualNetworkSites();
			}
		}

		logger.info("Creating virtual network site : " + networkSiteName);

		VirtualNetworkSite newSite = new VirtualNetworkSite();
		AddressSpace address = new AddressSpace();
		address.getAddressPrefix().add(addressSpace);
		newSite.setAddressSpace(address);
		newSite.setAffinityGroup(affinityGroup);
		newSite.setName(networkSiteName);
		Subnet subnet = new Subnet();
		subnet.setName("subnet-" + networkSiteName);
		subnet.getAddressPrefix().add(addressSpace);
		Subnets subnets = new Subnets();
		subnets.getSubnets().add(subnet);
		newSite.setSubnets(subnets);

		virtualNetworkSites.getVirtualNetworkSites().add(newSite);

		setNetworkConfiguration(endTime, virtualNetworkConfiguration);
		logger.fine("Created virtual network site : " + networkSiteName);
	}

	public void createVirtualNetworkSite(String addressSpace, String affinityGroup, String networkSiteName,
			String subnetName, String subnetAddr, long endTime) throws MicrosoftAzureException, TimeoutException,
			InterruptedException {

		VirtualNetworkConfiguration virtualNetworkConfiguration = getVirtualNetworkConfiguration();
		VirtualNetworkSites virtualNetworkSites = virtualNetworkConfiguration.getVirtualNetworkSites();

		if (virtualNetworkSites == null) {
			logger.info("Creating virtual network sites");
			virtualNetworkSites = new VirtualNetworkSites();
		}

		VirtualNetworkSite virtualNetworkSite = null;
		if (!virtualNetworkSites.contains(networkSiteName)) {
			VirtualNetworkSite newSite = new VirtualNetworkSite();
			AddressSpace address = new AddressSpace();
			address.getAddressPrefix().add(addressSpace);
			newSite.setAddressSpace(address);
			newSite.setAffinityGroup(affinityGroup);
			newSite.setName(networkSiteName);
			virtualNetworkSites.getVirtualNetworkSites().add(newSite);
		} else {
			logger.info("Using an already existing virtual network site : " + networkSiteName);
		}

		virtualNetworkSite = virtualNetworkSites.getVirtualNetworkSite(networkSiteName);
		if (virtualNetworkSite.getSubnets() == null) {
			virtualNetworkSite.setSubnets(new Subnets());
		} else {
			logger.info("Creating subnets for virtual network site : " + networkSiteName);
		}

		boolean shouldUpdateOrCreate = false;
		if (!virtualNetworkSite.getSubnets().contains(subnetName)) {
			logger.info("Creating the subnet: " + subnetName);
			if (virtualNetworkSite.getSubnets() == null) {
				virtualNetworkSite.setSubnets(new Subnets());
			}
			Subnet subnet = new Subnet();
			subnet.setName(subnetName);
			subnet.getAddressPrefix().add(subnetAddr);
			virtualNetworkSite.getSubnets().getSubnets().add(subnet);
			shouldUpdateOrCreate = true;
		} else {
			logger.info("Using an already existing subnet '" + subnetName + "' from virtual network site '"
					+ networkSiteName + "'");
		}

		if (shouldUpdateOrCreate) {
			setNetworkConfiguration(endTime, virtualNetworkConfiguration);
			logger.fine("Created/Updated virtual network site : " + networkSiteName);
		} else {
			logger.fine("Using existing virtual network site configuration: " + networkSiteName);
		}

	}

	public void addSubnetToVirtualNetwork(String networkSiteName, String subnetName, String subnetAddr, long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		VirtualNetworkConfiguration virtualNetworkConfiguration = this.getVirtualNetworkConfiguration();
		VirtualNetworkSites virtualNetworkSites = virtualNetworkConfiguration.getVirtualNetworkSites();

		if (!virtualNetworkSites.contains(networkSiteName)) {
			throw new IllegalStateException("Missing network '" + networkSiteName + "' in Microsoft Azure");
		}

		VirtualNetworkSite virtualNetworkSite = virtualNetworkSites.getVirtualNetworkSite(networkSiteName);
		if (virtualNetworkSite.getSubnets().contains(subnetName)) {
			// The subnet already exist
			logger.info("Subnet '" + subnetName + "' already exist");
		} else {
			logger.info("Creating the subnet: " + subnetName);
			Subnet subnet = new Subnet();
			subnet.setName(subnetName);
			subnet.getAddressPrefix().add(subnetAddr);
			virtualNetworkSite.getSubnets().getSubnets().add(subnet);
			this.setNetworkConfiguration(endTime, virtualNetworkConfiguration);
			logger.fine("Updated virtual network site : " + networkSiteName);
		}
	}

	public void removeSubnetByName(String networkSiteName, String subnetName, long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {
		VirtualNetworkConfiguration virtualNetworkConfiguration = this.getVirtualNetworkConfiguration();
		VirtualNetworkSites virtualNetworkSites = virtualNetworkConfiguration.getVirtualNetworkSites();
		VirtualNetworkSite virtualNetworkSite = virtualNetworkSites.getVirtualNetworkSite(networkSiteName);
		Subnets subnets = virtualNetworkSite.getSubnets();
		Iterator<Subnet> iterator = subnets.getSubnets().iterator();

		boolean shouldUpdate = false;
		while (iterator.hasNext()) {
			Subnet next = iterator.next();
			if (next.getName().equals(subnetName)) {
				iterator.remove();
				shouldUpdate = true;
				break;
			}
		}
		if (shouldUpdate) {
			this.setNetworkConfiguration(endTime, virtualNetworkConfiguration);
		} else {
			logger.warning("Subnet '" + subnetName + "' not found in network '" + networkSiteName + "'");
		}
	}

	/**
	 * this method creates an affinity group with the given name, or does nothing if the group exists.
	 *
	 * @param affinityGroup
	 *            - the name of the affinity group to create
	 * @param location
	 *            - one of MS Data Centers locations.
	 * @param endTime
	 *            .
	 *
	 * @throws InterruptedException .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public void createAffinityGroup(final String affinityGroup, final String location, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		CreateAffinityGroup createAffinityGroup = requestBodyBuilder
				.buildCreateAffinity(affinityGroup, location);

		if (affinityExists(affinityGroup)) {
			logger.info("Using an already existing affinity group : " + affinityGroup);
			return;
		}

		logger.info("Creating affinity group : " + affinityGroup);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(createAffinityGroup, false);
		ClientResponse response = doPost("/affinitygroups", xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Created affinity group : " + affinityGroup);
	}

	/**
	 * This method creates a virtual machine and a corresponding cloud service. the cloud service will use the affinity
	 * group specified by deploymentDesc.getAffinityGroup(); If another request was made this method will wait until the
	 * pending request is finished.
	 *
	 * If a failure happened after the cloud service was created, this method will delete it and throw.
	 *
	 * @param deplyomentDesc
	 *            .
	 * @param endTime
	 *            .
	 * @return an instance of {@link RoleDetails} containing the ip addresses information for the created role.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public RoleDetails createVirtualMachineDeployment(
			final CreatePersistentVMRoleDeploymentDescriptor deploymentDesc,
			final boolean isWindows, final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis - ESTIMATED_TIME_TO_START_VM;
		if (lockTimeout < 0) {
			throw new MicrosoftAzureException(
					"Aborted request to provision virtual machine. "
							+ "The timeout is less then the estimated time to provision the machine");
		}

		logger.fine(getThreadIdentity() + "Waiting for pending request lock for lock " + pendingRequest.hashCode());
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout, TimeUnit.MILLISECONDS);

		String serviceName = null;
		Deployment deployment = null;
		String deploymentSlot = deploymentDesc.getDeploymentSlot();

		if (lockAcquired) {

			logger.fine(getThreadIdentity() + "Lock acquired : " + pendingRequest.hashCode());
			logger.fine(getThreadIdentity() + "Executing a request to provision a new virtual machine");

			try {

				// is there a hosted service to use ?
				if (deploymentDesc.getHostedServiceName() == null) {

					// final cloud service name with extra characters (count)
					if (deploymentDesc.isGenerateCloudServiceName()) {
						serviceName = createCloudService(deploymentDesc.getAffinityGroup(), null, endTime);

						// create a cloud service with the specified name in compute template
					} else {
						serviceName = createCloudService(deploymentDesc.getAffinityGroup(),
								deploymentDesc.getCloudServiceName(), endTime);
					}

					deploymentDesc.setHostedServiceName(serviceName);
					deploymentDesc.setDeploymentName(serviceName);
				} else {
					// use existing cloud service
					serviceName = deploymentDesc.getHostedServiceName();
				}

				// check static IP(s) availability
				// which is skipped if no private ip was defined in the current compute template
				if (deploymentDesc.getIpAddresses() != null) {
					String availableIp = setAvailableIpIfExist(deploymentDesc.getIpAddresses(),
							deploymentDesc.getNetworkName(), deploymentDesc.getSubnetName(), endTime);

					if (availableIp == null) {
						String noIpAvailableString = String.format("The specified Ip addresses '%s' are not available",
								deploymentDesc.getIpAddresses().toString());

						logger.severe(noIpAvailableString);
						throw new MicrosoftAzureException("Can't provision VM :" + noIpAvailableString);

					}
					deploymentDesc.setAvailableIp(availableIp);
				}

				// Add role to specified deployment
				if (deploymentDesc.isAddToExistingDeployment()) {
					PersistentVMRole persistentVMRole = requestBodyBuilder.buildPersistentVMRole(deploymentDesc,
							isWindows);
					String xmlRequest = MicrosoftAzureModelUtils.marshall(persistentVMRole, false);
					logger.fine(getThreadIdentity()
							+ String.format("Launching virtual machine '%s', in current deployment '%s'",
									deploymentDesc.getRoleName(), deploymentDesc.getDeploymentName()));

					ClientResponse response = doPost("/services/hostedservices/" + serviceName + "/deployments/" +
							deploymentDesc.getDeploymentName() + "/roles", xmlRequest);
					String requestId = extractRequestId(response);
					waitForRequestToFinish(requestId, endTime);
				} else {
					// regular deployment
					deployment = requestBodyBuilder.buildDeployment(deploymentDesc, isWindows);
					String xmlRequest = MicrosoftAzureModelUtils.marshall(deployment, false);

					logger.fine(getThreadIdentity() + "Launching virtual machine : "
							+ deploymentDesc.getRoleName());

					ClientResponse response = doPost("/services/hostedservices/"
							+ serviceName + "/deployments", xmlRequest);
					String requestId = extractRequestId(response);
					waitForRequestToFinish(requestId, endTime);
				}

				logger.fine(getThreadIdentity() + "About to release lock " + pendingRequest.hashCode());
				pendingRequest.unlock();

			} catch (final Exception e) {
				logger.log(Level.FINE, getThreadIdentity() + "A failure occured : about to release lock "
						+ pendingRequest.hashCode(), e);
				if (serviceName != null) {
					try {
						// delete the dedicated cloud service that was created for the virtual machine.
						deleteCloudService(serviceName, endTime);
					} catch (final Exception e1) {
						logger.warning("Failed deleting cloud service " + serviceName + " : " + e1.getMessage());
						logger.finest(ExceptionUtils.getFullStackTrace(e1));
					}
				}
				pendingRequest.unlock();
				if (e instanceof MicrosoftAzureException) {
					throw (MicrosoftAzureException) e;
				}
				if (e instanceof TimeoutException) {
					throw (TimeoutException) e;
				}
				if (e instanceof InterruptedException) {
					throw (InterruptedException) e;
				}
				throw new MicrosoftAzureException(e);
			}
		} else {
			throw new TimeoutException(
					"Failed to acquire lock for deleteDeployment request after + "
							+ lockTimeout + " milliseconds");
		}

		Deployment deploymentResponse = null;
		try {
			deploymentResponse = waitForDeploymentStatus("Running", serviceName, deploymentSlot, endTime);
			deploymentResponse = waitForRoleInstanceStatus("ReadyRole", serviceName, deploymentSlot, endTime);
		} catch (final Exception e) {
			logger.fine("Error while waiting for VM status : " + e.getMessage());
			// the VM was created but with a bad status
			// TODO clean the current VM
			// deleteVirtualMachineByDeploymentName(serviceName, deployment.getName(), endTime);
			if (e instanceof MicrosoftAzureException) {
				throw (MicrosoftAzureException) e;
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
			throw new MicrosoftAzureException(e);
		}

		// ***************************************
		// Add a data disk when creating a new VM
		if (deploymentDesc.getDataDiskSize() != null) {
			this.addDataDiskToVM(deploymentDesc.getHostedServiceName(), deploymentDesc.getDeploymentName(),
					deploymentDesc.getRoleName(), deploymentDesc.getStorageAccountName(),
					deploymentDesc.getDataDiskSize(), endTime);
		}
		// ***************************************

		RoleDetails roleAddressDetails = new RoleDetails();

		String privateIp = null;
		// get instanceRole from details
		if (deploymentDesc.isAddToExistingDeployment()) {
			RoleInstance roleInstance = deploymentResponse.getRoleInstanceList().
					getInstanceRoleByRoleName(deploymentDesc.getRoleName());
			privateIp = roleInstance.getIpAddress();

		} else {
			privateIp = deploymentResponse.getRoleInstanceList().getRoleInstances().get(0).getIpAddress();
		}

		roleAddressDetails.setId(deploymentResponse.getPrivateId());
		roleAddressDetails.setPrivateIp(privateIp);
		ConfigurationSets configurationSets = deploymentResponse.getRoleList()
				.getRoles().get(0).getConfigurationSets();

		// TODO handle for vms on the same cloud service
		String publicIp = null;
		for (ConfigurationSet configurationSet : configurationSets) {
			if (configurationSet instanceof NetworkConfigurationSet) {
				NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
				if (networkConfigurationSet.getInputEndpoints() != null) {
					// TODO if no endpoint has been defined, the VM will note have a public IP
					// Should retrieve the public ip in the cloud service.
					publicIp = networkConfigurationSet.getInputEndpoints()
							.getInputEndpoints().get(0).getvIp();
				}
			}
		}

		roleAddressDetails.setPublicIp(publicIp);
		return roleAddressDetails;
	}

	// Exist
	private boolean isSubnetExist(String subnetName, String virtualNetwork, long endTime)
			throws MicrosoftAzureException,
			TimeoutException, InterruptedException {
		boolean exist = false;

		ClientResponse response;

		if (subnetName != null && !subnetName.trim().isEmpty()) {
			response = doGet("/services/networking/media");
			checkForError(response);
			String requestId = extractRequestId(response);
			waitForRequestToFinish(requestId, endTime);

			String responseXmlBodyString = response.getEntity(String.class);

			// Response body is a file format (contains \n \r) and starts with prolog <?xml version="1.0"...
			// Clear the XML declaration from the string
			// TODO find a better way (jaxb properties, libs...)
			int i = responseXmlBodyString.indexOf("?>") + 2;
			String xmlstring = responseXmlBodyString.substring(i);
			xmlstring = xmlstring.replaceAll(System.getProperty("line.separator"), "");

			GlobalNetworkConfiguration globalNetworkConfiguration =
					(GlobalNetworkConfiguration) MicrosoftAzureModelUtils.unmarshall(xmlstring);

			VirtualNetworkSite virtualNetworkSite = globalNetworkConfiguration.getVirtualNetworkConfiguration()
					.getVirtualNetworkSiteConfigurationByName(virtualNetwork);

			if (virtualNetworkSite != null) {
				exist = virtualNetworkSite.isSubnetExist(subnetName);
			}
		}

		return exist;
	}

	private String setAvailableIpIfExist(List<String> ips, String virtualNetwork, String subnetName, long endTime)
			throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		if (ips != null && !ips.isEmpty()) {
			ClientResponse response;

			for (String ip : ips) {

				StringBuilder sb = new StringBuilder();
				sb.append("/services/networking/");
				sb.append(virtualNetwork);
				sb.append("?op=checkavailability");
				sb.append("&address=");
				sb.append(ip);
				response = doGet(sb.toString());
				checkForError(response);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);

				AddressAvailability aa = (AddressAvailability) MicrosoftAzureModelUtils.
						unmarshall(response.getEntity(String.class));

				if (aa.isAvailable()) {
					// check subnet availability
					if (isSubnetExist(subnetName, virtualNetwork, endTime)) {
						return ip;
					} else {
						String subnetNotExistString =
								String.format("The specified subnet '%s' doesn't exist in network '%s' ",
										subnetName, virtualNetwork);

						logger.severe(subnetNotExistString);
						throw new MicrosoftAzureException("Can't provision VM :" + subnetNotExistString);
					}
				}
			}
		}

		return null;
	}

	/**
	 * @param vmStatus
	 */
	private boolean checkVirtualMachineStatusForError(final String vmStatus) {
		return (vmStatus.equals("FailedStartingRole")
				|| vmStatus.equals("FailedStartingVM")
				|| vmStatus.equals("UnresponsiveRole")
				|| vmStatus.equals("CyclingRole"));
	}

	/**
	 *
	 * @return - the response body listing every available OS Image that belongs to the subscription
	 * @throws MicrosoftAzureException
	 *             - indicates an exception was caught during the API call
	 * @throws TimeoutException .
	 */
	public String listOsImages() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/images");
		checkForError(response);
		return response.getEntity(String.class);

	}

	/**
	 * This method deletes the storage account with the specified name. or does nothing if the storage account does not
	 * exist.
	 *
	 * @param storageAccountName
	 *            .
	 * @param endTime
	 *            .
	 *
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteStorageAccount(final String storageAccountName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		if (!storageExists(storageAccountName)) {
			return true;
		}

		logger.info("Deleting storage account : " + storageAccountName);
		ClientResponse response = doDelete("/services/storageservices/" + storageAccountName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Deleted storage account : " + storageAccountName);
		return true;

	}

	/**
	 * This method deletes the affinity group with the specified name. or does nothing if the affinity group does not
	 * exist.
	 *
	 * @param affinityGroupName
	 *            .
	 * @param endTime
	 *            .
	 * @return true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteAffinityGroup(final String affinityGroupName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {
		if (!affinityExists(affinityGroupName)) {
			return true;
		}
		logger.info("Deleting affinity group : " + affinityGroupName);
		ClientResponse response = doDelete("/affinitygroups/" + affinityGroupName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Deleted affinity group : " + affinityGroupName);
		return true;
	}

	/**
	 * This method deletes the cloud service with the specified name. or does nothing if the cloud service does not
	 * exist.
	 *
	 * @param cloudServiceName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public void deleteCloudService(final String cloudServiceName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {
		if (!cloudServiceExists(cloudServiceName)) {
			logger.info("Cloud service " + cloudServiceName + " does not exist.");
			return;
		}

		logger.fine("Deleting cloud service : " + cloudServiceName);

		if (!doesCloudServiceContainsDeployments(cloudServiceName, endTime)) {
			// Delete cloud service
			ClientResponse response = doDelete("/services/hostedservices/" + cloudServiceName);
			String requestId = extractRequestId(response);
			waitForRequestToFinish(requestId, endTime);

		} else {
			logger.warning(String.format("Can't delete cloud service '%s', it still contains deployment(s)",
					cloudServiceName));
		}
	}

	private boolean doesCloudServiceContainsDeployments(final String cloudServiceName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {
		String[] slots = { "Production", "Staging" };
		for (String slot : slots) {
			Deployment deployment = listDeploymentsBySlot(cloudServiceName, slot, endTime);
			if (deployment != null) {
				logger.fine(String.format("Existing deployment in cloud service '%s' in slot '%s'",
						cloudServiceName, slot));
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @param machineIp
	 *            - the machine ip.
	 * @param isPrivateIp
	 *            - whether or not this ip is private or public.
	 * @param endTime
	 *            .
	 * @throws TimeoutException .
	 * @throws MicrosoftAzureException .
	 * @throws InterruptedException .
	 */
	public void deleteVirtualMachineByIp(final String machineIp, final boolean isPrivateIp, final long endTime)
			throws TimeoutException, MicrosoftAzureException, InterruptedException {

		Deployment deployment = getDeploymentByIp(machineIp, isPrivateIp);
		if (deployment == null) {
			throw new MicrosoftAzureException("Could not find a deployment for Virtual Machine with IP " + machineIp);
		}

		Role role = this.getRoleByIpAddress(machineIp, deployment);
		if (role == null) {
			throw new MicrosoftAzureException("Could not role for Virtual Machine with IP " + machineIp);
		}

		String roleName = role.getRoleName();

		// delete role if the current deployment has at least 2 roles.
		if (deployment.hasAtLeastTwoRoles()) {
			this.deleteRoleFromDeployment(roleName, deployment, endTime);

			// otherwise delete deployment
		} else {
			this.deleteDeployment(deployment.getHostedServiceName(), deployment.getName(), endTime);
		}

		// delete cloud service, verification for possible deployments is done inside this method
		this.deleteCloudService(deployment.getHostedServiceName(), endTime);

		logger.fine(String.format("Cleaning resources for role '%s' [%s]", roleName, machineIp));

		String osVhdName = role.getOsVirtualHardDisk().getName();
		try {
			this.deleteDisk(osVhdName, endTime);
		} catch (Exception e) {
			logger.warning(String.format("Failed deleting OS disk '%s' for the role '%s'", osVhdName,
					roleName));
		}

		for (DataVirtualHardDisk disk : role.getDataVirtualHardDisks().getDataVirtualHardDisks()) {
			try {
				this.deleteDisk(disk.getDiskName(), endTime);
			} catch (Exception e) {
				logger.warning(String.format("Failed delete disk '%s' for the role '%s'", disk.getDiskName(),
						roleName));
			}

		}

		logger.fine(String.format("Role '%s' resources cleaned with success", roleName));

		// logger.fine("Deployment name for Virtual Machine with IP " + machineIp + " is " + deployment.getName());
		// deleteVirtualMachineByDeploymentName(deployment.getHostedServiceName(),
		// deployment.getName(), endTime);

	}

	/**
	 * This method deletes the virtual machine under the deployment specifed by deploymentName. it also deletes the
	 * associated disk and cloud service.
	 *
	 * @param cloudServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @param endTime
	 *            .
	 * @throws TimeoutException .
	 * @throws MicrosoftAzureException .
	 * @throws InterruptedException .
	 */
	public void deleteVirtualMachineByDeploymentName(
			final String cloudServiceName, final String deploymentName, final long endTime)
			throws TimeoutException, MicrosoftAzureException, InterruptedException {

		String roleName = null;
		// Disk disk = getDiskByAttachedCloudService(cloudServiceName);
		List<Disk> disks = getDisksByAttachedCloudService(cloudServiceName);
		if (disks != null && !disks.isEmpty()) {
			roleName = disks.get(0).getAttachedTo().getRoleName();
		} else {
			throw new IllegalStateException("Disk cannot be null for an existing deployment " + deploymentName
					+ " in cloud service " + cloudServiceName);
		}

		logger.info("Deleting Virtual Machine " + roleName);
		deleteDeployment(cloudServiceName, deploymentName, endTime);

		logger.fine("Deleting cloud service : " + cloudServiceName + " that was dedicated for virtual machine "
				+ roleName);
		deleteCloudService(cloudServiceName, endTime);

		for (Disk disk : disks) {
			String diskName = disk.getName();
			logger.fine("Waiting for OS or Data Disk " + diskName + " to detach from role " + roleName);
			waitForDiskToDetach(diskName, roleName, endTime);
			logger.info("Deleting OS or Data Disk : " + diskName);
			deleteDisk(diskName, endTime);
		}
	}

	private Disk getDiskByAttachedCloudService(final String cloudServiceName)
			throws MicrosoftAzureException, TimeoutException {

		Disks disks = listDisks();
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if ((attachedTo != null) && (attachedTo.getHostedServiceName().equals(cloudServiceName))) {
				return disk;
			}
		}
		return null;
	}

	private List<Disk> getDisksByAttachedCloudService(final String cloudServiceName)
			throws MicrosoftAzureException, TimeoutException {
		List<Disk> cloudServiceDisks = new ArrayList<Disk>();
		Disks disks = listDisks();
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if ((attachedTo != null) && (attachedTo.getHostedServiceName().equals(cloudServiceName))) {
				cloudServiceDisks.add(disk);
			}
		}
		return cloudServiceDisks;
	}

	private void waitForDiskToDetach(final String diskName, final String roleName, long endTime)
			throws TimeoutException, MicrosoftAzureException, InterruptedException {

		while (true) {
			Disks disks = listDisks();
			Disk osDisk = null;
			for (Disk disk : disks) {
				if (disk.getName().equals(diskName)) {
					osDisk = disk;
					break;
				}
			}
			if (osDisk != null) {
				if (osDisk.getAttachedTo() == null) {
					return;
				} else {
					logger.fine("Disk " + diskName + " is still attached to role "
							+ osDisk.getAttachedTo().getRoleName());
					Thread.sleep(DEFAULT_POLLING_INTERVAL);
				}
			} else {
				throw new MicrosoftAzureException("Disk " + diskName + " does not exist");
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for disk " + diskName + " to detach from role " + roleName);
			}
		}

	}

	/**
	 * this method return all disks that are currently being used by this subscription. NOTE : disks that are not
	 * attached to any deployment are also returned. this means that {@code Disk.getAttachedTo} might return null.
	 *
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Disks listDisks() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/disks");
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		return (Disks) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 * This method deletes a disk with the specified name. or does nothing if the disk does not exist.
	 *
	 * @param diskName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteDisk(final String diskName, final long endTime)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {
		if (!isDiskExists(diskName)) {
			logger.info("Disk " + diskName + " does not exist");
			return true;
		}

		ClientResponse response = doDelete("/services/disks/" + diskName);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		return true;
	}

	/**
	 * This method deletes just the virtual machine from the specified cloud service. associated OS Disk and cloud
	 * service are not removed.
	 *
	 * @param hostedServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteDeployment(final String hostedServiceName,
			final String deploymentName, final long endTime)
			throws MicrosoftAzureException, TimeoutException, InterruptedException {

		if (!deploymentExists(hostedServiceName, deploymentName)) {
			logger.info("Deployment " + deploymentName + " does not exist");
			return true;
		}

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;

		logger.fine(getThreadIdentity() + "Waiting for pending request lock...");
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout, TimeUnit.MILLISECONDS);

		if (lockAcquired) {

			logger.fine(getThreadIdentity() + "Lock acquired : " + pendingRequest.hashCode());
			logger.fine(getThreadIdentity() + "Executing a request to delete virtual machine");

			try {

				logger.fine(getThreadIdentity() + "Deleting deployment of virtual machine from : "
						+ deploymentName);

				ClientResponse response = doDelete("/services/hostedservices/"
						+ hostedServiceName + "/deployments/" + deploymentName);
				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				pendingRequest.unlock();
				logger.fine(getThreadIdentity() + "Lock unlcoked");
			} catch (final Exception e) {
				logger.fine(getThreadIdentity() + "About to release lock " + pendingRequest.hashCode());
				pendingRequest.unlock();
				if (e instanceof MicrosoftAzureException) {
					throw (MicrosoftAzureException) e;
				}
				if (e instanceof TimeoutException) {
					throw (TimeoutException) e;
				}
				if (e instanceof InterruptedException) {
					throw (InterruptedException) e;
				}
			}
			return true;
		} else {
			throw new TimeoutException(
					"Failed to acquire lock for deleteDeployment request after + "
							+ lockTimeout + " milliseconds");
		}

	}

	// TODO : refactoring with other methods that use the same process lock>->request->response->unlock
	public boolean deleteRoleFromDeployment(final String roleName,
			final Deployment deployment, final long endTime) throws InterruptedException, MicrosoftAzureException,
			TimeoutException {

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;

		logger.fine(getThreadIdentity() + "Waiting for pending request lock...");
		boolean lockAcquired = pendingRequest.tryLock(lockTimeout, TimeUnit.MILLISECONDS);

		if (lockAcquired) {
			try {

				// https://management.core.windows.net/<subscription-id>/services/hostedservices/<cloudservice-name>/deployments/<deployment-name>/roles/<role-name>

				ClientResponse response = doDelete("/services/hostedservices/" + deployment.getHostedServiceName()
						+ "/deployments/" + deployment.getName() + "/roles/" + roleName);

				String requestId = extractRequestId(response);
				waitForRequestToFinish(requestId, endTime);
				pendingRequest.unlock();
				logger.fine(getThreadIdentity() + "Lock unlcoked");

			} catch (final Exception e) {
				logger.fine(getThreadIdentity() + "About to release lock " + pendingRequest.hashCode());
				pendingRequest.unlock();

				if (e instanceof MicrosoftAzureException) {
					throw (MicrosoftAzureException) e;
				}
				if (e instanceof TimeoutException) {
					throw (TimeoutException) e;
				}
				if (e instanceof InterruptedException) {
					throw (InterruptedException) e;
				}
			}
		} else {
			throw new TimeoutException("Failed to acquire lock for deleteRoleFromDeployment request after + "
					+ lockTimeout + " milliseconds");
		}

		return true;
	}

	/**
	 *
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public HostedServices listHostedServices() throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doGet("/services/hostedservices");
		String responseBody = response.getEntity(String.class);
		checkForError(response);
		return (HostedServices) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
	}

	/**
	 *
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public AffinityGroups listAffinityGroups() throws MicrosoftAzureException,
			TimeoutException {

		ClientResponse response = doGet("/affinitygroups");
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		checkForError(response);
		return (AffinityGroups) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 *
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public StorageServices listStorageServices() throws MicrosoftAzureException, TimeoutException {
		ClientResponse response = doGet("/services/storageservices");
		String responseBody = response.getEntity(String.class);
		return (StorageServices) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 *
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	/*
	 * public VirtualNetworkSites listVirtualNetworkSites() throws MicrosoftAzureException, TimeoutException {
	 * ClientResponse response = doGet("/services/networking/media"); if (response.getStatus() == HTTP_NOT_FOUND) {
	 * return null; } String responseBody = response.getEntity(String.class); if (responseBody.charAt(0) == BAD_CHAR) {
	 * responseBody = responseBody.substring(1); }
	 * 
	 * GlobalNetworkConfiguration globalNetowrkConfiguration = (GlobalNetworkConfiguration) MicrosoftAzureModelUtils
	 * .unmarshall(responseBody); return globalNetowrkConfiguration.getVirtualNetworkConfiguration()
	 * .getVirtualNetworkSites(); }
	 */

	public VirtualNetworkConfiguration getVirtualNetworkConfiguration() throws MicrosoftAzureException,
			TimeoutException {
		ClientResponse response = doGet("/services/networking/media");
		if (response.getStatus() == HTTP_NOT_FOUND) {
			return null;
		}
		String responseBody = response.getEntity(String.class);
		if (responseBody.charAt(0) == BAD_CHAR) {
			responseBody = responseBody.substring(1);
		}

		GlobalNetworkConfiguration globalNetowrkConfiguration = (GlobalNetworkConfiguration) MicrosoftAzureModelUtils
				.unmarshall(responseBody);
		return globalNetowrkConfiguration.getVirtualNetworkConfiguration();

	}

	/**
	 * @param hostedServiceName
	 *            - hosted service name.
	 * @param embedDeployments
	 *            - whether or not to include the deployments of this hosted service in the response.
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public HostedService getHostedService(final String hostedServiceName,
			final boolean embedDeployments) throws MicrosoftAzureException,
			TimeoutException {
		StringBuilder builder = new StringBuilder();
		builder.append("/services/hostedservices/").append(hostedServiceName);
		if (embedDeployments) {
			builder.append("?embed-detail=true");
		}
		ClientResponse response = doGet(builder.toString());
		checkForError(response);
		String responseBody = response.getEntity(String.class);
		return (HostedService) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 *
	 * @param hostedServiceName
	 *            .
	 * @param deploymentSlot
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByDeploymentSlot(
			final String hostedServiceName, final String deploymentSlot)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		try {
			response = doGet("/services/hostedservices/" + hostedServiceName
					+ "/deploymentslots/" + deploymentSlot);
			checkForError(response);
		} catch (TimeoutException e) {
			logger.warning("Timed out while waiting for deployment details. This may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		return (Deployment) MicrosoftAzureModelUtils.unmarshall(responseBody);
	}

	/**
	 *
	 * @param hostedServiceName
	 *            .
	 * @param deploymentName
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByDeploymentName(
			final String hostedServiceName, final String deploymentName)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		try {
			response = doGet("/services/hostedservices/" + hostedServiceName + "/deployments/" + deploymentName);
			checkForError(response);
		} catch (TimeoutException e) {
			logger.warning("Timed out while waiting for deployment details. this may cause a leaking node");
			throw e;
		}

		String responseBody = response.getEntity(String.class);
		Deployment deployment = (Deployment) MicrosoftAzureModelUtils.unmarshall(responseBody);
		deployment.setHostedServiceName(hostedServiceName);
		return deployment;
	}

	/**
	 *
	 * @param machineIp
	 *            .
	 * @param isPrivateIp
	 *            .
	 * @return .
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 */
	public Deployment getDeploymentByIp(final String machineIp, final boolean isPrivateIp)
			throws MicrosoftAzureException, TimeoutException {

		HostedServices cloudServices = listHostedServices();
		for (HostedService hostedService : cloudServices) {
			String cloudServiceName = hostedService.getServiceName();
			Deployments deployments = getHostedService(cloudServiceName, true).getDeployments();

			// skip empty cloud services
			if (deployments != null && !deployments.getDeployments().isEmpty()) {
				for (Deployment deployment : deployments) {

					// skip other networks
					if (this.virtualNetwork.equals(deployment.getVirtualNetworkName())) {

						// TODO checking with privateIp, checks also with publicIp
						for (RoleInstance ri : deployment.getRoleInstanceList()) {
							if (machineIp.equals(ri.getIpAddress())) {
								deployment.setHostedServiceName(cloudServiceName);
								return deployment;
							}

						}
						// TODO check with public ip when implemented
						// String publicIp = getPublicIpFromDeployment(deployment);
						// String privateIp = getPrivateIpFromDeployment(deployment);
						// String ip = isPrivateIp ? privateIp : publicIp;
						// if (machineIp.equals(ip)) {
						// deployment.setHostedServiceName(cloudServiceName);
						// return deployment;
						// }
					}

				}
			}
		}
		logger.info("Could not find roles with ip :" + machineIp);
		return null;

	}

	/**
	 * This method deletes the virtual network specified. or does nothing if the virtual network does not exist.
	 *
	 * @param virtualNetworkSite
	 *            - virtual network site name to delete .
	 * @param endTime
	 *            .
	 * @return - true if the operation was successful, throws otherwise.
	 * @throws MicrosoftAzureException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public boolean deleteVirtualNetworkSite(final String virtualNetworkSite,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		if (!virtualNetworkExists(virtualNetworkSite)) {
			return true;
		}
		VirtualNetworkConfiguration virtualNetworkConfiguration = getVirtualNetworkConfiguration();
		VirtualNetworkSites virtualNetworkSites = virtualNetworkConfiguration.getVirtualNetworkSites();
		int index = 0;
		for (int i = 0; i < virtualNetworkSites.getVirtualNetworkSites().size(); i++) {
			VirtualNetworkSite site = virtualNetworkSites.getVirtualNetworkSites().get(i);
			if (site.getName().equals(virtualNetworkSite)) {
				index = i;
				break;
			}
		}
		virtualNetworkSites.getVirtualNetworkSites().remove(index);
		logger.info("Deleting virtual network site : " + virtualNetworkSite);
		setNetworkConfiguration(endTime, virtualNetworkConfiguration);
		logger.fine("Deleted virtual network site : " + virtualNetworkSite);
		return true;

	}

	private String extractRequestId(final ClientResponse response) {
		return response.getHeaders().getFirst("x-ms-request-id");
	}

	private ClientResponse doPut(final String url, final String body,
			final String contentType) throws MicrosoftAzureException {
		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, contentType)
				.put(ClientResponse.class, body);
		checkForError(response);
		return response;
	}

	private ClientResponse doPost(final String url, final String body)
			throws MicrosoftAzureException {
		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.post(ClientResponse.class, body);
		checkForError(response);
		return response;
	}

	private ClientResponse doGet(final String url)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = null;
		for (int i = 0; i < MAX_RETRIES; i++) {
			try {
				response = resource
						.path(subscriptionId + url)
						.header(X_MS_VERSION_HEADER_NAME,
								X_MS_VERSION_HEADER_VALUE)
						.header(CONTENT_TYPE_HEADER_NAME,
								CONTENT_TYPE_HEADER_VALUE)
						.get(ClientResponse.class);
				break;
			} catch (ClientHandlerException e) {
				logger.warning("Caught an exception while executing GET with url "
						+ url + ". Message :" + e.getMessage());
				logger.warning("Retrying request");
				continue;
			}
		}
		if (response == null) {
			throw new TimeoutException("Timed out while executing GET after "
					+ MAX_RETRIES);
		}

		return response;
	}

	private ClientResponse doDelete(final String url)
			throws MicrosoftAzureException {
		ClientResponse response = resource.path(subscriptionId + url)
				.header(X_MS_VERSION_HEADER_NAME, X_MS_VERSION_HEADER_VALUE)
				.header(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE)
				.delete(ClientResponse.class);
		checkForError(response);
		return response;
	}

	private boolean cloudServiceExists(final String cloudServiceName) throws MicrosoftAzureException, TimeoutException {
		HostedServices cloudServices = listHostedServices();
		return (cloudServices.contains(cloudServiceName));
	}

	private boolean affinityExists(final String affinityGroupName)
			throws MicrosoftAzureException, TimeoutException {
		AffinityGroups affinityGroups = listAffinityGroups();
		return (affinityGroups.contains(affinityGroupName));
	}

	private boolean deploymentExists(final String cloudServiceName, final String deploymentName)
			throws MicrosoftAzureException, TimeoutException {

		HostedService service = getHostedService(cloudServiceName, true);
		if ((service.getDeployments() != null) && (service.getDeployments().contains(deploymentName))) {
			return true;
		} else {
			return false;
		}

	}

	private boolean storageExists(final String storageAccouhtName)
			throws MicrosoftAzureException, TimeoutException {
		StorageServices storageServices = listStorageServices();
		return (storageServices.contains(storageAccouhtName));
	}

	private boolean isDiskExists(final String osDiskName)
			throws MicrosoftAzureException, TimeoutException {
		Disks disks = listDisks();
		return (disks.contains(osDiskName));
	}

	private boolean virtualNetworkExists(final String virtualNetworkName)
			throws MicrosoftAzureException, TimeoutException {
		VirtualNetworkSites sites = getVirtualNetworkConfiguration().getVirtualNetworkSites();
		return (sites.contains(virtualNetworkName));
	}

	private void checkForError(final ClientResponse response)
			throws MicrosoftAzureException {
		int status = response.getStatus();
		if (status != HTTP_OK && status != HTTP_CREATED
				&& status != HTTP_ACCEPTED) { // we got some
			// sort of error
			Error error = (Error) MicrosoftAzureModelUtils.unmarshall(response
					.getEntity(String.class));
			String errorMessage = error.getMessage();
			String errorCode = error.getCode();
			throw new MicrosoftAzureException(errorCode, errorMessage);
		}
	}

	private Deployment waitForDeploymentStatus(final String state,
			final String hostedServiceName, final String deploymentSlot,
			final long endTime) throws TimeoutException,
			MicrosoftAzureException, InterruptedException {

		while (true) {
			Deployment deployment = getDeploymentByDeploymentSlot(
					hostedServiceName, deploymentSlot);
			String status = deployment.getStatus();
			if (status.equals(state)) {
				return deployment;
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Deployment waitForRoleInstanceStatus(final String state,
			final String hostedServiceName, final String deploymentSlot,
			final long endTime) throws TimeoutException,
			MicrosoftAzureException, InterruptedException {

		while (true) {
			Deployment deployment = getDeploymentByDeploymentSlot(
					hostedServiceName, deploymentSlot);
			String roleName = deployment.getRoleList().getRoles().get(0)
					.getRoleName();
			String status = deployment.getRoleInstanceList().getRoleInstances()
					.get(0).getInstanceStatus();
			boolean error = checkVirtualMachineStatusForError(status);
			if (error) {
				// bad status of VM.
				throw new MicrosoftAzureException("Virtual Machine " + roleName
						+ " was provisioned but found in status " + status);
			}
			if (status.equals(state)) {
				return deployment;
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}
			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}

		}

	}

	private void setNetworkConfiguration(final long endTime,
			final VirtualNetworkConfiguration virtualNetworkConfiguration)
			throws MicrosoftAzureException, TimeoutException,
			InterruptedException {
		GlobalNetworkConfiguration networkConfiguration =
				requestBodyBuilder.buildGlobalNetworkConfiguration(virtualNetworkConfiguration);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(networkConfiguration, true);

		ClientResponse response = doPut("/services/networking/media", xmlRequest, "text/plain");
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
	}

	private void waitForRequestToFinish(final String requestId,
			final long endTime) throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		while (true) {

			// Query Azure for operation details
			Operation operation = getOperation(requestId);
			String status = operation.getStatus();
			if (!status.equals(IN_PROGRESS)) {

				// if operation succeeded, we are good to go
				if (status.equals(SUCCEEDED)) {
					return;
				}
				if (status.equals(FAILED)) {
					String errorMessage = operation.getError().getMessage();
					String errorCode = operation.getError().getCode();
					throw new MicrosoftAzureException(errorCode, errorMessage);
				}
			} else {
				Thread.sleep(DEFAULT_POLLING_INTERVAL);
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException(
						"Timed out waiting for operation to finish. last state was : "
								+ status);
			}
		}

	}

	private Operation getOperation(final String requestId)
			throws MicrosoftAzureException, TimeoutException {

		ClientResponse response = doGet("/operations/" + requestId);
		return (Operation) MicrosoftAzureModelUtils
				.unmarshall(response.getEntity(String.class));
	}

	private String getPublicIpFromDeployment(final Deployment deployment) {
		ConfigurationSets configurationSets = deployment.getRoleList()
				.getRoles().get(0).getConfigurationSets();
		String publicIp = null;
		for (ConfigurationSet configurationSet : configurationSets) {
			if (configurationSet instanceof NetworkConfigurationSet) {
				NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) configurationSet;
				// FIXME No endpoints = no public IP !!!
				InputEndpoints inputEndpoints = networkConfigurationSet.getInputEndpoints();
				if (inputEndpoints != null) {
					publicIp = inputEndpoints.getInputEndpoints().get(0).getvIp();
				}
			}
		}
		return publicIp;
	}

	private String getThreadIdentity() {
		String threadName = Thread.currentThread().getName();
		long threadId = Thread.currentThread().getId();
		return "[" + threadName + "]" + "[" + threadId + "] - ";
	}

	public void rebootVirtualMachine(long endTime) throws MicrosoftAzureException, TimeoutException,
			InterruptedException {

		// https://management.core.windows.net/<subscription-id>/services/hostedservices/<cloudservice-name>/deploymentslots/<deployment-slot>/roleinstances/<role-instance-name>
		// https://management.core.windows.net/828d8ef4-292e-45b0-b029-50b9442e105a/services/hostedservices/fastubuntu1404/deployments/ubuntu/roleinstances/ubuntu-test/Operations

		String cloudServiceName = "fastubuntu1404";
		String deploymentSlot = "Production";
		String deploymentName = "ubuntu";
		String roleInstanceName = "ubuntu-test";
		String url = String.format("/services/hostedservices/%s/deployments/%s/roleinstances/%s/Operations",
				cloudServiceName, deploymentName, roleInstanceName);

		RestartRoleOperation restartRoleOperation = requestBodyBuilder.buildRestartRoleOperation();
		String xmlRequest = MicrosoftAzureModelUtils.marshall(restartRoleOperation, false);
		ClientResponse response = doPost(url, xmlRequest);
		String requestId = extractRequestId(response);
		this.waitForRequestToFinish(requestId, endTime);

		try {
			this.waitForDeploymentStatus("Running", cloudServiceName, deploymentSlot, endTime);
			this.waitForRoleInstanceStatus("ReadyRole", cloudServiceName, deploymentSlot, endTime);
		} catch (final Exception e) {
			logger.fine("Error while waiting for VM status : " + e.getMessage());
			// the VM was created but with a bad status
			deleteVirtualMachineByDeploymentName(cloudServiceName, roleInstanceName, endTime);
			if (e instanceof MicrosoftAzureException) {
				throw (MicrosoftAzureException) e;
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			}
			if (e instanceof InterruptedException) {
				throw (InterruptedException) e;
			}
			throw new MicrosoftAzureException(e);
		}
	}

	public Deployment listDeploymentsBySlot(String cloudService, String deploymentSlot, long endTime)
			throws MicrosoftAzureException,
			TimeoutException, InterruptedException {

		Deployment deployment = null;

		ClientResponse response = doGet("/services/hostedservices/" + cloudService + "/deploymentslots/"
				+ deploymentSlot);

		if (response.getStatus() != HTTP_NOT_FOUND) {

			String requestId = extractRequestId(response);
			this.waitForRequestToFinish(requestId, endTime);
			String responseBody = response.getEntity(String.class);
			deployment = (Deployment) MicrosoftAzureModelUtils.unmarshall(responseBody);

			// no deployment found (404)
		} else {
			logger.warning(String.format("The cloud service '%s' doesn't have any deployment in slot '%s'.",
					cloudService, deploymentSlot));
		}

		return deployment;
	}

	public void addDataDiskToVM(String serviceName, String deploymentName, String roleName, String storageAccountName,
			int diskSize, long endTime) throws MicrosoftAzureException, TimeoutException, InterruptedException {
		// https://management.core.windows.net/<subscription-id>/services/hostedservices/<service-name>/deployments/<deployment-name>/roles/<role-name>/DataDisks

		StringBuilder dataMediaLinkBuilder = new StringBuilder();
		dataMediaLinkBuilder.append("https://");
		dataMediaLinkBuilder.append(storageAccountName);
		dataMediaLinkBuilder.append(".blob.core.windows.net/vhds/");
		dataMediaLinkBuilder.append(serviceName);
		dataMediaLinkBuilder.append("-");
		dataMediaLinkBuilder.append(roleName);
		dataMediaLinkBuilder.append("-data-");
		dataMediaLinkBuilder.append(UUIDHelper.generateRandomUUID(4));
		dataMediaLinkBuilder.append(".vhd");
		DataVirtualHardDisk dataVirtualHardDisk = new DataVirtualHardDisk();
		dataVirtualHardDisk.setLogicalDiskSizeInGB(10);
		dataVirtualHardDisk.setMediaLink(dataMediaLinkBuilder.toString());
		dataVirtualHardDisk.setDiskLabel("Data");
		dataVirtualHardDisk.setLun(0);

		String xmlRequest = MicrosoftAzureModelUtils.marshall(dataVirtualHardDisk, false);
		String url = String.format("/services/hostedservices/%s/deployments/%s/roles/%s/DataDisks",
				serviceName, deploymentName, roleName);
		ClientResponse response = doPost(url, xmlRequest);
		String requestId = extractRequestId(response);
		waitForRequestToFinish(requestId, endTime);
		logger.fine("Added a data disk to " + roleName);
	}

	public String getVirtualNetwork() {
		return virtualNetwork;
	}

	public void setVirtualNetwork(String virtualNetwork) {
		this.virtualNetwork = virtualNetwork;
	}

	private Role getRoleByIpAddress(String ipAddress, Deployment deployment)
			throws MicrosoftAzureException, TimeoutException {

		Role role = null;
		try {

			RoleInstance roleInstance = deployment.getRoleInstanceList().getRoleInstanceByIpAddress(ipAddress);
			if (roleInstance != null) {
				role = deployment.getRoleList().getRoleByName(roleInstance.getRoleName());
			}

		} catch (Exception e) {
			logger.warning(String.format("Can't find role for machine with ip address '%s'", ipAddress));
		}
		return role;
	}
}
