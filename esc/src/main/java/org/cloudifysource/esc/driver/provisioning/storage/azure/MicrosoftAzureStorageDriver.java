package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.storage.CloudStorage;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureUtils;
import org.cloudifysource.esc.driver.provisioning.azure.StorageCallable;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.client.UUIDHelper;
import org.cloudifysource.esc.driver.provisioning.azure.model.DataVirtualHardDisk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstance;
import org.cloudifysource.esc.driver.provisioning.storage.BaseStorageDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;

/**
 * Class that manages storages on azure cloud
 * 
 * @author FastConnect
 * 
 */
public class MicrosoftAzureStorageDriver extends BaseStorageDriver implements StorageProvisioningDriver {

	private static final Logger logger = Logger.getLogger(MicrosoftAzureStorageDriver.class.getName());

	private static final String STORAGE_ACCOUNT_PROPERTY = "azure.storage.account";
	private static final String DATADISK_HOSTCACHING = "azure.storage.hostcaching";

	private static final String HOSTCACHING_NONE = "None";
	private static final String HOSTCACHING_READ_WRITE = "ReadWrite";
	private static final String HOSTCACHING_READ_ONLY = "ReadOnly";

	private Lock driverPendingRequest = new ReentrantLock(true);

	private MicrosoftAzureCloudDriver computeDriver;

	private CloudStorage cloudStorage;

	private String affinityGroup;

	@Override
	public void setConfig(Cloud cloud, String computeTemplateName) {
		cloudStorage = cloud.getCloudStorage();

		this.affinityGroup = (String) cloud.getCustom().get(MicrosoftAzureCloudDriver.AZURE_AFFINITY_GROUP);
		if (affinityGroup == null) {
			throw new IllegalArgumentException("Custom field '" + MicrosoftAzureCloudDriver.AZURE_AFFINITY_GROUP
					+ "' must be set");
		}
	}

	private AzureDeploymentContext getAzureContext() {
		return computeDriver.getAzureContext();
	}

	private MicrosoftAzureRestClient getAzureClient() {
		return computeDriver.getAzureContext().getAzureClient();
	}

	/**
	 * This method create and attach a new data disk to the first VM of the deployment role list. <br />
	 * Then it will detach the disk. <br/>
	 * Until Azure provide a way to create a data disk with no attach, we are obligate to use this ugly algorithm. <br />
	 * <br />
	 * The data disk will be created into the least occupied storage account from the storage template list.
	 * 
	 * @param templateNames
	 *            The storage template name.
	 * @param location
	 *            not used as we are suppose to use only 1 affinity group.
	 * @param duration
	 *            Max duration time to process the operation.
	 * @param timeUnit
	 *            Time unit for the operation duration.
	 * @return VolumeDetails where the id is the data disk name generate by Azure.
	 */
	@Override
	public VolumeDetails createVolume(String templateNames, String location, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {

		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);

		StorageTemplate storageTemplate = cloudStorage.getTemplates().get(templateNames);
		if (storageTemplate == null) {
			throw new StorageProvisioningException("Storage template '" + templateNames + "' does not exist.");
		}

		String balancedStorageAccount = null;
		@SuppressWarnings("unchecked")
		List<String> storageAccounts = (List<String>) storageTemplate.getCustom().get(STORAGE_ACCOUNT_PROPERTY);
		if (storageAccounts == null || storageAccounts.isEmpty()) {
			throw new StorageProvisioningException("No storage templates were definied in cloud");
		}

		long currentTimeInMillis = System.currentTimeMillis();
		long lockTimeout = endTime - currentTimeInMillis;
		if (lockTimeout < 0) {
			throw new StorageProvisioningException("Timeout. Abord request to configurate storage accounts");
		}
		logger.fine("Waiting for pending storage driver request lock for lock "
				+ driverPendingRequest.hashCode());

		try {
			boolean lockAcquired = driverPendingRequest.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
			if (!lockAcquired) {
				throw new TimeoutException("Failed to acquire lock for configurating storage accounts for "
						+ "volumes after " + lockTimeout + " milliseconds");
			}
			logger.fine("Configurating storage accounts for volumes");

			ExecutorService executorService = Executors.newFixedThreadPool(storageAccounts.size());
			List<Future<?>> futures = new ArrayList<Future<?>>();

			for (String storage : storageAccounts) {
				Future<?> f = executorService.submit(new StorageCallable(
						getAzureContext().getAzureClient(), affinityGroup, storage, endTime));
				futures.add(f);
			}

			logger.finest("Waiting for storage accounts configuration to finish");
			for (Future<?> f : futures) {
				f.get();
			}
			executorService.shutdownNow();

			balancedStorageAccount = MicrosoftAzureUtils.getBalancedStorageAccount(storageAccounts,
					getAzureContext().getAzureClient());

			driverPendingRequest.unlock();
			logger.fine("Configuration of storage accounts for volumes finished");

		} catch (Exception e) {
			logger.warning("Failed configurating storage accounts for volumes : " + e.getMessage());
			logger.warning("Selecting a storage account instead : " + storageAccounts.get(0));
			balancedStorageAccount = storageAccounts.get(0);
			driverPendingRequest.unlock();
		}

		MicrosoftAzureRestClient azureClient = getAzureClient();
		AzureDeploymentContext context = getAzureContext();

		logger.info("Creating a new volume");
		String dataDiskName = null;
		try {

			// Get the deployment to retrieve the role name
			String deploymentName = context.getDeploymentName();
			String cloudServiceName = context.getCloudServiceName();
			Deployment deployment = azureClient.getDeploymentByName(cloudServiceName, deploymentName);
			Role role = deployment.getRoleList().getRoles().get(0);

			String roleName = role.getRoleName();

			// Generate the vhd filename
			StringBuilder vhdFilename = new StringBuilder();
			vhdFilename.append(storageTemplate.getNamePrefix());
			vhdFilename.append(cloudServiceName);
			vhdFilename.append("-data-");
			vhdFilename.append(UUIDHelper.generateRandomUUID(4));

			// Create a data disk
			int diskSize = storageTemplate.getSize();
			String hostCaching = this.getHostCaching(storageTemplate);
			dataDiskName = azureClient.createDataDisk(cloudServiceName, deploymentName, roleName,
					balancedStorageAccount, vhdFilename.toString(), diskSize, hostCaching, endTime);

		} catch (MicrosoftAzureException e) {
			throw new StorageProvisioningException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageProvisioningException(e);
		}

		// Return the data disk name as id.
		VolumeDetails volumeDetails = new VolumeDetails();
		logger.fine("Created volume : " + dataDiskName);
		volumeDetails.setId(dataDiskName);
		return volumeDetails;
	}

	private String getHostCaching(StorageTemplate storageTemplate) {
		String hostCaching = (String) storageTemplate.getCustom().get(DATADISK_HOSTCACHING);
		hostCaching = StringUtils.isBlank(hostCaching) ? HOSTCACHING_NONE : hostCaching.trim();

		if (StringUtils.equalsIgnoreCase(hostCaching, HOSTCACHING_READ_ONLY)) {
			hostCaching = HOSTCACHING_READ_ONLY;
		} else if (StringUtils.equalsIgnoreCase(hostCaching, HOSTCACHING_READ_WRITE)) {
			hostCaching = HOSTCACHING_READ_WRITE;
		} else {
			if (!StringUtils.equalsIgnoreCase(hostCaching, HOSTCACHING_NONE)) {
				logger.warning("Unknown host caching value " + hostCaching + ". Using default (" + HOSTCACHING_NONE
						+ ")");
			}
			hostCaching = HOSTCACHING_NONE;
		}

		return hostCaching;
	}

	/**
	 * This method will attach an existing data disk to the VM specified by the IP. When attaching the data disk, the
	 * label field is field with the rolename. Using this trick allow the users to track which data disk are attached to
	 * which VMs.
	 * 
	 * @param volumeId
	 *            The data disk name
	 * @param device
	 *            The LUN number
	 * @param ip
	 *            The private IP of the target VM
	 * @param duration
	 *            Max duration time to process the operation.
	 * @param timeUnit
	 *            Time unit for the operation duration.
	 */
	@Override
	public void attachVolume(String volumeId, String device, String ip, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);

		MicrosoftAzureRestClient azureClient = getAzureClient();
		AzureDeploymentContext context = getAzureContext();

		String cloudServiceName = context.getCloudServiceName();
		String deploymentName = context.getDeploymentName();
		String roleName = null;

		try {
			// Get the deployment to retrieve the role name
			Deployment deployment = azureClient.getDeploymentByName(cloudServiceName, deploymentName);
			for (RoleInstance role : deployment.getRoleInstanceList().getRoleInstances()) {
				if (role.getIpAddress().equals(ip)) {
					roleName = role.getRoleName();
				}
			}

			// Update the disk label
			azureClient.updateDataDiskLabel(volumeId, roleName, endTime);

			// Attach the existing data disk to the VM
			azureClient.addExistingDataDiskToVM(cloudServiceName, deploymentName, roleName,
					volumeId, Integer.parseInt(device), endTime);
		} catch (MicrosoftAzureException e) {
			throw new StorageProvisioningException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageProvisioningException(e);
		}
	}

	/**
	 * This method will detach a data disk from the VM specified by the IP.<br />
	 * The label field of the data disk is cleaned to keep track of what data disk is attached to which VM.
	 * 
	 * @param volumeId
	 *            The data disk name
	 * @param ip
	 *            The private IP of the target VM
	 * @param duration
	 *            Max duration time to process the operation.
	 * @param timeUnit
	 *            Time unit for the operation duration.
	 */
	@Override
	public void detachVolume(String volumeId, String ip, long duration, TimeUnit timeUnit) throws TimeoutException,
			StorageProvisioningException {
		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);

		MicrosoftAzureRestClient azureClient = getAzureClient();
		AzureDeploymentContext context = getAzureContext();

		String cloudServiceName = context.getCloudServiceName();
		String deploymentName = context.getDeploymentName();
		String roleName = null;
		int lun = -1;

		try {
			// Get the deployment to retrieve the role name
			Deployment deployment = azureClient.getDeploymentByName(cloudServiceName, deploymentName);
			for (RoleInstance role : deployment.getRoleInstanceList().getRoleInstances()) {
				if (role.getIpAddress().equals(ip)) {
					roleName = role.getRoleName();
				}
			}

			// Retrieve the LUN number from the data disk name
			List<DataVirtualHardDisk> dataVirtualHardDisks =
					deployment.getRoleList().getRoleByName(roleName).getDataVirtualHardDisks()
							.getDataVirtualHardDisks();
			for (DataVirtualHardDisk dvhd : dataVirtualHardDisks) {
				if (dvhd.getDiskName().equals(volumeId)) {
					lun = dvhd.getLun();
				}
			}

			// Detach the data disk from the VM
			azureClient.removeDataDisk(cloudServiceName, deploymentName, roleName, lun, endTime);

			// set a new label name for the detached disk
			String newLabel = "unattached-data-disk" + UUIDHelper.generateRandomUUID(4);
			azureClient.updateDataDiskLabel(volumeId, newLabel, endTime);

		} catch (MicrosoftAzureException e) {
			throw new StorageProvisioningException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageProvisioningException(e);
		}

	}

	/**
	 * This method delete a data disk.
	 * 
	 * @param volumeId
	 *            The data disk name
	 * @param location
	 *            Not used.
	 * @param duration
	 *            Max duration time to process the operation.
	 * @param timeUnit
	 *            Time unit for the operation duration.
	 */
	@Override
	public void deleteVolume(String location, String volumeId, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		MicrosoftAzureRestClient azureClient = getAzureClient();
		try {
			azureClient.deleteDisk(volumeId, true, endTime);
		} catch (MicrosoftAzureException e) {
			throw new StorageProvisioningException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageProvisioningException(e);
		}

	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public void setComputeContext(Object computeContext) throws StorageProvisioningException {
		if (computeContext != null) {
			if (!(computeContext instanceof MicrosoftAzureCloudDriver)) {
				throw new StorageProvisioningException("Incompatible context class "
						+ computeContext.getClass().getName());
			}
			this.computeDriver = (MicrosoftAzureCloudDriver) computeContext;
		}
		logger.info("Initialize Azure storage driver with context: " + computeDriver.getAzureContext());
	}

	@Override
	public Set<VolumeDetails> listAllVolumes() throws StorageProvisioningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<VolumeDetails> listVolumes(String ip, long duration, TimeUnit timeUnit) throws TimeoutException,
			StorageProvisioningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getVolumeName(String volumeId) throws StorageProvisioningException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void terminateAllVolumes(long duration, TimeUnit timeUnit) throws StorageProvisioningException,
			TimeoutException {

		logger.finest("deleting volumes is done in the cleaning process of the azure compute driver");
	}

}
