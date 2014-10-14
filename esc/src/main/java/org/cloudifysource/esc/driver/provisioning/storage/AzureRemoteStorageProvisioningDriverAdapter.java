/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.storage;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.dsl.internal.context.AzureRemoteStorageProvisioningDriver;

/**
 * 
 * @author elip
 * 
 */
public class AzureRemoteStorageProvisioningDriverAdapter implements AzureRemoteStorageProvisioningDriver {

	private Logger logger = java.util.logging.Logger
			.getLogger(AzureRemoteStorageProvisioningDriverAdapter.class.getName());

	private static final long DEFAULT_STORAGE_OPERATION_TIMEOUT = 60 * 1000;

	private AzureStorageProvisioningDriver storageProvisioningDriver;

	public AzureRemoteStorageProvisioningDriverAdapter(final AzureStorageProvisioningDriver driver) {
		this.storageProvisioningDriver = driver;
	}

	/**
	 * Logs the exception as severe and throws a {@link RemoteStorageOperationException}. If the exception is
	 * serializable it is included in the newly thrown exception.
	 * 
	 * @param message
	 *            The error message to log
	 * @param e
	 *            The exception to log and re-throw if possible
	 * @throws RemoteStorageOperationException
	 */
	private void logSevereAndThrow(final String message, final Exception e) throws RemoteStorageOperationException {

		logger.log(Level.SEVERE, message, e);

		if (isSerializable(e)) {
			throw new RemoteStorageOperationException(message, e);
		} else {
			throw new RemoteStorageOperationException(message);
		}
	}

	/**
	 * Checks if the given object can be serialized.
	 * 
	 * @param obj
	 *            The object to serialize
	 * @return True is serialization was successful, False otherwise
	 */
	private boolean isSerializable(final Object obj) {

		boolean serializable = false;
		try {
			new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(obj);
			serializable = true;
		} catch (Exception e) {
			// failed to serialize
		}

		return serializable;
	}

	@Override
	public void createStorageAccount(String name) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver.createStorageAccount(name);
		} catch (final Exception e) {
			logSevereAndThrow(e.getMessage(), e);
		}
	}

	@Override
	public void createContainer(String storageAccountName, String containerName) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver.createContainer(storageAccountName, containerName);
		} catch (final Exception e) {
			logSevereAndThrow(e.getMessage(), e);
		}
	}

	@Override
	public void createFileService(String storageAccountName) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver.createFileService(storageAccountName);
		} catch (final Exception e) {
			logSevereAndThrow(e.getMessage(), e);
		}
	}

	@Override
	public void createDataDisk(String containerName, String vhdFileName) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver.createDataDisk(containerName, vhdFileName);
		} catch (final Exception e) {
			logSevereAndThrow(e.getMessage(), e);
		}
	}

	@Override
	public void attachDataDisk(String diskName) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver.attachDataDisk(diskName);
		} catch (final Exception e) {
			logSevereAndThrow(e.getMessage(), e);
		}
	}
}
