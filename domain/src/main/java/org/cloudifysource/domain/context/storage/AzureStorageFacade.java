package org.cloudifysource.domain.context.storage;

import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;

public interface AzureStorageFacade {

	void createStorageAccount(String name) throws RemoteStorageOperationException, LocalStorageOperationException;

	void createStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void deleteStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void deleteStorageAccount(String name) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching)
			throws RemoteStorageOperationException, LocalStorageOperationException;

	String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching,
			long timeoutInMillis) throws RemoteStorageOperationException, LocalStorageOperationException;

	void deleteDataDisk(String diskName) throws RemoteStorageOperationException, LocalStorageOperationException;

	void deleteDataDisk(String diskName, long timeoutInMillis) throws RemoteStorageOperationException,
			LocalStorageOperationException;

	void attachDataDisk(String diskName, String ipAddress, int lun)
			throws RemoteStorageOperationException, LocalStorageOperationException;

	void attachDataDisk(String diskName, String ipAddress, int lun, long timeoutInMillis)
			throws RemoteStorageOperationException, LocalStorageOperationException;

	void detachDataDisk(String diskName, String ipAddress)
			throws RemoteStorageOperationException, LocalStorageOperationException;

	void detachDataDisk(String diskName, String ipAddress, long timeoutInMillis)
			throws RemoteStorageOperationException, LocalStorageOperationException;

}
