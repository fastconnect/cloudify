package org.cloudifysource.utilitydomain.context.storage;

import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.domain.context.storage.AzureStorageFacade;
import org.cloudifysource.dsl.internal.context.AzureRemoteStorageProvisioningDriver;

public class AzureStorageFacadeImpl implements AzureStorageFacade {
	private AzureRemoteStorageProvisioningDriver remoteStorageApi;

	public AzureStorageFacadeImpl(AzureRemoteStorageProvisioningDriver storageApi) {
		this.remoteStorageApi = storageApi;
	}

	@Override
	public void createStorageAccount(String name)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		remoteStorageApi.createStorageAccount(name);

	}

	@Override
	public void createContainer(String storageAccountName, String containerName)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		remoteStorageApi.createContainer(storageAccountName, containerName);

	}

	@Override
	public void createFileService(String storageAccountName)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		remoteStorageApi.createFileService(storageAccountName);

	}

	@Override
	public void createDataDisk(String containerName, String vhdFileName)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		remoteStorageApi.createDataDisk(containerName, vhdFileName);
	}

	@Override
	public void attachDataDisk(String diskName)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		remoteStorageApi.attachDataDisk(diskName);
	}

}
