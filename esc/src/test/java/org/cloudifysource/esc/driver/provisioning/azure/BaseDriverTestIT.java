package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudProvider;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;
import org.junit.Assert;

public class BaseDriverTestIT {

	private static final String CLOUD_RESOURCES_FOLDER = "./src/main/resources/clouds";
	private static final String AZURE_WIN = "azure_win";

	protected static final Logger logger = Logger.getLogger(BaseDriverTestIT.class.getName());
	protected static final int TIMEOUT = 1000 * 60 * 60; // 60 minutes

	protected Cloud cloud;

	protected MicrosoftAzureCloudDriver createDriver(String computeTemplate, String overridesDir, boolean isManagement)
			throws IOException, DSLException, CloudProvisioningException {
		return this.createDriver(computeTemplate, overridesDir, isManagement, CLOUD_RESOURCES_FOLDER, AZURE_WIN);
	}

	protected MicrosoftAzureCloudDriver createDriver(String computeTemplate, String overridesDir,
			boolean isManagement, String cloudFolder, String cloudName)
			throws IOException, DSLException, CloudProvisioningException {

		cloud = AzureTestUtils.createCloud(cloudFolder, cloudName, overridesDir, computeTemplate);

		// Create the Driver
		MicrosoftAzureCloudDriver driver = new MicrosoftAzureCloudDriver();
		ProvisioningContextImpl ctx = new ProvisioningContextImpl();
		ProvisioningContextAccess.setCurrentProvisioingContext(ctx);
		ctx.getInstallationDetailsBuilder().setCloud(cloud);
		ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(computeTemplate);
		ctx.getInstallationDetailsBuilder().setTemplate(template);

		ComputeDriverConfiguration configuration = new ComputeDriverConfiguration();
		configuration.setCloud(cloud);
		configuration.setCloudTemplate(computeTemplate);
		configuration.setManagement(isManagement);

		CloudProvider provider = cloud.getProvider();

		provider.setMachineNamePrefix("agent");

		if (!isManagement) {
			configuration.setServiceName("default.NOM");
		}

		driver.setConfig(configuration);
		logger.info(cloud.toString());
		return driver;
	}

	protected void doStartManagementMachine(String computeTemplate) throws Exception {
		this.doStartManagementMachine(computeTemplate, null, new MachineDetailsAssertion());
	}

	protected void doStartManagementMachine(String computeTemplate, MachineDetailsAssertion additionalAssertion)
			throws Exception {
		this.doStartManagementMachine(computeTemplate, null, additionalAssertion);
	}

	protected void doStartManagementMachine(String computeTemplate, String overridesDir,
			MachineDetailsAssertion assertion) throws Exception {
		MicrosoftAzureCloudDriver driver = this.createDriver(computeTemplate, overridesDir, true);
		try {
			MachineDetails[] mds = driver.startManagementMachines(null, TIMEOUT, TimeUnit.MILLISECONDS);
			for (MachineDetails md : mds) {
				assertion.assertMachineDetails(md);
			}
		} finally {
			if (driver != null) {
				try {
					driver.stopManagementMachines();
				} catch (Exception e) {
					logger.log(Level.WARNING, "Fail to stop machine", e);
				}
			}
		}
	}

	protected void doTestStartMachine(String computeTemplate, String overridesDir) throws Exception {
		MicrosoftAzureCloudDriver driver = this.createDriver(computeTemplate, overridesDir, false);
		MachineDetails md = null;
		try {
			md = driver.startMachine(null, TIMEOUT, TimeUnit.MILLISECONDS);
			new MachineDetailsAssertion().assertMachineDetails(md);
		} finally {
			if (md != null) {
				driver.stopMachine(md.getPrivateAddress(), TIMEOUT, TimeUnit.MILLISECONDS);
			}
		}
	}

	public class MachineDetailsAssertion {
		public void assertMachineDetails(MachineDetails md) {
			Assert.assertNotNull("MachineDetails is null", md);
			Assert.assertNotNull("machineId is null", md.getMachineId());
			String privateAddress = md.getPrivateAddress();
			String publicAddress = md.getPublicAddress();
			logger.info("private ip=" + privateAddress);
			logger.info("public ip=" + publicAddress);
			Assert.assertNotNull("public address is null", publicAddress);
			Assert.assertNotNull("private address is null", privateAddress);
			additionalAssertions(md);
		}

		public void additionalAssertions(MachineDetails md) {
		}
	}
}
