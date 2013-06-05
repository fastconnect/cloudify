package org.cloudifysource.shell.rest.inspect;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.inspect.application.ApplicationUninstallationProcessInspector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/5/13
 * Time: 1:17 AM
 */
public class CLIApplicationUninstaller {

    private static final int DEFAULT_TIMEOUT_MINUTES = 15
            ;
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private boolean askOnTimeout = true;
    private String applicationName;
    private RestAdminFacade restAdminFacade;
    private int initialTimeout;
    private CommandSession session;

    public void setAskOnTimeout(final boolean askOnTimeout) {
        this.askOnTimeout = askOnTimeout;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setRestAdminFacade(final RestAdminFacade restAdminFacade) {
        this.restAdminFacade = restAdminFacade;
    }

    public void setInitialTimeout(final int initialTimeout) {
        this.initialTimeout = initialTimeout;
    }

    public void setSession(final CommandSession session) {
        this.session = session;
    }

    public void uninstall() throws RestClientException, CLIException, InterruptedException, IOException {

        ApplicationDescription applicationDescription = restAdminFacade.getNewRestClient().getApplicationDescription(applicationName);
        Map<String, Integer> currentNumberOfRunningInstancesPerService = new HashMap<String, Integer>();
        for (ServiceDescription serviceDescription : applicationDescription.getServicesDescription()) {
            currentNumberOfRunningInstancesPerService.put(serviceDescription.getServiceName(), serviceDescription.getInstanceCount());
        }

        final String deploymentId = applicationDescription.getServicesDescription().get(0).getDeploymentId();

        final int nextEventId = getNextEventId(restAdminFacade.getNewRestClient(), deploymentId);

        ApplicationUninstallationProcessInspector inspector =
                new ApplicationUninstallationProcessInspector(
                        restAdminFacade.getNewRestClient(),
                        deploymentId,
                        false,
                        currentNumberOfRunningInstancesPerService,
                        applicationName,
                        nextEventId);
        inspector.setServiceDescriptionList(applicationDescription.getServicesDescription());


        restAdminFacade.getNewRestClient().uninstallApplication(
                applicationName, initialTimeout);


        // start polling for life cycle events
        boolean isDone = false;
        displayer.printEvent("uninstalling_application", applicationName);
        displayer.printEvent("waiting_for_lifecycle_of_application", applicationName);


        int actualTimeout = initialTimeout;
        while (!isDone) {
            try {
                inspector.waitForLifeCycleToEnd(actualTimeout);
                isDone = true;
            } catch (final TimeoutException e) {
                // if non interactive, throw exception
                if (!askOnTimeout || !(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
                    throw new CLIException(e.getMessage(), e);
                }

                // ask the user whether to continue viewing the installation or to stop
                displayer.printEvent("");
                boolean continueViewing = promptWouldYouLikeToContinueQuestion();
                if (continueViewing) {
                    // prolong the polling timeouts
                    actualTimeout = DEFAULT_TIMEOUT_MINUTES;
                } else {
                    throw new CLIStatusException(e, "application_uninstallation_timed_out_on_client", applicationName);
                }
            }
        }
        // drop one line before printing the last message
        displayer.printEvent("");
    }

    private int getNextEventId(final RestClient client, final String deploymentId) throws RestClientException {
        int lastEventId = 0;
        final DeploymentEvents lastDeploymentEvents = client.getLastEvent(deploymentId);
        if (!lastDeploymentEvents.getEvents().isEmpty()) {
            lastEventId = lastDeploymentEvents.getEvents().iterator().next().getIndex();
        }
        return lastEventId+1;
    }

    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session, "would_you_like_to_continue_application_uninstallation", applicationName);
    }

    public String getApplicationName() {
        return applicationName;
    }
}
