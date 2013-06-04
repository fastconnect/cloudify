/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.NewApplicationUninstallationProcessInspector;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.fusesource.jansi.Ansi.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author rafi, adaml, barakm, noak
 * @since 2.0.0
 * 
 *        Uninstalls an application.
 * 
 *        Required arguments: applicationName - The name of the application
 * 
 *        Optional arguments: timeout - The number of minutes to wait until the
 *        operation is completed (default: 5).
 * 
 *        Command syntax: uninstall-application [-timeout timeout]
 *        applicationName
 * 
 */
@Command(scope = "cloudify", name = "uninstall-application", description = "Uninstalls an application.")
public class UninstallApplication extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;
	private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	@Argument(index = 0, required = true, name = "The name of the application")
	private String applicationName;

	/**
	 * Gets all deployed applications' names.
	 * 
	 * @return Collection of applications' names.
	 */
	@CompleterValues(index = 0)
	public Collection<String> getCompleterValues() {
		try {
			return getRestAdminFacade().getApplicationNamesList();
		} catch (final CLIException e) {
			return new ArrayList<String>();
		}
	}

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done. Defaults to 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {

		final RestClient restClient = ((RestAdminFacade) adminFacade).getNewRestClient();

		if (!askUninstallConfirmationQuestion()) {
			return getFormattedMessage("uninstall_aborted");
		}

		if (CloudifyConstants.MANAGEMENT_APPLICATION_NAME.equalsIgnoreCase(applicationName)) {
			throw new CLIStatusException("cannot_uninstall_management_application");
		}

        ApplicationDescription applicationDescription = restClient.getApplicationDescription(applicationName);
        Map<String, Integer> currentNumberOfRunningInstancesPerService = new HashMap<String, Integer>();
        for (ServiceDescription serviceDescription : applicationDescription.getServicesDescription()) {
            currentNumberOfRunningInstancesPerService.put(serviceDescription.getServiceName(), serviceDescription.getInstanceCount());
        }

        final String deploymentId = applicationDescription.getServicesDescription().get(0).getDeploymentId();

        final int nextEventId = getNextEventId(restClient, deploymentId);

        NewApplicationUninstallationProcessInspector inspector =
                new NewApplicationUninstallationProcessInspector(
                        restClient, deploymentId, verbose, currentNumberOfRunningInstancesPerService, applicationName,
                        nextEventId);
        inspector.setServiceDescriptionList(applicationDescription.getServicesDescription());

		
		restClient.uninstallApplication(
				applicationName, timeoutInMinutes);

		
		// start polling for life cycle events
        boolean isDone = false;
        displayer.printEvent("Waiting for life cycle events for application " + applicationName);

        while (!isDone) {
            try {
                inspector.waitForLifeCycleToEnd(timeoutInMinutes);
                isDone = true;
            } catch (final TimeoutException e) {
                // if non interactive, throw exception
                if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
                    throw new CLIException(e.getMessage(), e);
                }

                // ask the user whether to continue viewing the installation or to stop
                displayer.printEvent("");
                boolean continueViewing = promptWouldYouLikeToContinueQuestion();
                if (continueViewing) {
                    // prolong the polling timeouts
                	timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;
                } else {
                    throw new CLIStatusException(e, "application_uninstallation_timed_out_on_client", applicationName);
                }
            }
        }

		session.put(Constants.ACTIVE_APP, "default");
		GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("application_uninstalled_successfully", Color.GREEN, this.applicationName);
	}

	// returns true if the answer to the question was 'Yes'.
	/**
	 * Asks the user for confirmation to uninstall the application.
	 * 
	 * @return true if the user confirmed, false otherwise
	 * @throws IOException
	 *             Reporting a failure to get the user's confirmation
	 */
	private boolean askUninstallConfirmationQuestion() throws IOException {
		return ShellUtils.promptUser(session, "application_uninstall_confirmation", applicationName);
	}
	
    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_application_uninstallation", applicationName);
    }

    private int getNextEventId(final RestClient client, final String deploymentId) throws RestClientException {
        int lastEventId = 0;
        final DeploymentEvents lastDeploymentEvents = client.getLastEvent(deploymentId);
        if (!lastDeploymentEvents.getEvents().isEmpty()) {
            lastEventId = lastDeploymentEvents.getEvents().iterator().next().getIndex();
        }
        return lastEventId+1;
    }

}
