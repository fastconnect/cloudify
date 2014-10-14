package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
import org.cloudifysource.esc.driver.provisioning.storage.AzureStorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;

public class AzureStorageProvisioningDriverImpl implements AzureStorageProvisioningDriver {

	private static final Logger logger = Logger.getLogger(AzureStorageProvisioningDriverImpl.class.getName());

	private static final String EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API = "try_to_connect_to_cloud_api";
	private static final String EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API = "connection_to_cloud_api_succeeded";

	private Cloud cloud;

	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();

	@Override
	public void setConfig(Cloud cloud, String computeTemplateName) {
		logger.info("Initializing Azure storage provisioning on Microsoft Azure");
		this.cloud = cloud;
		final String provider = cloud.getProvider().getProvider();

		publishEvent(EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API, provider);
		logger.info("Creating Azure storage driver context");
		initDeployer();
		publishEvent(EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API, provider);
	}

	private void initDeployer() {
		logger.finest("TODO initialize deployer");

	}

	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listener : this.eventsListenersList) {
			listener.onProvisioningEvent(eventName, args);
		}
	}

	@Override
	public void setComputeContext(Object context) throws StorageProvisioningException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void createStorageAccount(String name) {
		logger.info(String.format("name=%s", name));

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
