package org.cloudifysource.esc.driver.provisioning.storage.azure;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.AzureDriverTestBuilder;
import org.cloudifysource.esc.driver.provisioning.azure.AzureTestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.BaseDriverTestIT;
import org.cloudifysource.esc.driver.provisioning.azure.MachineDetailsAssertion;
import org.cloudifysource.esc.driver.provisioning.azure.MicrosoftAzureCloudDriver;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AzureStorageProvisioningDriverImplIT extends BaseDriverTestIT {

	private AzureStorageProvisioningDriverImpl azureStorageProvisioningDriverImpl;
	private AzureDriverTestBuilder azureDriverTestBuilder;
	final private String storageAccountName = "itstoragetobedeleted";

	@Before
	public void init() throws Exception {
		this.azureStorageProvisioningDriverImpl = new AzureStorageProvisioningDriverImpl();
		azureDriverTestBuilder = new AzureDriverTestBuilder();

	}

	private void setStorageContext(String computeTemplateName, Cloud cloud, String deploymentName) throws Exception,
			IOException, DSLException {

		final ComputeTemplate computeTemplate =
				cloud.getCloudCompute().getTemplates().get(computeTemplateName);

		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);

		AzureDeploymentContext context = new AzureDeploymentContext(cloudServiceName, cloudServiceName,
				azureRestClient);

		if (StringUtils.isNotBlank(deploymentName)) {
			context.setDeploymentName(deploymentName);
			context.setCloudServiceName(deploymentName);
		}
		MicrosoftAzureCloudDriver azureCloudDriver =
				azureDriverTestBuilder.createDriverAndSetConfig(computeTemplateName);

		azureStorageProvisioningDriverImpl.setComputeContext(azureCloudDriver);
		azureStorageProvisioningDriverImpl.setConfig(cloud, computeTemplateName);
		azureCloudDriver.setAzureDeploymentContext(context);

	}

	@Test
	public void createAndDeleteStorageAccountTest() throws Exception {

		final String computeTemplateName = "ubuntu1410_disk";
		this.startAndStopManagementMachine(computeTemplateName, new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws Exception {

				try {

					final Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
							computeTemplateName);

					final ComputeTemplate computeTemplate =
							cloud.getCloudCompute().getTemplates().get(computeTemplateName);

					final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
					String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

					final MicrosoftAzureRestClient azureRestClient =
							AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
					final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");
					azureRestClient.setVirtualNetwork(networkName);

					setStorageContext(computeTemplateName, cloud, cloudServiceName);

					// create storage test
					azureStorageProvisioningDriverImpl.createStorageAccount(storageAccountName, 25, TimeUnit.MINUTES);
					Assert.assertTrue(azureRestClient.storageExists(storageAccountName));

					azureStorageProvisioningDriverImpl.deleteStorageAccount(storageAccountName, 25, TimeUnit.MINUTES);
					Assert.assertFalse(azureRestClient.storageExists(storageAccountName));

				} catch (Exception e) {
					Assert.fail("Failed delete storage Test :" + e.getMessage());
				}
			}

		});

	}

	@Test
	public void detachDeleteDiskTest() throws Exception {

		final String computeTemplateName = "ubuntu1410_disk";

		this.startAndStopManagementMachine(computeTemplateName, new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws Exception {

				try {

					final String vmName = "NZ01CFYM1";

					final Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
							computeTemplateName);

					final String storageAccountName = (String) cloud.getCustom().get("azure.storage.account.prefix");

					final ComputeTemplate computeTemplate =
							cloud.getCloudCompute().getTemplates().get(computeTemplateName);

					final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
					String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

					final MicrosoftAzureRestClient azureRestClient =
							AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
					final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");
					azureRestClient.setVirtualNetwork(networkName);

					setStorageContext(computeTemplateName, cloud, cloudServiceName);

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

					azureStorageProvisioningDriverImpl.detachDataDisk(dataDiskName, instanceIpAddress, 25,
							TimeUnit.MINUTES);
					deployment = azureRestClient.getDeploymentByIp(instanceIpAddress, true);
					role = deployment.getRoleList().getRoleByName(vmName);

					Assert.assertFalse(role.isDiskAttached(dataDiskName));

					azureStorageProvisioningDriverImpl.deleteDataDisk(dataDiskName, 25, TimeUnit.MINUTES);
					Assert.assertFalse(azureRestClient.listDisks().contains(dataDiskName));

				} catch (Exception e) {
					Assert.fail("Failed detach disk Test :" + e.getMessage());
				}

			}

		});

	}

	@Test(expected = StorageProvisioningException.class)
	public void invalidLunTest() throws Exception {

		final String computeTemplateName = "ubuntu1410_disk";

		this.startAndStopManagementMachine(computeTemplateName, new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws Exception {

				final Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
						computeTemplateName);

				final ComputeTemplate computeTemplate =
						cloud.getCloudCompute().getTemplates().get(computeTemplateName);

				final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
				String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");

				final MicrosoftAzureRestClient azureRestClient =
						AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
				final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");
				azureRestClient.setVirtualNetwork(networkName);

				setStorageContext(computeTemplateName, cloud, cloudServiceName);

				final String storageAccountName = (String) cloud.getCustom().get("azure.storage.account.prefix");
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

				Assert.fail("'invalid lun test' should not reach this line, test should be in fail status");

			}

		});

	}

}
