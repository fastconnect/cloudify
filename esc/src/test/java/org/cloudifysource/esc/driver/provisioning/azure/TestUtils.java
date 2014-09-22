package org.cloudifysource.esc.driver.provisioning.azure;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;

public class TestUtils {

	private static final Logger logger = Logger.getLogger(TestUtils.class.getName());

	private static final String credentialsPath = "./src/test/resources/clouds/credentials/azure";
	private static final String credentialPropertyName = "azure_win-cloud.properties";

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_CLOUD_SERVICE_PREFIX = "cloudifycloudservice";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";

	private TestUtils() {

	}

	public static MicrosoftAzureRestClient createOpenStackNumergyNetworkClient() throws MalformedURLException {
		File configFile = new File(credentialsPath, credentialPropertyName);
		ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL());

		String subscriptionId = (String) config.getProperty("subscriptionId");
		String pfxFile = (String) config.getProperty("pfxFile");
		String pathToPfx = credentialsPath + "/upload/" + pfxFile;
		String pfxPassword = (String) config.getProperty("pfxPassword");

		return new MicrosoftAzureRestClient(subscriptionId, pathToPfx, pfxPassword, CLOUDIFY_AFFINITY_PREFIX,
				CLOUDIFY_CLOUD_SERVICE_PREFIX,
				CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
	}

	public static Cloud createCloud(String cloudFolder, String cloudName, String overridesDir) throws IOException,
			FileNotFoundException, DSLException {
		// Create a temporary directory
		File tmpCloudDir = File.createTempFile("azure-test", "");
		tmpCloudDir.delete();
		tmpCloudDir.mkdir();
		tmpCloudDir.deleteOnExit();
		new File(tmpCloudDir, "upload").mkdir();
		logger.info("Initialize driver using cloud folder at " + tmpCloudDir.getAbsolutePath());

		// Copy openstack-numergy cloud file into the temporary directory
		FileUtils.copyDirectory(new File(cloudFolder, cloudName), tmpCloudDir);
		FileUtils.copyDirectory(new File(credentialsPath), tmpCloudDir);
		// FileUtils.deleteQuietly(new File(tmpCloudDir, cloudName + "-cloud.properties"));
		// FileUtils.moveFile(new File(tmpCloudDir, credentialPropertyName), new File(tmpCloudDir, cloudName
		// + "-cloud.properties"));

		// Configure openstack-numergy-cloud.properties file
		File propertiesFile = new File(tmpCloudDir, cloudName + "-cloud.properties");
		if (propertiesFile.exists() && overridesDir != null) {
			String openstackPropertiesFile = FileUtils.readFileToString(propertiesFile);

			// Create openstack-numergy-cloud.properties from openstack-numergy-cloud.properties.sample and override it
			// with the content of
			// the test properties file
			Properties current = new Properties();
			current.load(new FileInputStream(propertiesFile));
			Properties overrides = new Properties();
			overrides.load(new FileInputStream(new File(overridesDir, cloudName + "-cloud.properties")));
			for (Object key : overrides.keySet()) {
				if (current.contains(key)) {
					String value = (String) overrides.get(key.toString());
					if (value.contains("\\")) {
						value = value.replaceAll("\\\\", "/");
					}
					openstackPropertiesFile =
							openstackPropertiesFile.replaceAll(String.format("%s=.*", key.toString()),
									String.format("%s=%s", key.toString(), value));
				} else {
					openstackPropertiesFile += "\n" + key + "=" + overrides.get(key.toString());
				}
			}
			File tmpPropertiesfile = new File(tmpCloudDir, cloudName + "-cloud.properties");
			FileUtils.writeStringToFile(tmpPropertiesfile, openstackPropertiesFile);
			logger.info("Using properties :\n" + FileUtils.readFileToString(tmpPropertiesfile));
		}

		// Use Cloudify API to read the cloud configuration file
		return ServiceReader.readCloudFromDirectory(tmpCloudDir.getAbsolutePath());
	}
}
