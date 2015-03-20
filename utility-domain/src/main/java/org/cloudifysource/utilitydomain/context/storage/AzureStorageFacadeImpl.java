package org.cloudifysource.utilitydomain.context.storage;

import java.util.logging.Logger;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.domain.context.storage.AzureStorageFacade;
import org.cloudifysource.dsl.internal.context.AzureRemoteStorageProvisioningDriver;

public class AzureStorageFacadeImpl implements AzureStorageFacade {

	private static final Logger logger = java.util.logging.Logger
			.getLogger(AzureStorageFacadeImpl.class.getName());

	private ServiceContext serviceContext;
	private AzureRemoteStorageProvisioningDriver remoteStorageApi;

	public AzureStorageFacadeImpl(ServiceContext serviceContext, AzureRemoteStorageProvisioningDriver storageApi) {
		this.serviceContext = serviceContext;
		this.remoteStorageApi = storageApi;
		// TODO Check the use of management space ??? (cf. StorageFacadeImpl)
	}

	@Override
	public void createStorageAccount(String name) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info("Creating storage account " + name + " for service " + serviceContext.getServiceName() + ".");
		remoteStorageApi.createStorageAccount(name);

	}

	@Override
	public void createStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info("Creating storage account " + name + " for service " + serviceContext.getServiceName() + ".");
		remoteStorageApi.createStorageAccount(name, timeoutInMillis);
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
