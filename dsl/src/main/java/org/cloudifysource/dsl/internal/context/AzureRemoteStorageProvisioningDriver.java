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

	void createContainer(String storageAccountName, String containerName) throws RemoteStorageOperationException;

	void createFileService(String storageAccountName) throws RemoteStorageOperationException;

	void createDataDisk(String containerName, String vhdFileName) throws RemoteStorageOperationException;

	void attachDataDisk(String diskName) throws RemoteStorageOperationException;
}
