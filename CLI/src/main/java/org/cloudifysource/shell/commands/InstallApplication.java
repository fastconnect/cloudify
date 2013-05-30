/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.ApplicationInstallationProcessInspector;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.util.ApplicationResolver;
import org.cloudifysource.shell.util.NameAndPackedFileResolver;
import org.cloudifysource.shell.util.PreparedApplicationPackageResolver;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm, adaml
 * @since 2.0.0
 *
 *        Installs an application, including it's contained services ordered according to their dependencies.
 *
 *        Required arguments: application-file - The application recipe file path, folder or archive (zip/jar)
 *
 *        Optional arguments: name - The name of the application timeout - The number of minutes to wait until the
 *        operation is completed (default: 10 minutes)
 *
 *        Command syntax: install-application [-name name] [-timeout timeout] application-file
 */
@Command(scope = "cloudify", name = "install-application", description = "Installs an application. If you specify"
		+ " a folder path it will be packed and deployed. If you sepcify an application archive, the shell will deploy"
		+ " that file.")
public class InstallApplication extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 10;

	private static final long TEN_K = 10 * FileUtils.ONE_KB;

	@Argument(required = true, name = "application-file", description = "The application recipe file path, folder "
			+ "or archive")
	private File applicationFile;

	@Option(required = false, name = "-authGroups", description = "The groups authorized to access this application "
			+ "(multiple values can be comma-separated)")
	private String authGroups;

	@Option(required = false, name = "-name", description = "The name of the application")
	private String applicationName;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation"
			+ " is done.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-disableSelfHealing",
			description = "Disables service self healing")
	private boolean disableSelfHealing = false;

	@Option(required = false, name = "-cloudConfiguration",
			description = "File or directory containing configuration information to be used by the cloud driver "
					+ "for this application")
	private File cloudConfiguration;

	@Option(required = false, name = "-overrides",
			description = "File containing properties to be used to override the current "
					+ "propeties of the application and its services")
	private File overrides;

	@Option(required = false, name = "-cloud-overrides",
			description = "File containing properties to be used to override the current cloud "
					+ "configuration for this application and its services.")
	private File cloudOverrides;

	@Option(required = false, name = "-debug-all",
			description = "Debug all supported lifecycle events")
	private boolean debugAll;

	@Option(required = false, name = "-debug-events",
			description = "Debug the specified events")
	private String debugEvents;

	@Option(required = false, name = "-debug-mode",
			description = "Debug mode. One of: instead, after or onError")
	private String debugModeString = DebugModes.INSTEAD.getName();

	private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	public File getCloudConfiguration() {
		return cloudConfiguration;
	}

	public void setCloudConfiguration(final File cloudConfiguration) {
		this.cloudConfiguration = cloudConfiguration;
	}

	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	public String getDebugModeString() {
		return debugModeString;
	}

	public void setDebugModeString(final String debugModeString) {
		this.debugModeString = debugModeString;
	}

	public boolean isDisableSelfHealing() {
		return disableSelfHealing;
	}

	public void setDisableSelfHealing(final boolean disableSelfHealing) {
		this.disableSelfHealing = disableSelfHealing;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("boxing")
	@Override
	protected Object doExecute()
			throws Exception {

		//resolve the path for the given app input
		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveApplication(applicationFile)) {
			applicationFile = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("application_not_found",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}
		//resolve packed file and application name
		final NameAndPackedFileResolver nameAndPackedFileResolver = getResolver(applicationFile);
		if (StringUtils.isBlank(applicationName)) {
			applicationName = nameAndPackedFileResolver.getName();
		}
		
		final File packedFile = nameAndPackedFileResolver.getPackedFile();
		final File cloudConfigurationFile = createCloudConfigurationZipFile();
		//upload relevant application deployment files 
		final String packedFileKey = uploadToRepo(packedFile);
		final String overridesFileKey = uploadToRepo(this.overrides);
		final String cloudOverridesFileKey = uploadToRepo(this.cloudOverrides);
		final String cloudConfigurationFileKey = uploadToRepo(cloudConfigurationFile);
		
		//create the install request
		InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(packedFileKey);
		request.setApplicationOverridesUploadKey(overridesFileKey);
		request.setCloudOverridesUploadKey(cloudOverridesFileKey);
		request.setCloudConfigurationUploadKey(cloudConfigurationFileKey);
		request.setApplicationName(applicationName);
		request.setAuthGroups(this.authGroups);
		request.setDebugAll(this.debugAll);
		request.setDebugEvents(this.debugEvents);
		request.setDebugMode(this.debugModeString);
		request.setSelfHealing(this.disableSelfHealing);
		request.setTimeoutInMillis(TimeUnit.MINUTES.toMillis(timeoutInMinutes));
		
		//install application
		final InstallApplicationResponse installApplicationResponse = 
				((RestAdminFacade) adminFacade).installApplication(applicationName, request);
		
        Map<String, Integer> plannedNumberOfInstancesPerService = nameAndPackedFileResolver
                .getPlannedNumberOfInstancesPerService();
        ApplicationInstallationProcessInspector inspector = new ApplicationInstallationProcessInspector(
        		((RestAdminFacade) adminFacade).getNewRestClient(), 
        		installApplicationResponse.getDeploymentID(), 
        		applicationName,
        		verbose,
        		plannedNumberOfInstancesPerService);	
        		
        Application application = ((Application) nameAndPackedFileResolver.getDSLObject());
		//print application info.
		printApplicationInfo(application);
        int actualTimeout = timeoutInMinutes;
        boolean isDone = false;
        displayer.printEvent("installing_application", applicationName);
        displayer.printEvent("waiting_for_lifecycle_of_application", applicationName);
        while (!isDone) {
            try {

                inspector.waitForLifeCycleToEnd(actualTimeout);
                isDone = true;

            } catch (final TimeoutException e) {

                // if non interactive, throw exception
                if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
                    throw new CLIException(e.getMessage(), e);
                }

                // ask user if he want to continue viewing the installation.
                displayer.printEvent("");
                boolean continueViewing = promptWouldYouLikeToContinueQuestion();
                if (continueViewing) {
                    // prolong the polling timeouts
                    actualTimeout = DEFAULT_TIMEOUT_MINUTES;
                } else {
                	throw new CLIStatusException(e, "application_installation_timed_out_on_client",
							applicationName);
                }
            }
        }

		if (!applicationFile.isFile()) {
			final boolean delete = packedFile.delete();
			if (!delete) {
				logger.info("Failed to delete application file: " + packedFile.getAbsolutePath());
			}
		}

		//set the active application in the CLI.
		session.put(Constants.ACTIVE_APP, applicationName);
		GigaShellMain.getInstance().setCurrentApplicationName(applicationName);
		// drop one line before printing the last message
        displayer.printEvent("");
		return this.getFormattedMessage("application_installed_successfully", Color.GREEN, applicationName);
	}

	private NameAndPackedFileResolver getResolver(final File applicationFile) 
			throws CLIStatusException {
		// this is a prepared package we can just use.
		if (applicationFile.isFile()) {
			if (applicationFile.getName().endsWith("zip") || applicationFile.getName().endsWith("jar")) {
				return new PreparedApplicationPackageResolver(applicationFile, overrides);
			} 
			throw new CLIStatusException("application_file_format_mismatch", applicationFile.getPath()); 
		} else {
			// this is an actual application directory
			return new ApplicationResolver(applicationFile);
		}
	}

    private String uploadToRepo(final File file) throws RestClientException, CLIException {
        if (file != null) {
            if (!file.isFile()) {
                throw new CLIException(file.getAbsolutePath() + " is not a file or is missing");
            } else {
                displayer.printEvent("Uploading " + file.getAbsolutePath());
                return ((RestAdminFacade) adminFacade).upload(null, file).getUploadKey();
            }
        }
        return null;
    }

	private boolean promptWouldYouLikeToContinueQuestion()
			throws IOException {
		return ShellUtils.promptUser(session, "would_you_like_to_continue_application_installation",
				this.applicationName);
	}

	/**
	 * Prints Application data - the application name and it's services name, dependencies and number of instances.
	 *
	 * @param application
	 *            Application object to analyze
	 */
	private void printApplicationInfo(final Application application) {
		final List<Service> services = application.getServices();
		logger.info("Application [" + applicationName + "] with " + services.size() + " services");
		for (final Service service : services) {
			if (service.getDependsOn().isEmpty()) {
				logger.info("Service [" + service.getName() + "] " + service.getNumInstances() + " planned instances");
			} else { // Service has dependencies
				logger.info("Service [" + service.getName() + "] depends on " + service.getDependsOn().toString()
						+ " " + service.getNumInstances() + " planned instances");
			}
		}
	}
	
	private File createCloudConfigurationZipFile()
			throws CLIStatusException, IOException {
		if (this.cloudConfiguration == null) {
			return null;
		}

		if (!this.cloudConfiguration.exists()) {
			throw new CLIStatusException("cloud_configuration_file_not_found",
					this.cloudConfiguration.getAbsolutePath());
		}

		// create a temp file in a temp directory
		final File tempDir = File.createTempFile("__Cloudify_Cloud_configuration", ".tmp");
		FileUtils.forceDelete(tempDir);
		final boolean mkdirs = tempDir.mkdirs();
		if (!mkdirs) {
			logger.info("Field to create temporary directory " + tempDir.getAbsolutePath());
		}
		final File tempFile = new File(tempDir, CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);
		logger.info("Created temporary file " + tempFile.getAbsolutePath()
				+ " in temporary directory" + tempDir.getAbsolutePath());

		// mark files for deletion on JVM exit
		tempFile.deleteOnExit();
		tempDir.deleteOnExit();

		if (this.cloudConfiguration.isDirectory()) {
			ZipUtils.zip(this.cloudConfiguration, tempFile);
		} else if (this.cloudConfiguration.isFile()) {
			ZipUtils.zipSingleFile(this.cloudConfiguration, tempFile);
		} else {
			throw new IOException(this.cloudConfiguration + " is neither a file nor a directory");
		}

		return tempFile;
	}
}
