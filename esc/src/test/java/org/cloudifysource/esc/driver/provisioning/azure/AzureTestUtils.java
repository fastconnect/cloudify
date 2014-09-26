package org.cloudifysource.esc.driver.provisioning.azure;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.ComputeTemplateHolder;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;

public class AzureTestUtils {

	private static final Logger logger = Logger.getLogger(AzureTestUtils.class.getName());

	private static final String TEMPLATES_FOLDER = "src/test/resources/azure/templates";

	private static final String defaultCredentialsFolder = "./src/test/resources/clouds/credentials/azure";
	private static final String defaultCredentialsFilename = "azure_win-cloud.properties";

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_CLOUD_SERVICE_PREFIX = "cloudifycloudservice";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";

	private AzureTestUtils() {

	}

	private static String getCredentialsFolder() {
		String systemCredentialsPath = (String) System.getProperty("CredentialsFolder");
		return systemCredentialsPath == null ? defaultCredentialsFolder : systemCredentialsPath;
	}

	private static String getCredentialsFilename() {
		String systemCredentialsFilename = (String) System.getProperty("CredentialsFilename");
		return systemCredentialsFilename == null ? defaultCredentialsFilename : systemCredentialsFilename;
	}

	public static File getCredentialsFile() {
		return new File(getCredentialsFolder(), getCredentialsFilename());
	}

	public static Map<String, String> getCloudProperties() throws FileNotFoundException, IOException {

		File credentialsFile = AzureTestUtils.getCredentialsFile();
		Properties props = new Properties();
		props.load(new FileInputStream(credentialsFile));

		Map<String, String> cloudProperties = new HashMap<String, String>();

		String regex = "\"(.*)\"";
		Pattern pattern = Pattern.compile(regex);
		for (Object k : props.keySet()) {
			String key = (String) k;
			if (!key.startsWith("//")) {
				String value = (String) props.getProperty(key);
				Matcher matcher = pattern.matcher(value);
				if (matcher.matches()) {
					cloudProperties.put(key, matcher.group(1));
				} else {
					cloudProperties.put(key, value);
				}
			}
		}

		return cloudProperties;
	}

	private static void copyTemplate(String computeTemplate, File destFolder) throws IOException {
		String templateDSLFilename = computeTemplate + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX;
		File templateDSLFile = new File(TEMPLATES_FOLDER, templateDSLFilename);
		if (templateDSLFile.exists()) {
			FileUtils.copyFile(templateDSLFile, new File(destFolder, templateDSLFilename));
			String templatePropsFilename = computeTemplate + DSLUtils.TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX;
			File templatePropsFile = new File(TEMPLATES_FOLDER, templatePropsFilename);
			if (templatePropsFile.exists()) {
				logger.info("Use template properties file from  '" + templatePropsFile + "'");
				FileUtils.copyFile(templatePropsFile, new File(destFolder, templatePropsFilename));
			} else {
				File credentialsProperties = getCredentialsFile();
				logger.info("Use template properties file from  '" + credentialsProperties + "'");
				FileUtils.copyFile(credentialsProperties, new File(destFolder, templatePropsFilename));
			}
		} else {
			logger.warning("Couldn't find compute template '" + computeTemplate
					+ "' in folder '" + TEMPLATES_FOLDER + "'");
		}

	}

	private static ComputeTemplateHolder retrieveComputeTemplate(File folder, String templateName) throws DSLException {
		List<ComputeTemplateHolder> readCloudTemplatesFromFile =
				new ComputeTemplatesReader().readCloudTemplatesFromFile(
						new File(folder, templateName + "-template.groovy"));
		ComputeTemplateHolder computeTemplateHolder = readCloudTemplatesFromFile.get(0);
		return computeTemplateHolder;
	}

	public static MicrosoftAzureRestClient createMicrosoftAzureRestClient(String cloudServicePrefix,
			String affinityPrefix) throws MalformedURLException {
		File configFile = new File(getCredentialsFolder(), getCredentialsFilename());
		ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL());

		String subscriptionId = (String) config.getProperty("subscriptionId");
		String pfxFile = (String) config.getProperty("pfxFile");
		String pathToPfx = getCredentialsFolder() + "/upload/" + pfxFile;
		String pfxPassword = (String) config.getProperty("pfxPassword");

		return new MicrosoftAzureRestClient(subscriptionId, pathToPfx, pfxPassword, affinityPrefix,
				cloudServicePrefix, CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
	}

	public static MicrosoftAzureRestClient createMicrosoftAzureRestClient() throws MalformedURLException {
		File configFile = getCredentialsFile();
		ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL());

		String subscriptionId = (String) config.getProperty("subscriptionId");
		String pfxFile = (String) config.getProperty("pfxFile");
		String pathToPfx = getCredentialsFolder() + "/upload/" + pfxFile;
		String pfxPassword = (String) config.getProperty("pfxPassword");

		return new MicrosoftAzureRestClient(subscriptionId, pathToPfx, pfxPassword, CLOUDIFY_AFFINITY_PREFIX,
				CLOUDIFY_CLOUD_SERVICE_PREFIX, CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
	}

	public static Cloud createCloud(String cloudFolder, String cloudName, String overridesDir)
			throws FileNotFoundException, IOException, DSLException {
		return createCloud(cloudFolder, cloudName, overridesDir, null);
	}

	public static Cloud createCloud(String cloudFolder, String cloudName, String overridesDir, String computeTemplate)
			throws IOException,
			FileNotFoundException, DSLException {

		// Create a temporary directory
		File tmpCloudDir = File.createTempFile(cloudName, "");
		tmpCloudDir.delete();
		tmpCloudDir.mkdir();
		tmpCloudDir.deleteOnExit();
		new File(tmpCloudDir, "upload").mkdir();
		logger.info("Initialize driver using cloud folder at " + tmpCloudDir.getAbsolutePath());

		// Copy azure cloud file into the temporary directory
		FileUtils.copyDirectory(new File(cloudFolder, cloudName), tmpCloudDir);
		FileUtils.copyDirectory(new File(getCredentialsFolder()), tmpCloudDir);
		// FileUtils.deleteQuietly(new File(tmpCloudDir, cloudName + "-cloud.properties"));
		// FileUtils.moveFile(new File(tmpCloudDir, credentialPropertyName), new File(tmpCloudDir, cloudName
		// + "-cloud.properties"));

		// Configure azure-cloud.properties file
		File propertiesFile = new File(tmpCloudDir, cloudName + "-cloud.properties");
		if (propertiesFile.exists() && overridesDir != null) {
			String openstackPropertiesFile = FileUtils.readFileToString(propertiesFile);

			// Create azure-cloud.properties from azure-cloud.properties.sample and override it
			// with the content of the test properties file
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
		Cloud cloud = ServiceReader.readCloudFromDirectory(tmpCloudDir.getAbsolutePath());

		if (computeTemplate != null) {
			copyTemplate(computeTemplate, tmpCloudDir);
			ComputeTemplateHolder holder = retrieveComputeTemplate(tmpCloudDir, computeTemplate);
			if (holder != null) {
				cloud.getCloudCompute().getTemplates().put(holder.getName(), holder.getCloudTemplate());
			}
		}

		return cloud;
	}
}
