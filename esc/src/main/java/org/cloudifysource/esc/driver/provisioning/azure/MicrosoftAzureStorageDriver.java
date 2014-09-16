package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.storage.BaseStorageDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;

/**
 * Class that manages storages on azure cloud
 * 
 * @author fastconnect
 * 
 */
public class MicrosoftAzureStorageDriver extends BaseStorageDriver implements StorageProvisioningDriver {

	@Override
	public void setConfig(Cloud cloud, String computeTemplateName) {
		// TODO Auto-generated method stub

	}

	@Override
	public VolumeDetails createVolume(String templateName, String location, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void attachVolume(String volumeId, String device, String ip, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachVolume(String volumeId, String ip, long duration, TimeUnit timeUnit) throws TimeoutException,
			StorageProvisioningException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteVolume(String location, String volumeId, long duration, TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<VolumeDetails> listVolumes(String ip, long duration, TimeUnit timeUnit) throws TimeoutException,
			StorageProvisioningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVolumeName(String volumeId) throws StorageProvisioningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setComputeContext(Object computeContext) throws StorageProvisioningException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<VolumeDetails> listAllVolumes() throws StorageProvisioningException {
		// TODO Auto-generated method stub
		return null;
	}
}
