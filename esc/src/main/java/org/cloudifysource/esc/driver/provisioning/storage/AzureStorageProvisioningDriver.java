/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;

/**
 * This interface provides an entry point to provision block storage devices and attach them to specific compute
 * instances nodes created by the {@link org.cloudifysource.esc.driver.provisioning.BaseComputeDriver} .
 * 
 * This is still a work in progress. so there may be changes to the method signatures in the near future. Also, snapshot
 * functionality will be provided.
 * 
 * @author elip
 * @author adaml
 * @since 2.5.0
 * 
 */
public interface AzureStorageProvisioningDriver {

	/**
	 * Called once at initialization. Sets configuration members for later use. use this method to extract all necessary
	 * information needed for future provisioning calls.
	 * 
	 * @param cloud
	 *            - The {@link Cloud} Object.
	 * @param computeTemplateName
	 *            - the compute template name used to provision the machine that this volume is dedicated to.
	 */
	void setConfig(Cloud cloud, String computeTemplateName);

	/**
	 * Sets the jClouds compute context if exists.
	 * 
	 * @param context
	 *            jClouds compute context
	 * 
	 * @throws StorageProvisioningException
	 *             In-case context does not match the specified storage driver.
	 */
	void setComputeContext(final Object context) throws StorageProvisioningException;

	/**
	 * This method is used when using cloudify storage static attachment
	 */
	void onMachineFailure(final ProvisioningContext context, final String templateName, final long duration,
			final TimeUnit unit) throws TimeoutException, CloudProvisioningException, StorageProvisioningException;

	/**
	 * Close all resources.
	 */
	void close();

	void createStorageAccount(String name, long duration, TimeUnit timeUnit) throws StorageProvisioningException,
			TimeoutException;

	void createContainer(String storageAccountName, String containerName) throws StorageProvisioningException;

	void createFileService(String storageAccountName) throws StorageProvisioningException;

	void createDataDisk(String containerName, String vhdFileName) throws StorageProvisioningException;

	void attachDataDisk(String diskName) throws StorageProvisioningException;

}
