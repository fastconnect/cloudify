package org.cloudifysource.domain.context.storage;

import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;

public interface AzureStorageFacade {

	void createStorageAccount(String name) throws RemoteStorageOperationException, LocalStorageOperationException;

	void createContainer(String storageAccountName, String containerName) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void createFileService(String storageAccountName) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void createDataDisk(String containerName, String vhdFileName) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void attachDataDisk(String diskName) throws RemoteStorageOperationException, LocalStorageOperationException;

}
