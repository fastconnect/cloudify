/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.utilitydomain.context;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.blockstorage.StorageFacade;
import org.cloudifysource.domain.context.kvstorage.AttributesFacade;
import org.cloudifysource.domain.context.network.NetworkFacade;
import org.cloudifysource.domain.context.storage.AzureStorageFacade;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.context.AzureRemoteStorageProvisioningDriver;
import org.cloudifysource.dsl.internal.context.RemoteNetworkProvisioningDriver;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.utilitydomain.admin.TimedAdmin;
import org.cloudifysource.utilitydomain.context.blockstorage.StorageFacadeImpl;
import org.cloudifysource.utilitydomain.context.kvstore.AttributesFacadeImpl;
import org.cloudifysource.utilitydomain.context.network.NetworkFacadeImpl;
import org.cloudifysource.utilitydomain.context.storage.AzureStorageFacadeImpl;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.internal.esm.InternalElasticServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.cluster.ClusterInfo;

/**
 * 
 * 
 * @author barakme
 * @since 1.0
 */
public class ServiceContextImpl implements ServiceContext {

	private static final String LOCALCLOUD = "localcloud";
	private org.cloudifysource.domain.Service service;
	private TimedAdmin timedAdmin;
	private Admin openAdmin;
	private final String serviceDirectory;
	private ClusterInfo clusterInfo;
	private boolean initialized = false;

	private String serviceName;

	private String applicationName;

	private AzureStorageFacade azureStorageFacade;
	private StorageFacade storageFacade;
	private NetworkFacade networkDriver;
	private AttributesFacade attributesFacade;

	// TODO - this property should not be settable - there should be a separate
	// interface for that.
	// this pid may be modified due to process crashed, so volatile is required.
	private volatile long externalProcessId;

	/*************
	 * Constructor.
	 * 
	 * @param clusterInfo
	 *            the cluster info.
	 * @param serviceDirectory
	 *            the service directory.
	 * 
	 */
	public ServiceContextImpl(final ClusterInfo clusterInfo,
			final String serviceDirectory) {
		if (clusterInfo == null) {
			throw new NullPointerException(
					"Cluster Info provided to service context cannot be null!");
		}
		this.clusterInfo = clusterInfo;
		this.serviceDirectory = serviceDirectory;
		if (clusterInfo.getName() != null) {
			FullServiceName fullName = ServiceUtils
					.getFullServiceName(clusterInfo.getName());
			this.applicationName = fullName.getApplicationName();
			this.serviceName = fullName.getServiceName();
		}

	}

	/**********
	 * Late object initialization.
	 * 
	 * @param service
	 *            .
	 * @param admin
	 *            .
	 * @param clusterInfo
	 *            .
	 */
	public void init(final Service service, final TimedAdmin timedAdmin, final ClusterInfo clusterInfo) {
		this.service = service;
		this.timedAdmin = timedAdmin;

		// TODO - is the null path even possible?
		if (clusterInfo == null) {
			this.applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
			this.serviceName = service.getName();
		} else {
			logger.fine("Parsing full service name from PU name: "
					+ clusterInfo.getName());
			final FullServiceName fullServiceName = ServiceUtils
					.getFullServiceName(clusterInfo.getName());
			logger.fine("Got full service name: " + fullServiceName);
			this.serviceName = fullServiceName.getServiceName();
			this.applicationName = fullServiceName.getApplicationName();

		}
		if (timedAdmin != null) {
			final boolean found = this.timedAdmin.waitForLookupServices(1, 30, TimeUnit.SECONDS);
			if (!found) {
				throw new AdminException(
						"A service context could not be created as the Admin API could not find a lookup service "
								+ "in the network, using groups: " + Arrays.toString(timedAdmin.getAdminGroups())
								+ " and locators: " + Arrays.toString(timedAdmin.getAdminLocators()));
			}
		}
		this.attributesFacade = new AttributesFacadeImpl(this, timedAdmin);
		initialized = true;
	}

	private Object getRemoteApi(final String apiName) {
		ElasticServiceManager elasticServiceManager;
		if (timedAdmin != null) {
			elasticServiceManager = timedAdmin.waitForElasticServiceManager();
			String puName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
			Object remoteApi = null;
			remoteApi = ((InternalElasticServiceManager) elasticServiceManager).getRemoteApi(puName, apiName);

			if (logger.isLoggable(Level.FINE)) {
				if (remoteApi == null) {
					logger.fine(apiName + " was not found for pu name: " + puName);
				} else {
					logger.fine(apiName + " successfully located for pu name: " + puName);
				}
			}

			logger.finest("Requested remote api name '" + apiName + "' got: " + remoteApi);

			if (CloudifyConstants.STORAGE_REMOTE_API_KEY.equals(apiName)) {
				return new StorageFacadeImpl(this, (RemoteStorageProvisioningDriver) remoteApi);
			} else if (CloudifyConstants.NETWORK_REMOTE_API_KEY.equals(apiName)) {
				return new NetworkFacadeImpl((RemoteNetworkProvisioningDriver) remoteApi);
			} else if (CloudifyConstants.AZURE_REMOTE_API_KEY.equals(apiName)) {
				return new AzureStorageFacadeImpl(this, (AzureRemoteStorageProvisioningDriver) remoteApi);
			}
		}
		return null;
	}

	/************
	 * Late initializer, used in the integrated container (i.e. test-recipe)
	 * 
	 * @param service
	 *            .
	 */
	public void initInIntegratedContainer(final Service service) {
		this.service = service;

		this.clusterInfo = new ClusterInfo(null, 1, 0, 1, 0);
		if (service != null) {
			this.clusterInfo.setName(service.getName());
			this.serviceName = service.getName();
		}

		this.applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;

		this.attributesFacade = new AttributesFacadeImpl(this, timedAdmin);
		initialized = true;

	}

	private void checkInitialized() {
		if (!this.initialized) {
			throw new IllegalStateException(
					"The Service Context has not been initialized yet. "
							+ "It can only be used after the Service file has been fully evaluated");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#getInstanceId()
	 */
	@Override
	public int getInstanceId() {
		// checkInitialized();

		return clusterInfo.getInstanceId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#waitForService(java.lang .String, int,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public org.cloudifysource.domain.context.Service waitForService(
			final String name, final int timeout, final TimeUnit unit) {
		checkInitialized();

		if (this.timedAdmin != null) {
			final String puName = ServiceUtils.getAbsolutePUName(
					this.applicationName, name);
			final ProcessingUnit pu = waitForProcessingUnitFromAdmin(puName,
					timeout, unit);
			if (pu == null) {
				return null;
			} else {
				return new ServiceImpl(
						pu);
			}
		}

		// running in integrated container
		if (name.equals(this.service.getName())) {
			return new ServiceImpl(
					name, service.getNumInstances());
		}

		throw new IllegalArgumentException(
				"When running in the integrated container, Service Context only includes the running service");

	}

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceContextImpl.class.getName());

	private ProcessingUnit waitForProcessingUnitFromAdmin(final String name,
			final long timeout, final TimeUnit unit) {

		final ProcessingUnit pu = timedAdmin.waitForPU(name, timeout, unit);
		if (pu == null) {
			logger.warning("Processing unit with name: "
					+ name
					+ " was not found in the cluster. Are you running in an IntegratedProcessingUnitContainer? "
					+ "If not, consider extending the timeout.");
		}

		return pu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#getServiceDirectory()
	 */
	@Override
	public String getServiceDirectory() {

		return serviceDirectory;
	}

	/**
	 * Returns the Admin Object the underlies the Service Context. Note: this is intended as a debugging aid, and should
	 * not be used by most application. Only power users, familiar with the details of the Admin API, should use it.
	 * 
	 * @return the admin.
	 */
	public Admin getAdmin() {
		logger.warning("Using an admin object directly is not recommended. This action bypasses the admin"
				+ " timing mechanism and might hinder performace");
		return getOpenAdmin();
	}

	private synchronized Admin getOpenAdmin() {
		if (openAdmin != null) {
			logger.info("using a cached un-timed Admin");
			return openAdmin;
		}

		logger.fine("creating a new un-timed Admin object");
		final AdminFactory factory = new AdminFactory();
		factory.useDaemonThreads(true);
		factory.discoverUnmanagedSpaces();

		openAdmin = factory.createAdmin();
		openAdmin.setStatisticsHistorySize(0);

		logger.info("Created a new un-timed Admin object with groups: " + Arrays.toString(timedAdmin.getAdminGroups())
				+ " and Locators: " + Arrays.toString(timedAdmin.getAdminLocators()));

		return openAdmin;
	}

	/**
	 * 
	 * @param service
	 */
	void setService(final Service service) {
		this.service = service;
	}

	public ClusterInfo getClusterInfo() {
		return clusterInfo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#getServiceName()
	 */
	@Override
	public String getServiceName() {
		return serviceName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#getApplicationName()
	 */
	@Override
	public String getApplicationName() {
		return applicationName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudifysource.dsl.context.IServiceContext#getAttributes()
	 */
	@Override
	public AttributesFacade getAttributes() {
		return attributesFacade;
	}

	@Override
	public String toString() {
		if (this.initialized) {
			return "ServiceContext [dir=" + serviceDirectory + ", clusterInfo="
					+ clusterInfo + "]";
		} else {
			return "ServiceContext [NOT INITIALIZED]";
		}
	}

	@Override
	public long getExternalProcessId() {
		return externalProcessId;
	}

	public void setExternalProcessId(final long externalProcessId) {
		this.externalProcessId = externalProcessId;
	}

	@Override
	public boolean isLocalCloud() {
		String isLocalCloudStr = System.getenv(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID);
		return LOCALCLOUD.equalsIgnoreCase(isLocalCloudStr);
	}

	@Override
	public String getPublicAddress() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP);
		if (envVar != null) {
			return envVar;
		}

		return ServiceUtils.getPrimaryInetAddress();

	}

	@Override
	public String getPrivateAddress() {
		final String envVar = System
				.getenv(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP);
		if (envVar != null) {
			return envVar;
		}

		return ServiceUtils.getPrimaryInetAddress();
	}

	@Override
	public String getImageID() {
		return System.getenv(CloudifyConstants.CLOUDIFY_CLOUD_IMAGE_ID);
	}

	@Override
	public String getHardwareID() {
		return System.getenv(CloudifyConstants.CLOUDIFY_CLOUD_HARDWARE_ID);
	}

	@Override
	public String getCloudTemplateName() {
		return System.getenv(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME);
	}

	@Override
	public String getMachineID() {
		return System.getenv(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID);
	}

	@Override
	public String getLocationId() {
		return System.getenv(CloudifyConstants.CLOUDIFY_CLOUD_LOCATION_ID);
	}

	@Override
	public StorageFacade getStorage() {
		if (storageFacade == null) {
			this.storageFacade = (StorageFacade) getRemoteApi(CloudifyConstants.STORAGE_REMOTE_API_KEY);
		}
		return storageFacade;
	}

	@Override
	public AzureStorageFacade getAzureStorage() {

		if (azureStorageFacade == null) {
			this.azureStorageFacade = (AzureStorageFacade) getRemoteApi(CloudifyConstants.AZURE_REMOTE_API_KEY);
			logger.finest("got azure storage: " + azureStorageFacade);
		}
		return this.azureStorageFacade;
	}

	@Override
	public boolean isPrivileged() {
		final String envVar = System.getenv(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVILEGED);
		return Boolean.valueOf(envVar);
	}

	@Override
	public String getBindAddress() {
		return System.getenv(CloudifyConstants.CLOUDIFY_CLOUD_MACHINE_IP_ADDRESS_ENV);
	}

	@Override
	public String getAttributesStoreDiscoveryTimeout() {
		return System.getenv(CloudifyConstants.USM_ATTRIBUTES_STORE_DISCOVERY_TIMEOUT_ENV_VAR);
	}

	@Override
	public void stopMaintenanceMode() {
		InternalElasticServiceManager esm = (InternalElasticServiceManager) timedAdmin.waitForElasticServiceManager();
		String absolutePUName = ServiceUtils.getAbsolutePUName(getApplicationName(), getServiceName());
		esm.enableAgentFailureDetection(absolutePUName);
	}

	@Override
	public void startMaintenanceMode(final long timeout,
			final TimeUnit unit) {
		InternalElasticServiceManager esm = (InternalElasticServiceManager) timedAdmin.waitForElasticServiceManager();
		String absolutePUName = ServiceUtils.getAbsolutePUName(getApplicationName(), getServiceName());
		esm.disableAgentFailureDetection(absolutePUName, timeout, unit);
	}

	@Override
	public NetworkFacade getNetwork() {
		if (this.networkDriver == null) {
			this.networkDriver = (NetworkFacade) getRemoteApi(CloudifyConstants.NETWORK_REMOTE_API_KEY);
		}
		return this.networkDriver;
	}
}
