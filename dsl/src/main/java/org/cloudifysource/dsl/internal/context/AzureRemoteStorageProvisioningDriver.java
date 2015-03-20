package org.cloudifysource.dsl.internal.context;

import java.rmi.Remote;

import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;

/**
 * 
 * @author victor
 * 
 */
public interface AzureRemoteStorageProvisioningDriver extends Remote {

	void createStorageAccount(String name) throws RemoteStorageOperationException;

	void createStorageAccount(String name, long timeoutInMillis) throws RemoteStorageOperationException;

	String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching)
			throws RemoteStorageOperationException;

	String createDataDisk(String storageAccountName, String ipAddress, int size, int lun, String hostCaching,
			long timeoutInMillis) throws RemoteStorageOperationException;

	void deleteDataDisk(String diskName) throws RemoteStorageOperationException;

	void deleteDataDisk(String diskName, long timeoutInMillis) throws RemoteStorageOperationException;

	void attachDataDisk(String diskName, String ipAddress, int lun)
			throws RemoteStorageOperationException;

	void attachDataDisk(String diskName, String ipAddress, int lun, long timeoutInMillis)
			throws RemoteStorageOperationException;
}
