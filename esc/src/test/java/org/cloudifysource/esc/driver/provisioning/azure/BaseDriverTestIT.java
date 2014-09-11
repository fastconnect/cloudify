package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.IOException;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudProvider;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;

public class BaseDriverTestIT {

	protected static final Logger logger = Logger.getLogger(BaseDriverTestIT.class.getName());

	protected static final int TIMEOUT = 1000 * 60 * 60; // 60 minutes

	protected Cloud cloud;

	protected MicrosoftAzureCloudDriver createDriver(String computeTemplate, String overridesDir, boolean isManagement)
			throws IOException, DSLException, CloudProvisioningException {
		return this.createDriver(computeTemplate, overridesDir, isManagement, "./src/main/resources/clouds",
				"azure_win-cdiscount");
	}

	protected MicrosoftAzureCloudDriver createDriver(String cloudFolder, String cloudName) throws IOException,
			DSLException, CloudProvisioningException {
		return this.createDriver("LINUX", null, true, cloudFolder, cloudName);
	}

	protected MicrosoftAzureCloudDriver createDriver(String computeTemplate, String overridesDir,
			boolean isManagement, String cloudFolder,
			String cloudName) throws IOException, DSLException, CloudProvisioningException {

		cloud = TestUtils.createCloud(cloudFolder, cloudName, overridesDir);

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

		provider.setManagementGroup("management");
		provider.setMachineNamePrefix("agent");

		if (!isManagement) {
			configuration.setServiceName("default.service");
		}

		driver.setConfig(configuration);
		logger.info(cloud.toString());
		return driver;
	}
}
