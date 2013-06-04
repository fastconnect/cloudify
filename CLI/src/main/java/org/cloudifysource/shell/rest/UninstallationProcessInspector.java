package org.cloudifysource.shell.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 11:41 AM
 */
public abstract class UninstallationProcessInspector {

    private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;
    protected static final int RESOURCE_NOT_FOUND_EXCEPTION_CODE = 404;


    protected RestClient restClient;
    private String deploymentId;
    private boolean verbose;
    private Map<String, Integer> initialNumberOfInstancesPerService;
    private Map<String, Integer> baseLineNumberOfRunningInstancesPerService;

    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();
    private int lastEventIndex = 0;


    protected abstract String getTimeoutErrorMessage();


    public UninstallationProcessInspector(final RestClient restClient,
                                          final String deploymentId,
                                          final boolean verbose,
                                          final Map<String, Integer> initialNumberOfInstancesPerService) {
        //To change body of created methods use File | Settings | File Templates.
        this.restClient = restClient;
        this.deploymentId = deploymentId;
        this.verbose = verbose;
        this.initialNumberOfInstancesPerService = initialNumberOfInstancesPerService;
        this.baseLineNumberOfRunningInstancesPerService = initialNumberOfInstancesPerService;
    }

    /**
     * Gets the latest events of this deployment id. Events are sorted by event index.
     *
     * @return A list of events. If this is the first time events are requested, all events are retrieved.
     * Otherwise, only new events (that were not reported earlier) are retrieved.
     * @throws RestClientException Indicates a failure to get events from the server.
     */
    public List<String> getLatestEvents() throws RestClientException {

        List<String> eventsStrings = new ArrayList<String>();

        DeploymentEvents events = restClient.getDeploymentEvents(deploymentId, lastEventIndex, -1);
        if (events == null || events.getEvents().isEmpty()) {
            return eventsStrings;
        }


        for (DeploymentEvent event : events.getEvents()) {
            eventsStrings.add(event.getDescription());
        }
        lastEventIndex = events.getEvents().get(events.getEvents().size() - 1).getIndex() + 1;
        return eventsStrings;
    }


    public void waitForLifeCycleToEnd(final long timeout) throws InterruptedException, CLIException, TimeoutException {

        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

            private Map<String, Boolean> endedPerService = initWithFalse(initialNumberOfInstancesPerService.keySet());

            private Map<String, Boolean> resourcesMsgPrintedPerService = initWithFalse(initialNumberOfInstancesPerService.keySet());

            private Map<String, Boolean> initWithFalse(final Set<String> serviceNames) {
                Map<String, Boolean> resourcesMsgPrintedPerService = new HashMap<String, Boolean>();
                for (String serviceName : serviceNames) {
                    resourcesMsgPrintedPerService.put(serviceName, false);
                }
                return resourcesMsgPrintedPerService;
            }

            @Override
            public boolean isDone() throws CLIException, InterruptedException {

                for (String serviceName : endedPerService.keySet()) {

                    ServiceDescription description;
                    try {
                        description = getServiceDescription(serviceName);
                    } catch (RestClientException e) {
                        if (e instanceof RestClientResponseException
                                && ((RestClientResponseException)e).getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
                            //if we got here - the service is not installed anymore
                            description = null;
                        } else {
                            throw new CLIException(e.getMessage(), e);
                        }
                    }

                    try {
                        List<String> latestEvents;

                        printUninstalledInstances(description);
                        latestEvents = getLatestEvents();
                        if (latestEvents == null || latestEvents.isEmpty()) {
                            displayer.printNoChange();
                        } else {
                            // If the event "Service undeployed successfully" is found - the server undeploy-thread ended.
                            // This is an "internal" event and should not be printed; it is removed from the list.
                            // Even if undeploy has completed - all lifecycle events should be printed first.
                            if (latestEvents.contains(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY)) {
                                latestEvents.remove(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY);
                                endedPerService.remove(serviceName);
                            }
                            displayer.printEvents(latestEvents);
                        }

                    // If the service' zone is no longer found - USM lifecycle events are not expected anymore.
                    // Print that cloud resources are being released.
                    if (description == null && !resourcesMsgPrintedPerService.get(serviceName)) {
                        displayer.printEvent(ShellUtils.getFormattedMessage("releasing_cloud_resources"));
                        resourcesMsgPrintedPerService.put(serviceName, true);
                    }

                    } catch (final RestClientException e) {
                        throw new CLIException(e.getMessage(), e);
                    }
                }
                return endedPerService.isEmpty();
            }


            private void printUninstalledInstances(final ServiceDescription serviceDescription)
                    throws RestClientException {

                int updatedNumberOfRunningInstances;
                if (serviceDescription != null) {
                    updatedNumberOfRunningInstances = serviceDescription.getInstanceCount();

                    if (baseLineNumberOfRunningInstancesPerService.get(serviceDescription.getServiceName()) > updatedNumberOfRunningInstances) {
                        int numberOfRemovedInstance = initialNumberOfInstancesPerService.get(serviceDescription.getServiceName()) - updatedNumberOfRunningInstances;
                        // another instance was uninstalled
                        displayer.printEvent(
                                "succesfully_uninstalled_instances",
                                numberOfRemovedInstance,
                                initialNumberOfInstancesPerService.get(serviceDescription.getServiceName()),
                                serviceDescription.getServiceName());
                        baseLineNumberOfRunningInstancesPerService.put(serviceDescription.getServiceName(), updatedNumberOfRunningInstances);
                    }
                }
            }

        });



    }

    protected abstract ServiceDescription getServiceDescription(String serviceName) throws RestClientException;

    /**
     * Creates a {@link org.cloudifysource.shell.ConditionLatch} object with the given timeout (in minutes),
     * using a polling interval of 500 ms.
     * @param timeout Timeout, in minutes.
     * @return a configured {@link org.cloudifysource.shell.ConditionLatch} object
     */
    public ConditionLatch createConditionLatch(final long timeout) {
        return new ConditionLatch()
                .verbose(verbose)
                .pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
                .timeout(timeout, TimeUnit.MINUTES)
                .timeoutErrorMessage(getTimeoutErrorMessage());
    }

}
