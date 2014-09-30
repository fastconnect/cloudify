package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.Map;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstanceList;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MicrosoftAzureCloudDriverTestIT extends BaseDriverTestIT {

	private final static Logger LOGGER = Logger.getLogger(MicrosoftAzureCloudDriverTestIT.class.getName());

	@Test
	public void testNamingConvention() throws Exception {

		this.startAndStopManagementMachine("ubuntu1410", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws Exception {
				Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
				String codeCountry = cloudProperties.get("codeCountry");
				String codeEnvironment = cloudProperties.get("codeEnvironment");
				String cloudServiceCode = cloudProperties.get("cloudServiceCode");

				// Cloud Service : {codeCountry}{codeEnv}{codeCloudService}XXX
				String cloudServiceName = String.format("%s%s%s001", codeCountry, codeEnvironment, cloudServiceCode);

				// Machine Name : {codeCountry}{codeEnv}{serviceName}XXX
				String machineName = String.format("%s%s" + MicrosoftAzureCloudDriver.CLOUDIFY_MANAGER_NAME + "1",
						codeCountry, codeEnvironment);

				MicrosoftAzureRestClient client = AzureTestUtils.createMicrosoftAzureRestClient();

				// Check Cloud service name
				Deployment deployment = client.getDeploymentByDeploymentName(cloudServiceName, cloudServiceName);
				Assert.assertEquals("Cloud service name is not correct", cloudServiceName,
						deployment.getHostedServiceName());

				// Check Cloudify management machine name
				RoleInstanceList roleInstanceList = deployment.getRoleInstanceList();
				Assert.assertNotNull("Couldn't find VM " + machineName,
						roleInstanceList.getInstanceRoleByRoleName(machineName));
			}
		});
	}

	@Test
	@Ignore("Issue with windows tests. Powershell can't be executed on Linux machines. Reactivate the test once the custom data feature is implemented")
	public void testStartWindowsManagementMachine() throws Exception {
		this.startAndStopManagementMachine("win2012");
	}

	@Test
	@Ignore("Issue with windows tests. Powershell can't be executed on Linux machines. Reactivate the test once the custom data feature is implemented")
	public void testStartWindowsManagementMachineWithStaticIp() throws Exception {
		this.startAndStopManagementMachine("win2012_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

	@Test
	public void testStartUbuntuManagementMachine() throws Exception {
		this.startAndStopManagementMachine("ubuntu1410");
	}

	@Test
	public void testStartUbuntuManagementMachineWithStaticIp() throws Exception {
		this.startAndStopManagementMachine("ubuntu1410_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

	@Test
	public void testStartUbuntuMachinesMultipleStaticIPs() throws Exception {
		MicrosoftAzureCloudDriver driver = createDriver("ubuntu1410_multipleFixedIPs", true);
		try {
			this.startManagementMachine(driver, new MachineDetailsAssertion() {
				@Override
				public void additionalAssertions(MachineDetails md) {
					Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
				}
			});

			this.startAndStopMachine("ubuntu1410_multipleFixedIPs", new MachineDetailsAssertion() {
				@Override
				public void additionalAssertions(MachineDetails md) {
					Assert.assertEquals("10.0.0.13", md.getPrivateAddress());
				}
			});
		} finally {
			stopManagementMachines(driver);
		}
	}

	@Test
	public void testStartUbuntuManagementMachineInExsitingCS() throws Exception {

		final long endTime = 10000000;

		String computeTemplateName = "ubuntu1410_cloudservice";
		cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null, computeTemplateName);

		// Retrieve the cloud service name
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");
		String location = (String) cloud.getCustom().get("azure.affinity.location");

		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);

		// Created resources should be released by the stop management
		azureRestClient.createAffinityGroup(affinityPrefix, location, endTime);
		azureRestClient.createCloudService(affinityPrefix, cloudServiceName, endTime);

		this.startAndStopManagementMachine(computeTemplateName, new MachineDetailsAssertion() {
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
					Assert.assertNotNull(deployment.getRoleInstanceList().getInstanceRoleByRoleName(roleName));
				} catch (Exception e) {
					Assert.fail(e.getMessage());
				}
			};
		});
	}

	@Test
	public void testStartUbuntuManagementMachineEndPoints() throws Exception {

		final long endTime = 10000000;

		String computeTemplateName = "ubuntu1410_endpoints";
		cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null, computeTemplateName);

		// Retrieve the cloud service name
		final ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		String affinityPrefix = (String) cloud.getCustom().get("azure.affinity.group");
		String location = (String) cloud.getCustom().get("azure.affinity.location");

		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);

		// Created resources should be released by the stop management
		azureRestClient.createAffinityGroup(affinityPrefix, location, endTime);
		azureRestClient.createCloudService(affinityPrefix, cloudServiceName, endTime);

		this.startAndStopManagementMachine(computeTemplateName, new MachineDetailsAssertion() {
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
					Role role = deployment.getRoleList().getRoleByName(roleName);
					Assert.assertNotNull(role);

					NetworkConfigurationSet networkConfigurationSet = role.getConfigurationSets().
							getNetworkConfigurationSet();
					networkConfigurationSet.getInputEndpoints();

					// valid ports
					Assert.assertNotNull(networkConfigurationSet.getInputEndpoints().getInputEndpointByPort(5000));
					Assert.assertNotNull(networkConfigurationSet.getInputEndpoints().getInputEndpointByPort(81));

				} catch (Exception e) {
					Assert.fail(e.getMessage());
				}
			};
		});
	}

	@Test
	@Ignore
	// TODO enhance test ( verification )
	public void testStartWindowsManagementMachineJoinDomain() throws Exception {
		String computeTemplateName = "win2012_joindomain";
		this.startAndStopManagementMachine(computeTemplateName);

	}
}
