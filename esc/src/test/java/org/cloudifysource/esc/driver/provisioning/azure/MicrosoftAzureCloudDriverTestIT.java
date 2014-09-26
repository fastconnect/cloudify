package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstanceList;
import org.junit.Assert;
import org.junit.Test;

public class MicrosoftAzureCloudDriverTestIT extends BaseDriverTestIT {

	private final static Logger LOGGER = Logger.getLogger(MicrosoftAzureCloudDriverTestIT.class.getName());

	@Test
	public void testStartWindowsManagementMachine() throws Exception {
		this.doStartManagementMachine("win2012");
	}

	@Test
	public void testStartMachineWindowsWithStaticIp() throws Exception {
		this.doStartManagementMachine("win2012_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

	@Test
	public void testStartUbuntuManagementMachine() throws Exception {
		this.doStartManagementMachine("ubuntu1410");
	}

	@Test
	public void testStartMachineUbuntuWithStaticIp() throws Exception {
		this.doStartManagementMachine("ubuntu1410_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

	@Test
	public void testStartWinManagementMachineInExsitingCS() throws Exception {

		String computeTemplateName = "medium_win2012_cloudservice";
		cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null,
				computeTemplateName);
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");

		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");
		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);

		final HostedServices listHostedServices = azureRestClient.listHostedServices();

		final long endTime = 10000000;
		if (!listHostedServices.contains(cloudServiceName)) {
			azureRestClient.createCloudService(affinityPrefix, cloudServiceName, 10000000);
		}

		this.doStartManagementMachine("medium_win2012_cloudservice", new MachineDetailsAssertion() {

			@Override
			public void additionalAssertions(MachineDetails md) {
				try {

					HostedServices cloudServices = azureRestClient.listHostedServices();
					Assert.assertTrue(cloudServices.contains(cloudServiceName));
					String deploymentSlot = (String) computeTemplate.getCustom().get("azure.deployment.slot");
					Deployment deployment = azureRestClient.listDeploymentsBySlot(cloudServiceName, deploymentSlot,
							endTime);

					Assert.assertNotNull(deployment);
					// TODO make dynamic roleName
					String roleName = String.format("%sCFYM1", cloud.getProvider().getManagementGroup());
					System.out.println();
					Assert.assertNotNull(deployment.getRoleInstanceList().getInstanceRoleByRoleName(roleName));
				} catch (Exception e) {
					Assert.fail(e.getMessage());
				}
			};
		});
	}
}
