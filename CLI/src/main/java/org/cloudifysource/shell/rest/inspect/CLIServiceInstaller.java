package org.cloudifysource.shell.rest.inspect;

import org.apache.felix.service.command.CommandSession;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.inspect.service.ServiceInstallationProcessInspector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/5/13
 * Time: 12:40 AM
 */
public class CLIServiceInstaller {

    private static final int DEFAULT_TIMEOUT_MINUTES = 5
            ;
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private boolean askOnTimeout = true;
    private String applicationName;
    private String serviceName;
    private RestAdminFacade restAdminFacade;
    private int plannedNumberOfInstances;
    private int initialTimeout;
    private CommandSession session;
    private String deploymentId;

    public void setAskOnTimeout(final boolean askOnTimeout) {
        this.askOnTimeout = askOnTimeout;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setServiceName(final String serviceName) {
        this.serviceName = serviceName;
    }

    public void setRestAdminFacade(final RestAdminFacade restAdminFacade) {
        this.restAdminFacade = restAdminFacade;
    }

    public void setPlannedNumberOfInstances(final int plannedNumberOfInstances) {
        this.plannedNumberOfInstances = plannedNumberOfInstances;
    }

    public void setInitialTimeout(final int initialTimeout) {
        this.initialTimeout = initialTimeout;
    }

    public void setSession(final CommandSession session) {
        this.session = session;
    }

    public void setDeploymentId(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public void install() throws RestClientException, CLIException, InterruptedException, IOException {

        ServiceInstallationProcessInspector inspector = new ServiceInstallationProcessInspector(
                restAdminFacade.getNewRestClient(),
                deploymentId,
                false,
                serviceName,
                plannedNumberOfInstances,
                applicationName);

        int actualTimeout = initialTimeout;
        boolean isDone = false;
        displayer.printEvent("installing_service", serviceName, plannedNumberOfInstances);
        displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);
        while (!isDone) {
            try {

                inspector.waitForLifeCycleToEnd(actualTimeout);
                isDone = true;

            } catch (final TimeoutException e) {

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
                    throw new CLIStatusException(e,
                            "service_installation_timed_out_on_client",
                            serviceName);
                }
            }
        }
        // drop one line before printing the last message
        displayer.printEvent("");
    }

    private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
        return ShellUtils.promptUser(session,
                "would_you_like_to_continue_service_installation", serviceName);
    }
}
