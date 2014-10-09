package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Dns;
import org.cloudifysource.esc.driver.provisioning.azure.model.DnsServer;
import org.cloudifysource.esc.driver.provisioning.azure.model.DnsServers;
import org.cloudifysource.esc.driver.provisioning.azure.model.DnsServersRef;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;
import org.cloudifysource.esc.driver.provisioning.azure.model.NetworkConfigurationSet;
import org.cloudifysource.esc.driver.provisioning.azure.model.Role;
import org.cloudifysource.esc.driver.provisioning.azure.model.RoleInstanceList;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSite;
import org.cloudifysource.esc.util.Utils;
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

				// Check availabilitySet
				String availabilitySet = deployment.getRoleList().getRoleByName(machineName).getAvailabilitySetName();
				Assert.assertNotNull(availabilitySet);
				Assert.assertTrue(availabilitySet.startsWith(codeCountry + codeEnvironment + "AVITTEST"));

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
		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriverAndSetConfig("ubuntu1410_multipleFixedIPs");
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
		final Cloud cloud =
				AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null, computeTemplateName);

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
		final Cloud cloud =
				AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null, computeTemplateName);

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

					NetworkConfigurationSet netConfigSet = role.getConfigurationSets().getNetworkConfigurationSet();
					InputEndpoints inputEndpoints = netConfigSet.getInputEndpoints();

					// Valid ports
					InputEndpoint httpEndpoint = inputEndpoints.getInputEndpointByName("HTTP");
					Assert.assertNotNull("Missing HTTP endpoint", httpEndpoint);
					Assert.assertEquals(8080, httpEndpoint.getPort());

					InputEndpoint sshEndpoint = inputEndpoints.getInputEndpointByName("SSH");
					Assert.assertNotNull("Missing SHH endpoint", sshEndpoint);
					Assert.assertEquals(22, sshEndpoint.getPort());
				} catch (Exception e) {
					Assert.fail(e.getMessage());
				}
			}

		});
	}

	@Test
	@Ignore("Need to check what happens if the same computer name is added into AD")
	public void testStartWindowsManagementMachineJoinDomain() throws Exception {
		String computeTemplateName = "win2012_joindomain";
		this.startAndStopManagementMachine(computeTemplateName);
	}

	@Test
	public void testCustomDataUbuntu() throws Exception {

		this.startAndStopManagementMachine("ubuntu1410_customdata", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws TimeoutException {
				try {
					Utils.executeSSHCommand(md.getPublicAddress(),
							"ls -l /home/administrateur/hello.txt",
							md.getRemoteUsername(),
							md.getRemotePassword(),
							null,
							30L, TimeUnit.SECONDS);
				} catch (Exception e) {
					Assert.fail("Failed to find the file '/home/administrateur/hello.txt' which should have been created by the custom data.");
				}
			}
		});

	}

	@Test
	@Ignore("Custom Data not working on Windows (content issue?)")
	public void testCustomDataWindows() throws Exception {

		this.startAndStopManagementMachine("win2012_customdata", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) throws TimeoutException {
				logger.warning("TODO assert that created file does exist.");
			}
		});

	}

	@Test
	public void testUbuntuComputeTemplateStorage() throws Exception {
		String computeTemplate = "ubuntu1410_storage";

		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriverAndSetConfig(computeTemplate);
		Cloud cloud = driverBuilder.getConfiguration().getCloud();
		try {
			StorageAssertion storageAssertionManagement = new StorageAssertion("CFYM1", "specificstorage", true);
			storageAssertionManagement.setManagementGroup(cloud.getProvider().getManagementGroup());
			this.startManagementMachine(driver, storageAssertionManagement);

			final String roleSuffix = driverBuilder.getServiceName() + "001";
			StorageAssertion storageAssertionMachine = new StorageAssertion(roleSuffix, "specificstorage", false);
			this.startAndStopMachine(computeTemplate, storageAssertionMachine);
		} finally {
			stopManagementMachines(driver);
		}

	}

	@Test
	public void testDeleteRoleUbuntuMachineFromDeployment() throws Exception {

		String computeTemplateName = "ubuntu1410_deleterole";
		Cloud cloud = AzureTestUtils.createCloud("./src/main/resources/clouds", "azure_win", null, computeTemplateName);
		ComputeTemplate computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		final String cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		final String networkName = cloud.getCloudNetwork().getCustom().get("azure.networksite.name");

		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriverAndSetConfig(computeTemplateName);

		Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
		String affinityPrefix = cloudProperties.get("affinityGroup");
		final MicrosoftAzureRestClient azureRestClient =
				AzureTestUtils.createMicrosoftAzureRestClient(cloudServiceName, affinityPrefix);
		azureRestClient.setVirtualNetwork(networkName);

		try {

			final DeleteRoleAssertion deleteRoleAssertion = new DeleteRoleAssertion(azureRestClient,
					computeTemplateName, computeTemplate);

			this.startManagementMachine(driver, deleteRoleAssertion);

			final String deploymentLabel = deleteRoleAssertion.getDeploymentLabel();
			Assert.assertNotNull(deploymentLabel);

			this.startAndStopMachine(computeTemplateName, new MachineDetailsAssertion());

			deleteRoleAssertion.assertCloudSeviceAndDeploymentExist(deploymentLabel);

		} finally {
			stopManagementMachines(driver);
		}
	}

	@Test
	public void testNetworkAndSubnets() throws Exception {
		final MicrosoftAzureRestClient restClient = AzureTestUtils.createMicrosoftAzureRestClient();
		final String dataSubnetName = "data_subnet";

		Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
		final String networkSite = cloudProperties.get("netWorksite");

		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver mngDriver = driverBuilder.createDriverAndSetConfig("ubuntu1410");

		try {
			// Start the manager machine without creating the service subnet
			this.startManagementMachine(mngDriver, new MachineDetailsAssertion() {
				@Override
				public void additionalAssertions(MachineDetails md) throws Exception {
					Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
					String networkSite = cloudProperties.get("netWorksite");
					VirtualNetworkConfiguration vnetConfig = restClient.getVirtualNetworkConfiguration();
					VirtualNetworkSite virtualNetworkSite =
							vnetConfig.getVirtualNetworkSites().getVirtualNetworkSite(networkSite);
					Assert.assertNull("Doesn't expect existing subnet '" + dataSubnetName + "'",
							virtualNetworkSite.getSubnets().getSubnet(dataSubnetName));
				}
			});

			// Starting the agent machine should create the subnet
			ServiceNetwork serviceNetwork = new ServiceNetwork();
			serviceNetwork.setTemplate("DATA_NET");
			MicrosoftAzureCloudDriver serviceDriver =
					this.startAndStopMachine("ubuntu1410", serviceNetwork, new MachineDetailsAssertion() {
						@Override
						public void additionalAssertions(MachineDetails md) throws Exception {
							VirtualNetworkConfiguration vnetConfig = restClient.getVirtualNetworkConfiguration();
							VirtualNetworkSite virtualNetworkSite =
									vnetConfig.getVirtualNetworkSites().getVirtualNetworkSite(networkSite);
							Assert.assertNotNull("Expecting subnet '" + dataSubnetName + "'",
									virtualNetworkSite.getSubnets().getSubnet(dataSubnetName));
						}
					});

			// Test deletion of the subnet
			serviceDriver.onServiceUninstalled(30, TimeUnit.MINUTES);
			VirtualNetworkConfiguration vnetConfig = restClient.getVirtualNetworkConfiguration();
			VirtualNetworkSite virtualNetworkSite =
					vnetConfig.getVirtualNetworkSites().getVirtualNetworkSite(networkSite);
			Assert.assertNull("Expected subnet '" + dataSubnetName + "' to be deleted",
					virtualNetworkSite.getSubnets().getSubnet(dataSubnetName));

		} finally {
			stopManagementMachines(mngDriver);
			restClient.removeSubnetByName(networkSite, dataSubnetName, System.currentTimeMillis() + TIMEOUT);
		}

	}

	@Test
	public void testDnsServers() throws Exception {
		Map<String, String> cloudProperties = AzureTestUtils.getCloudProperties();
		String networkName = cloudProperties.get("netWorksite");
		String dnsname = "myDNS";
		String dnsIpAddress = "10.0.0.4";

		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriver("ubuntu1410");
		ComputeDriverConfiguration configuration = driverBuilder.getConfiguration();
		CloudNetwork cloudNetwork = configuration.getCloud().getCloudNetwork();
		cloudNetwork.getCustom().put("azure.dns.servers", dnsname + ":" + dnsIpAddress);
		driver.setConfig(configuration);
		try {
			this.startManagementMachine(driver);
			MicrosoftAzureRestClient azureClient = AzureTestUtils.createMicrosoftAzureRestClient();
			VirtualNetworkConfiguration vnetConfig = azureClient.getVirtualNetworkConfiguration();
			Dns dns = vnetConfig.getDns();
			Assert.assertNotNull(dns);
			DnsServers dnsServers = dns.getDnsServers();
			Assert.assertNotNull(dnsServers);
			DnsServer dnsServer = dnsServers.getDnsServerByName(dnsname);
			Assert.assertNotNull(dnsServer);
			Assert.assertEquals(dnsIpAddress, dnsServer.getIpAddress());

			VirtualNetworkSite vnetsite = vnetConfig.getVirtualNetworkSiteConfigurationByName(networkName);
			DnsServersRef dnsServersRef = vnetsite.getDnsServersRef();
			Assert.assertNotNull(dnsServersRef);
			Assert.assertTrue(dnsServersRef.containsDnsName(dnsname));
		} finally {
			this.stopManagementMachines(driver);
		}
	}

}
