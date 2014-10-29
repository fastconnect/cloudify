package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.storage.CloudStorage;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
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

	@Override
	public void setConfig(Cloud cloud, String computeTemplateName) {
		cloudStorage = cloud.getCloudStorage();
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

		// TODO Auto-generated method stub

		VolumeDetails volumeDetails = new VolumeDetails();
		AzureDeploymentContext azureContext = computeDriver.getAzureContext();
		if (azureContext != null) {
			volumeDetails.setId(computeDriver.getAzureContext().getDeploymentName() + "-"
					+ azureContext.getCloudServiceName());
		}
		logger.info("azureContext=" + azureContext);
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
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

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
