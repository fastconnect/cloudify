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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.ServiceInstallationProcessInspector;
import org.cloudifysource.shell.util.NameAndPackedFileResolver;
import org.cloudifysource.shell.util.PreparedPackageResolver;
import org.cloudifysource.shell.util.ServiceResolver;
import org.fusesource.jansi.Ansi.Color;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Installs a service by deploying the service files as one packed file (zip, war or jar). Service files can also
 *        be supplied as a folder containing multiple files.
 * 
 *        Required arguments: service-file - Path to the service's packed file or folder
 * 
 *        Optional arguments: zone - The machines zone in which to install the service name - The name of the service
 *        timeout - The number of minutes to wait until the operation is completed (default: 5 minutes)
 * 
 *        Command syntax: install-service [-zone zone] [-name name] [-timeout timeout] service-file
 */
@Command(scope = "cloudify", name = "install-service", description = "Installs a service. If you specify a folder"
		+ " path it will be packed and deployed. If you specify a service archive, the shell will deploy that file.")
public class InstallService extends AdminAwareCommand {

    private static final long MILLIS_IN_MINUTES = 60 * 1000;

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;
	private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out."
			+ " Configure the timeout using the -timeout flag.";
	private static final long TEN_K = 10 * FileUtils.ONE_KB;

    private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;

    @Argument(required = true, name = "recipe", description = "The service recipe folder or archive")
	private File recipe = null;

	@Option(required = false, name = "-authGroups", description = "The groups authorized to access this application "
			+ "(multiple values can be comma-separated)")
	private String authGroups = null;

	@Option(required = false, name = "-name", description = "The name of the service")
	private String serviceName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is "
			+ "done. Defaults to 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-service-file-name", description = "Name of the service file in the "
			+ "recipe folder. If not specified, uses the default file name")
	private String serviceFileName = null;

	@Option(required = false, name = "-cloudConfiguration", description =
			"File of directory containing configuration information to be used by the cloud driver "
					+ "for this application")
	private File cloudConfiguration;

	@Option(required = false, name = "-disableSelfHealing",
			description = "Disables service self healing")
	private boolean disableSelfHealing = false;

	@Option(required = false, name = "-overrides", description =
			"File containing properties to be used to overrides the current service's properties.")
	private File overrides = null;

	@Option(required = false, name = "-cloud-overrides",
			description = "File containing properties to be used to override the current cloud "
					+ "configuration for this service.")
	private File cloudOverrides = null;

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

    public boolean isDisableSelfHealing() {
        return disableSelfHealing;
    }

    public void setDisableSelfHealing(final boolean disableSelfHealing) {
        this.disableSelfHealing = disableSelfHealing;
    }

    public boolean isDebugAll() {
        return debugAll;
    }

    public void setDebugAll(final boolean debugAll) {
        this.debugAll = debugAll;
    }

    public String getDebugEvents() {
        return debugEvents;
    }

    public void setDebugEvents(final String debugEvents) {
        this.debugEvents = debugEvents;
    }

    public String getDebugModeString() {
        return debugModeString;
    }

    public void setDebugModeString(final String debugModeString) {
        this.debugModeString = debugModeString;
    }

    public String getServiceFileName() {
        return serviceFileName;
    }

    public void setServiceFileName(final String serviceFileName) {
        this.serviceFileName = serviceFileName;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {

        NameAndPackedFileResolver nameAndPackedFileResolver = getResolver(recipe);
        String actualServiceName = serviceName;
        if (actualServiceName == null) {
            // no override name was defined. use the default.
            actualServiceName = nameAndPackedFileResolver.getName();
        }
        File packedFile = nameAndPackedFileResolver.getPackedFile();

        // upload the files if necessary
        final String cloudConfigurationFileKey = uploadToRepo(cloudConfiguration);
        final String cloudOverridesFileKey = uploadToRepo(cloudOverrides);
        final String overridesFileKey = uploadToRepo(overrides);

        final String recipeFileKey = uploadToRepo(packedFile);

        InstallServiceRequest request = new InstallServiceRequest();
        request.setAuthGroups(authGroups);
        request.setCloudConfigurationUploadKey(cloudConfigurationFileKey);
        request.setDebugAll(debugAll);
        request.setCloudOverridesUploadKey(cloudOverridesFileKey);
        request.setDebugEvents(debugEvents);
        request.setServiceOverridesUploadKey(overridesFileKey);
        request.setServiceFolderUploadKey(recipeFileKey);
        request.setServiceFileName(serviceFileName);
        request.setSelfHealing(disableSelfHealing);
        request.setTimeoutInMillis(timeoutInMinutes * MILLIS_IN_MINUTES);

        // execute the request
        InstallServiceResponse installServiceResponse = ((RestAdminFacade) adminFacade)
                .installService(CloudifyConstants.DEFAULT_APPLICATION_NAME, actualServiceName, request);

        ServiceInstallationProcessInspector inspector = new ServiceInstallationProcessInspector(
                ((RestAdminFacade) adminFacade).getNewRestClient(),
                installServiceResponse.getDeploymentID(),
                CloudifyConstants.DEFAULT_APPLICATION_NAME, actualServiceName);

        int actualTimeout = timeoutInMinutes;
        boolean isDone = false;

        displayer.printEvent("Waiting for life cycle events for service " + actualServiceName);
        while (!isDone) {
            try {

                waitForLifeCycleToEnd(actualTimeout, inspector);
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
                    throw new CLIStatusException(e,
                            "service_installation_timed_out_on_client",
                            actualServiceName);
                }
            }
        }

        // drop one line before printing the last message
        displayer.printEvent("");
        return getFormattedMessage("service_install_ended", Color.GREEN, actualServiceName);
    }


    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session,
                "would_you_like_to_continue_service_installation", serviceName);
    }

    private void waitForLifeCycleToEnd(
            final long timeout,
            final ServiceInstallationProcessInspector inspector)
            throws InterruptedException, CLIException, TimeoutException {

        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

            @Override
            public boolean isDone() throws CLIException, InterruptedException {
                try {
                    boolean ended = inspector.lifeCycleEnded();
                    if (!ended) {

                        List<String> latestEvents = inspector.getLatestEvents();
                        if (latestEvents != null) {
                            displayer.printEvents(latestEvents);
                        } else {
                            displayer.printNoChange();
                        }
                    }
                    return ended;
                } catch (final RestClientException e) {
                    throw new CLIException(e.getMessage(), e);
                }
            }
        });

    }

    private ConditionLatch createConditionLatch(final long timeout) {
        return new ConditionLatch()
                .verbose(verbose)
                .pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
                .timeout(timeout, TimeUnit.MINUTES)
                .timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE);
    }

    private NameAndPackedFileResolver getResolver(final File recipe) throws CLIStatusException {
        if (recipe.isFile()) {
            // this is a prepared package we can just use.
            return new PreparedPackageResolver(recipe);
        } else {
            // this is an actual service directory
            return new ServiceResolver(resolve(recipe), overrides, serviceFileName);
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

    private File resolve(final File recipe) throws CLIStatusException {
        final RecipePathResolver pathResolver = new RecipePathResolver();
        if (pathResolver.resolveService(recipe)) {
            return pathResolver.getResolved();
        } else {
            throw new CLIStatusException("service_file_doesnt_exist",
                    StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
        }
    }
}
