package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.Map;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstanceList;
import org.junit.Assert;
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
				String cloudServiceName = String.format("%s%s%s001", codeCountry, codeEnvironment,
						cloudServiceCode);

				// Machine Name : {codeCountry}{codeEnv}{serviceName}XXX
				String machineName = String.format("%s%s" + MicrosoftAzureCloudDriver.CLOUDIFY_MANAGER_NAME + "1",
						codeCountry, codeEnvironment);

				MicrosoftAzureRestClient client = AzureTestUtils.createMicrosoftAzureRestClient();

				// Check Cloud service name
				Deployment deployment = client.getDeploymentByDeploymentName("vk14adm001", "vk14adm001");
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
	public void testStartWindowsManagementMachine() throws Exception {
		this.startAndStopManagementMachine("win2012");
	}

	@Test
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
	public void testStartWinManagementMachineInExsitingCS() throws Exception {
		this.startAndStopManagementMachine("medium_win2012_cloudservice");
	}

}
