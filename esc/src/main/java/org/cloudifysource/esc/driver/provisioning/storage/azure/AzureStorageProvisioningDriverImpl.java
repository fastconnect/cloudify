package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.storage.AzureStorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;

public class AzureStorageProvisioningDriverImpl implements AzureStorageProvisioningDriver {

	private static final Logger logger = Logger.getLogger(AzureStorageProvisioningDriverImpl.class.getName());

	private static final long STORAGE_CREATION_SLEEP_TIMEOUT = 30000L;

	private MicrosoftAzureCloudDriver computeDriver;

	private String affinityGroup;

	@Override
	public void setConfig(Cloud cloud, String computeTemplateName) {
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
	public void onMachineFailure(final ProvisioningContext context, final String templateName, final long duration,
			final TimeUnit unit) throws TimeoutException, CloudProvisioningException, StorageProvisioningException {
		String machineId = context.getPreviousMachineDetails().getMachineId();
		logger.warning("Machine id '" + machineId
				+ "'has failed and might have create volumes which are not cleaned propertly");
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void createStorageAccount(String storageAccountName, long duration, TimeUnit timeUnit)
			throws StorageProvisioningException, TimeoutException {

		long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);

		boolean created = false;
		while (!created) {
			try {
				getAzureClient().createStorageAccount(affinityGroup, storageAccountName, endTime);
				created = true;
			} catch (Exception e) {
				try {
					logger.log(Level.WARNING, "Error creating the storage account '" + storageAccountName
							+ "'. Sleeping " + STORAGE_CREATION_SLEEP_TIMEOUT + " ms before reattempt",
							e.getMessage());
					Thread.sleep(STORAGE_CREATION_SLEEP_TIMEOUT);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					logger.warning("Sleep interrupted");
				}
			}
			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("Timeout creating the storage account " + storageAccountName);
			}
		}
	}

	@Override
	public void createContainer(String storageAccountName, String containerName) {
		logger.info(String.format("storageAccountName=%s, containerName=%s", storageAccountName, containerName));
	}

	@Override
	public void createFileService(String storageAccountName) {
		logger.info(String.format("storageAccountName=%s", storageAccountName));
	}

	@Override
	public void createDataDisk(String containerName, String vhdFileName) {
		logger.info(String.format("containerName=%s, vhdFileName=%s", containerName, vhdFileName));
	}

	@Override
	public void attachDataDisk(String diskName) {
		logger.info(String.format("diskName=%s", diskName));
	}

}
