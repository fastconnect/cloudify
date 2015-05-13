package org.cloudifysource.utilitydomain.context.storage;

import java.util.logging.Logger;

import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.domain.context.storage.AzureStorageFacade;
import org.cloudifysource.dsl.internal.context.AzureRemoteStorageProvisioningDriver;

public class AzureStorageFacadeImpl implements AzureStorageFacade {

	private static final Logger logger = Logger.getLogger(AzureStorageFacadeImpl.class.getName());

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
		logger.info(String.format("Creating storage account %s for service %s", name, serviceContext.getServiceName()));
		remoteStorageApi.createStorageAccount(name);

	}

	@Override
	public void createStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info(String.format("Creating storage account %s for service %s (timeout=%s)", name,
				serviceContext.getServiceName(), timeoutInMillis));
		remoteStorageApi.createStorageAccount(name, timeoutInMillis);
	}

	@Override
	public String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		logger.info(String.format("Creating data disk in storage account %s for service %s (%s)", storageAccountName,
				serviceContext.getServiceName(), ipAddress));
		return remoteStorageApi.createDataDisk(storageAccountName, ipAddress, size, lun, hostCaching);
	}

	@Override
	public String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching,
			long timeoutInMillis) throws RemoteStorageOperationException, LocalStorageOperationException {
		logger.info(String.format("Creating data disk in storage account %s for service %s (%s) (timeout=%s)",
				storageAccountName, serviceContext.getServiceName(), ipAddress, timeoutInMillis));
		return remoteStorageApi.createDataDisk(storageAccountName, ipAddress, size, lun, hostCaching, timeoutInMillis);
	}

	@Override
	public void deleteDataDisk(String diskName) throws RemoteStorageOperationException, LocalStorageOperationException {
		logger.info(String.format("Deleting data disk %s for service %s", diskName, serviceContext.getServiceName()));
		remoteStorageApi.deleteDataDisk(diskName);
	}

	@Override
	public void deleteDataDisk(String diskName, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info(String.format("Deleting data disk %s for service %s (timeout=%s)", diskName,
				serviceContext.getServiceName(), timeoutInMillis));
		remoteStorageApi.deleteDataDisk(diskName, timeoutInMillis);
	}

	@Override
	public void attachDataDisk(String diskName, String ipAddress, int lun) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info(String.format("Attaching data disk %s for service %s (%s)", diskName,
				serviceContext.getServiceName(), ipAddress));
		remoteStorageApi.attachDataDisk(diskName, ipAddress, lun);
	}

	@Override
	public void attachDataDisk(String diskName, String ipAddress, int lun, long timeoutInMillis)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		logger.info(String.format("Attaching data disk %s for service %s (%s) (timeout=%s)", diskName,
				serviceContext.getServiceName(), ipAddress, timeoutInMillis));
		remoteStorageApi.attachDataDisk(diskName, ipAddress, lun, timeoutInMillis);
	}

	@Override
	public void detachDataDisk(String diskName, String ipAddress, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException {

		logger.info(String.format("Detaching data disk %s for service %s (%s) (timeout=%s)", diskName,
				serviceContext.getServiceName(), ipAddress, timeoutInMillis));

		remoteStorageApi.detachDataDisk(diskName, ipAddress, timeoutInMillis);

	}

	@Override
	public void detachDataDisk(String diskName, String ipAddress) throws RemoteStorageOperationException,
			LocalStorageOperationException {

		logger.info(String.format("Detaching data disk %s for service %s (%s) (timeout=%s)", diskName,
				serviceContext.getServiceName(), ipAddress));

		remoteStorageApi.detachDataDisk(diskName, ipAddress);

	}

	@Override
	public void deleteStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException {
		logger.info(String.format("Deleting storage account %s", name ));
		remoteStorageApi.deleteStorageAccount(name, timeoutInMillis);

	}

	@Override
	public void deleteStorageAccount(String name) throws RemoteStorageOperationException, LocalStorageOperationException {
		logger.info(String.format("Deleting storage account %s", name));
		remoteStorageApi.deleteStorageAccount(name);

	}
}
