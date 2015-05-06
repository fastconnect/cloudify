package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.azure.AzureDriverTestBuilder;
import org.cloudifysource.esc.driver.provisioning.azure.AzureTestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AzureStorageProvisioningDriverImplIT {

	private AzureStorageProvisioningDriverImpl azureStorageProvisioningDriverImpl;
	private AzureDriverTestBuilder azureDriverTestBuilder;

	@Before
	public void init() throws Exception {
		this.azureStorageProvisioningDriverImpl = new AzureStorageProvisioningDriverImpl();
		azureDriverTestBuilder = new AzureDriverTestBuilder();

	}

	@Test
	public void detachVolumeTest() throws Exception {

		String computeTemplateName = "ubuntu1410_cloudservice";
		final String vmName = "NZ01CFYM1";
		final String storageAccountName = "ittestcreatestorage";

		final Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
				computeTemplateName);

		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		MicrosoftAzureCloudDriver azureCloudDriver =
				azureDriverTestBuilder.createDriverAndSetConfig(computeTemplateName);

		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
		final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");
		azureRestClient.setVirtualNetwork(networkName);

		AzureDeploymentContext context =
				new AzureDeploymentContext(cloudServiceName, cloudServiceName, azureRestClient);
		azureStorageProvisioningDriverImpl.setComputeContext(azureCloudDriver);
		azureStorageProvisioningDriverImpl.setConfig(cloud, computeTemplateName);
		azureCloudDriver.setAzureDeploymentContext(context);

		azureStorageProvisioningDriverImpl.createStorageAccount(storageAccountName, 60, TimeUnit.MINUTES);

		final String instanceIpAddress = "10.0.0.4";
		final int diskSize = 5;
		final int lun = 7;

		String dataDiskName = azureStorageProvisioningDriverImpl.createDataDisk(
				storageAccountName,
				instanceIpAddress,
				diskSize,
				lun,
				"ReadWrite",
				20, TimeUnit.MINUTES);

		Deployment deployment = azureRestClient.getDeploymentByIp(instanceIpAddress, true);
		Role role = deployment.getRoleList().getRoleByName(vmName);

		Assert.assertTrue(role.isDiskAttached(dataDiskName));

		azureStorageProvisioningDriverImpl.detachDataDisk(dataDiskName, instanceIpAddress, 25, TimeUnit.MINUTES);
		deployment = azureRestClient.getDeploymentByIp(instanceIpAddress, true);
		role = deployment.getRoleList().getRoleByName(vmName);

		Assert.assertFalse(role.isDiskAttached(dataDiskName));

	}

	@Test(expected = StorageProvisioningException.class)
	public void invalidLunTest() throws Exception {

		String computeTemplateName = "ubuntu1410_cloudservice";
		final String storageAccountName = "ittestcreatestorage";

		final Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
				computeTemplateName);

		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		MicrosoftAzureCloudDriver azureCloudDriver =
				azureDriverTestBuilder.createDriverAndSetConfig(computeTemplateName);

		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
		final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");
		azureRestClient.setVirtualNetwork(networkName);

		AzureDeploymentContext context =
				new AzureDeploymentContext(cloudServiceName, cloudServiceName, azureRestClient);
		azureStorageProvisioningDriverImpl.setComputeContext(azureCloudDriver);
		azureStorageProvisioningDriverImpl.setConfig(cloud, computeTemplateName);
		azureCloudDriver.setAzureDeploymentContext(context);

		azureStorageProvisioningDriverImpl.createStorageAccount(storageAccountName, 60, TimeUnit.MINUTES);

		final String instanceIpAddress = "10.0.0.4";
		final int diskSize = 5;
		final int lun = 16;

		azureStorageProvisioningDriverImpl.createDataDisk(
				storageAccountName,
				instanceIpAddress,
				diskSize,
				lun,
				"ReadWrite",
				20, TimeUnit.MINUTES);

	}

}
