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

		this.doStartManagementMachine("ubuntu1410", new MachineDetailsAssertion() {

			private String affinityGroup;
			private String affinityLocation;
			private String netWorksite;
			private String netAddress;
			private String storageAccount;
			private String codeCountry;
			private String codeEnvironment;
			private String cloudServiceCode;
			private String availabilityCode;

			@Override
			public void additionalAssertions(MachineDetails md) throws Exception {

				Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
				this.affinityGroup = cloudProperties.get("affinityGroup");
				this.affinityLocation = cloudProperties.get("affinityLocation");
				this.netWorksite = cloudProperties.get("netWorksite");
				this.netAddress = cloudProperties.get("netAddress");
				this.storageAccount = cloudProperties.get("storageAccount");
				this.codeCountry = cloudProperties.get("codeCountry");
				this.codeEnvironment = cloudProperties.get("codeEnvironment");
				this.cloudServiceCode = cloudProperties.get("cloudServiceCode");
				this.availabilityCode = cloudProperties.get("availabilityCode");

				String cloudServiceName = String.format("%s%s%s001", codeCountry, codeEnvironment,
						cloudServiceCode);
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
		this.doStartManagementMachine("medium_win2012_cloudservice");
	}

}
