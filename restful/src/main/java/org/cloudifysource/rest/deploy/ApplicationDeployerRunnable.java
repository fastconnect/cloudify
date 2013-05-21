/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.DeploymentsController;
import org.cloudifysource.rest.controllers.RestErrorException;

import com.j_spaces.kernel.Environment;

/**
 * A Runnable implementation that executes the deployment logic of an application.
 * 
 * @author adaml
 *
 */
public class ApplicationDeployerRunnable implements Runnable{
	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 60;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ApplicationDeployerRunnable.class.getName());

	private final DeploymentsController controller;
	private final InstallApplicationRequest request;
	private final String applicationName;
	private final File overridesFile;
	private final List<Service> services;
	private final DSLApplicationCompilatioResult result;
	private UUID pollingTaskId;

	/**************
	 * Constructor.
	 *
	 * @param controller
	 *            installation requests are delegated to this controller.
	 * @param result
	 *            the application compilation result.
	 * @param applicationName
	 *            the application name.
	 * @param overridesFile
	 *            Application overrides file.
	 * @param authGroups
	 *            Security authorization groups for this application.
	 * @param cloud
	 *            the cloud configuration object.
	 * @param selfHealing
	 *            true if self healing is enabled for all services in this application, false if it is disabled for
	 *            them.
	 * @param cloudOverrides
	 *            cloud configuration overrides for all services in this application.
	 * @param debugAll
	 * @param debugModeString
	 * @param debugEvents
	 * @throws RestErrorException 
	 */
	
	/**
	 * 
	 * @param controller
	 * 		installation requests are delegated to this controller.
	 * @param request
	 * 		the install application request.
	 * @param result
	 * 		the application compilation result.
	 * @param services
	 * 		the list of services.
	 * @param overridesFile
	 * 		application overrides file.
	 * @throws RestErrorException
	 */
	public ApplicationDeployerRunnable(final DeploymentsController controller, 
						final InstallApplicationRequest request, 
						final DSLApplicationCompilatioResult result, 
						final List<Service> services,
						final File overridesFile) throws RestErrorException {
		this.request = request;
		this.controller = controller;
		this.result = result;
		this.services = services;
		this.applicationName = request.getApplicationName();
		this.overridesFile = overridesFile;
	}

	@Override
	public void run() {
		logger.fine("Installing application " + applicationName + " with the following services: " + services);

		final boolean asyncInstallPossible = isAsyncInstallPossibleForApplication();
		logger.info("Async install setting is " + asyncInstallPossible);
		try {
			installServices(asyncInstallPossible);
			
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private void installServices(final boolean async)
			throws IOException {

		final File appDir = result.getApplicationDir();
		
		logger.info("Installing services for application: " + applicationName 
				+ ". Async install: " + async + ". Number of services: " + this.services.size());
		for (final Service service : services) {
			logger.info("Installing service: " + service.getName() + " for application: " + applicationName);
			service.getCustomProperties().put("usmJarPath",
					Environment.getHomeDirectory() + "/lib/platform/usm");

//			final Properties contextProperties = createServiceContextProperties(
//					service, applicationName, async, cloud);

			final String serviceName = service.getName();
			final String absolutePUName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			final File serviceDirectory = new File(appDir, serviceName);

			// scan for service cloud configuration file
			final File cloudConfiguration = new File(serviceDirectory,
					CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);

			boolean found = false;

			try {
				// this will actually create an empty props file.
				final FileAppender appender = new FileAppender("finalPropsFile.properties");
				final LinkedHashMap<File, String> filesToAppend = new LinkedHashMap<File, String>();

				// first add the application properties file. least important overrides.
				// lookup application properties file
				final File applicationPropertiesFile =
						DSLReader.findDefaultDSLFileIfExists(DSLUtils.APPLICATION_PROPERTIES_FILE_NAME, appDir);
				filesToAppend.put(applicationPropertiesFile, "Application Properties File");
				// add the service properties file, second level overrides.
				// lookup service properties file
				final String propertiesFileName = DSLUtils.getPropertiesFileName(serviceDirectory,
						DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
				final File servicePropertiesFile = new File(serviceDirectory, propertiesFileName);
				filesToAppend.put(servicePropertiesFile, "Service Properties File");
				// lookup overrides file
				File actualOverridesFile = overridesFile;
				if (actualOverridesFile == null) {
					// when using the CLI, the application overrides file is inside the directory
					actualOverridesFile =
							DSLReader.findDefaultDSLFileIfExists(DSLUtils.APPLICATION_OVERRIDES_FILE_NAME, appDir);
				}
				// add the overrides file given in the command or via REST, most important overrides.
				filesToAppend.put(actualOverridesFile, "Overrides Properties File");
				/*
				 * name the merged properties file as the original properties file. this will allow all properties to be
				 * available by anyone who parses the default properties file. (like Lifecycle scripts)
				 */
				appender.appendAll(servicePropertiesFile, filesToAppend);

				// Pack the folder and name it absolutePuName
				final File packedFile = Packager.pack(service, serviceDirectory, absolutePUName, null);
				result.getApplicationFile().delete();
				packedFile.deleteOnExit();
				// Deployment will be done using the service's absolute PU name.
//				logger.info("Deploying PU: " + absolutePUName + ". File: "
//						+ packedFile + ". Properties: " + contextProperties);
				final InstallServiceRequest installServiceReq = createInstallServiceRequest(
						cloudConfiguration, applicationPropertiesFile,
						packedFile);
				controller.installService(this.applicationName, serviceName, installServiceReq);
				try {
					FileUtils.deleteDirectory(packedFile.getParentFile());
				} catch (final IOException ioe) {
					// sometimes this delete fails. Not sure why. Maybe deploy
					// is async?
					logger.warning("Failed to delete temporary directory: "
							+ packedFile.getParentFile());
				}

				if (!async) {
					logger.info("Waiting for instance of service: " + serviceName 
							+ " of application: "	+ applicationName);
					final boolean instanceFound = controller
							.waitForServiceInstance(applicationName,
									serviceName,
									SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES,
									TimeUnit.MINUTES);
					if (!instanceFound) {
						throw new TimeoutException(
								"Service "
										+ serviceName
										+ " of application "
										+ applicationName
										+ " was installed, but no instance of the service has started after "
										+ SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES
										+ " minutes.");
					}
					logger.info("Found instance of: " + serviceName);
				}

				found = true;
				logger.fine("service " + service + " deployed.");
			} catch (final Exception e) {
				logger.log(
						Level.SEVERE,
						"Failed to install service: "
								+ serviceName
								+ " of application: "
								+ applicationName
								+ ". Application installation will halt. "
								+ "Some services may already have started, and should be shutdown manually. Error was: "
								+ e.getMessage(), e);
				//TODO:What should I do with this?
//				this.controller.handleDeploymentException(e, this.pollingTaskId);
				return;
			}

			if (!found) {
				logger.severe("Failed to find an instance of service: "
						+ serviceName
						+ " while installing application "
						+ applicationName
						+ ". Application installation will stop. Some services may have been installed!");
				return;
			}

		}
		FileUtils.deleteDirectory(appDir);
	}

	InstallServiceRequest createInstallServiceRequest(
			final File cloudConfiguration,
			final File applicationPropertiesFile, final File packedFile) {
		final InstallServiceRequest installServiceReq = new InstallServiceRequest();
		installServiceReq.setAuthGroups(this.request.getAuthGroups());
		installServiceReq.setDebugAll(this.request.isDebugAll());
		installServiceReq.setDebugEvents(this.request.getDebugEvents());
		installServiceReq.setDebugMode(this.request.getDebugModeString());
		installServiceReq.setSelfHealing(this.request.isSelfHealing());
		installServiceReq.setServiceFileName(packedFile.getName());
		installServiceReq.setTimeoutInMillis(0);
		//TODO:Uncomment this when resolving file transfer issue
//		installServiceReq.setApplicationPropertiesFile(applicationPropertiesFile);
		installServiceReq.setCloudOverridesUploadKey(request.getCloudOverridesUploadKey());
		//TODO:These paramenters should be transfered 
//		installServiceReq.setPackedFile(packedFile);
//		installServiceReq.setCloudConfiguration(cloudConfiguration);
		return installServiceReq;
	}

	/**
	 *
	 * @return true if all services have Lifecycle events.
	 */
	public boolean isAsyncInstallPossibleForApplication() {

		// check if all services are USM
		for (final Service service : this.services) {
			if (service.getLifecycle() == null) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Sets the polling id for this deployment task.
	 *
	 * @param taskPollingId
	 *            polling task id
	 */
	public void setTaskPollingId(final UUID taskPollingId) {
		this.pollingTaskId = taskPollingId;
	}

//	private Properties createServiceContextProperties(final Service service,
//			final String applicationName, final boolean async, final Cloud cloud) {
//		final Properties contextProperties = new Properties();
//
//		if (service.getDependsOn() != null) {
//			String serviceNames = service.getDependsOn().toString();
//			serviceNames = serviceNames.substring(1, serviceNames.length() - 1);
//			if (serviceNames.equals("")) {
//				contextProperties.setProperty(
//						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
//			} else {
//				final String[] splitServiceNames = serviceNames.split(",");
//				final List<String> absoluteServiceNames = new ArrayList<String>();
//				for (final String name : splitServiceNames) {
//					absoluteServiceNames.add(ServiceUtils.getAbsolutePUName(
//							applicationName, name.trim()));
//				}
//				contextProperties.setProperty(
//						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON,
//						Arrays.toString(absoluteServiceNames.toArray()));
//			}
//		}
//		if (service.getType() != null) {
//			contextProperties.setProperty(
//					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE,
//					service.getType());
//		}
//		if (service.getIcon() != null) {
//			contextProperties.setProperty(
//					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
//					CloudifyConstants.SERVICE_EXTERNAL_FOLDER
//							+ service.getIcon());
//		}
//		if (service.getNetwork() != null) {
//			contextProperties
//					.setProperty(
//							CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
//							service.getNetwork().getProtocolDescription());
//		}
//
//		contextProperties.setProperty(
//				CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL,
//				Boolean.toString(async));
//
//		if (cloud != null) {
//			contextProperties.setProperty(
//					CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME,
//					cloud.getName());
//		}
//
//		contextProperties.setProperty(
//				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
//				Boolean.toString(service.isElastic()));
//
//		if (debugAll) {
//			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_ALL, Boolean.TRUE.toString());
//			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, debugModeString);
//		} else if (StringUtils.isNotBlank(debugEvents)) {
//			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_EVENTS, debugEvents);
//			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, debugModeString);
//		}
//
//		return contextProperties;
//	}

}
