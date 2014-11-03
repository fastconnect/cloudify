package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.storage.CloudStorage;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
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

	private final static Logger logger = Logger.getLogger(MicrosoftAzureStorageDriver.class.getName());

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
	 * @param templateName
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
	public VolumeDetails createVolume(String templateName, String location, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);

		StorageTemplate storageTemplate = cloudStorage.getTemplates().get(templateName);
		if (storageTemplate == null) {
			throw new StorageProvisioningException("Storage template '" + templateName + "' does not exist.");
		}

		String namePrefix = storageTemplate.getNamePrefix();
		int diskSize = storageTemplate.getSize();
		String saName = (String) storageTemplate.getCustom().get(MicrosoftAzureCloudDriver.AZURE_STORAGE_ACCOUNT);

		MicrosoftAzureRestClient azureClient = getAzureClient();
		AzureDeploymentContext context = getAzureContext();

		String dataDiskName = null;
		try {
			// Make sure the storage account exists
			azureClient.createStorageAccount(affinityGroup, saName, endTime);

			String cloudServiceName = context.getCloudServiceName();
			String deploymentName = context.getDeploymentName();
			// Get the deployment to retrieve the role name
			Deployment deployment = azureClient.getDeploymentByDeploymentName(cloudServiceName, deploymentName);
			Role role = deployment.getRoleList().getRoles().get(0);

			String roleName = role.getRoleName();

			// Generate the vhd filename
			StringBuilder vhdFilename = new StringBuilder();
			vhdFilename.append(namePrefix);
			vhdFilename.append(cloudServiceName);
			vhdFilename.append("-data-");
			vhdFilename.append(UUIDHelper.generateRandomUUID(4));

			// Create a data disk
			dataDiskName = azureClient.createDataDisk(cloudServiceName, deploymentName, roleName, saName,
					vhdFilename.toString(), diskSize, endTime);

		} catch (MicrosoftAzureException e) {
			throw new StorageProvisioningException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new StorageProvisioningException(e);
		}

		// Return the data disk name as id.
		VolumeDetails volumeDetails = new VolumeDetails();
		volumeDetails.setId(dataDiskName);
		return volumeDetails;
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
			Deployment deployment = azureClient.getDeploymentByDeploymentName(cloudServiceName, deploymentName);
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
			Deployment deployment = azureClient.getDeploymentByDeploymentName(cloudServiceName, deploymentName);
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
}
