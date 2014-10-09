package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextAccess;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContextImpl;

public class AzureDriverTestBuilder {

	private static final String AZURE_WIN = "azure_win";
	private static final String CLOUD_RESOURCES_FOLDER = "./src/main/resources/clouds";
	public static final String DEFAULT_SERVICE_NAME = "SOLR";

	protected static final Logger logger = Logger.getLogger(AzureDriverTestBuilder.class.getName());

	private boolean isManagement = true;

	private String cloudName = AZURE_WIN;
	private String cloudFolder = CLOUD_RESOURCES_FOLDER;
	private String serviceName = DEFAULT_SERVICE_NAME;
	private String overridesDir;

	private ComputeDriverConfiguration configuration;

	public AzureDriverTestBuilder() {
	}

	public AzureDriverTestBuilder(boolean isManagement) {
		this.isManagement = isManagement;
	}

	// ************************************************************
	// * Configuration for the driver creation.
	// *
	// *******
	public String getOverridesDir() {
		return overridesDir;
	}

	public void setOverridesDir(String overridesDir) {
		this.overridesDir = overridesDir;
	}

	public String getCloudFolder() {
		return cloudFolder;
	}

	public void setCloudFolder(String cloudFolder) {
		this.cloudFolder = cloudFolder;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public MicrosoftAzureCloudDriver createDriverAndSetConfig(String computeTemplate)
			throws IOException, DSLException, CloudProvisioningException {
		MicrosoftAzureCloudDriver driver = this.createDriver(computeTemplate);
		driver.setConfig(configuration);
		return driver;
	}

	/**
	 * Initialize the driver object without calling the setConfig method.<br />
	 * Use this method to override configuration.
	 */
	public MicrosoftAzureCloudDriver createDriver(String computeTemplate) throws IOException, FileNotFoundException,
			DSLException,
			CloudProvisioningException {
		Cloud cloud = AzureTestUtils.createCloud(cloudFolder, cloudName, overridesDir, computeTemplate);

		// Create the Driver
		MicrosoftAzureCloudDriver driver = new MicrosoftAzureCloudDriver();
		ProvisioningContextImpl ctx = new ProvisioningContextImpl();
		ProvisioningContextAccess.setCurrentProvisioingContext(ctx);
		ctx.getInstallationDetailsBuilder().setCloud(cloud);
		ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(computeTemplate);
		ctx.getInstallationDetailsBuilder().setTemplate(template);

		configuration = new ComputeDriverConfiguration();
		configuration.setCloud(cloud);
		configuration.setCloudTemplate(computeTemplate);
		configuration.setManagement(isManagement);

		if (!isManagement) {
			configuration.setServiceName("default." + this.serviceName);
		} else {
			cloud.getConfiguration().setManagementMachineTemplate(computeTemplate);
		}

		logger.info(cloud.toString());
		return driver;
	}

	public boolean isManagement() {
		return isManagement;
	}

	public void setManagement(boolean isManagement) {
		this.isManagement = isManagement;
	}

	public ComputeDriverConfiguration getConfiguration() {
		if (configuration == null) {
			throw new IllegalStateException("You must use createDriver method first");
		}
		return configuration;
	}

}
