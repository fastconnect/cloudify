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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.ServiceInstallationProcessInspector;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 *
 *        Uninstalls a service. Required arguments: service-name The name of the service to uninstall.
 *
 *        Optional arguments: timeout - The number of minutes to wait until the operation is completed (default: 5
 *        minutes) progress - The polling time interval in seconds, used for checking if the operation is completed
 *        (default: 5 seconds)
 *
 *        Command syntax: uninstall-service [-timeout timeout] [-progress progress] service-name
 */
@Command(scope = "cloudify", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

	private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;
	private static final int DEFAULT_TIMEOUT_MINUTES = 5;
    private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
            + "Try to increase the timeout using the -timeout flag";
    
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    @Argument(index = 0, required = true, name = "service-name")
    private String serviceName;

    /**
     * Gets all services installed on the default application.
     *
     * @return a collection of services' names
     */
    @CompleterValues(index = 0)
    public Collection<String> getServiceList() {
        try {
            return getRestAdminFacade().getServicesList(CloudifyConstants.DEFAULT_APPLICATION_NAME);
        } catch (final Exception e) {
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
    protected Object doExecute()
            throws Exception {

        if (!askUninstallConfirmationQuestion()) {
            return getFormattedMessage("uninstall_aborted");
        }

        //TODO noak: use RestAdminFacade instead of directly calling the RestClient
        UninstallServiceResponse uninstallServiceResponse = 
        		((RestAdminFacade) adminFacade).uninstallService(CloudifyConstants.DEFAULT_APPLICATION_NAME, 
        				serviceName, timeoutInMinutes);
        
        // poll for life cycle events
        final String undeploymentId = uninstallServiceResponse.getDeploymentID();

        // start polling for life cycle events
        ServiceInstallationProcessInspector inspector = new ServiceInstallationProcessInspector(
                ((RestAdminFacade) adminFacade).getNewRestClient(), undeploymentId, 
                CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName);
        
        boolean isDone = false;
        displayer.printEvent("Waiting for life cycle events for service " + serviceName);
        while (!isDone) {
            try {
                waitForLifeCycleToEnd(timeoutInMinutes, inspector);
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
                	timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;
                } else {
                    throw new CLIStatusException(e, "service_installation_timed_out_on_client", serviceName);
                }
            }
        }

        // drop one line before printing the last message
        displayer.printEvent("");

        return getFormattedMessage("undeployed_successfully", Color.GREEN, serviceName);
    }
    
    private void waitForLifeCycleToEnd(
            final long timeout,
            final ServiceInstallationProcessInspector inspector)
            throws InterruptedException, CLIException, TimeoutException {

        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {
        	
        	private boolean resourcesMsgPrinted = false;

            @Override
            public boolean isDone() throws CLIException, InterruptedException {
                try {
                	boolean ended = false;

                	List<String> latestEvents;
                	latestEvents = inspector.getLatestEvents();
                	if (latestEvents == null || latestEvents.isEmpty()) {
                		displayer.printNoChange();
                	} else {
                		// If the event "Service undeployed successfully" is found - the server undeploy-thread ended.
                		// This is an "internal" event and should not be printed; it is removed from the list.
                		// Even if undeploy has completed - all lifecycle events should be printed first.
                		if (latestEvents.contains(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY)) {
                			latestEvents.remove(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY);
                			ended = true;
                		}
                		displayer.printEvents(latestEvents);
                    }
                	
                	// If the service' zone is no longer found - USM lifecycle events are not expected anymore.
                	// Print that cloud resources are being released.
                	if (!resourcesMsgPrinted && inspector.isServiceInState(DeploymentState.UNINSTALLED)) {
                    	displayer.printEvent(getFormattedMessage("releasing_cloud_resources"));
                    	resourcesMsgPrinted = true;
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

    /**
     * Asks the user for confirmation to uninstall the service.
     *
     * @return true if the user confirmed, false otherwise
     * @throws java.io.IOException Reporting a failure to get the user's confirmation
     */
    // returns true if the answer to the question was 'Yes'.
    private boolean askUninstallConfirmationQuestion()
            throws IOException {
       return ShellUtils.promptUser(session, "service_uninstall_confirmation", serviceName);
    }

    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_service_installation", serviceName);
    }
}
