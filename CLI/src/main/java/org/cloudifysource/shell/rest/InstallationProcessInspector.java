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
package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/29/13
 * Time: 1:50 PM
 * <br></br>
 *
 * Provides functionality for inspecting the installation process of services/application.
 *
 */
public abstract class InstallationProcessInspector {

    private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;

    protected RestClient restClient;
    private boolean verbose;
    private String deploymentId;
    private Map<String, Integer> plannedNumberOfInstancesPerService;

    private int lastEventIndex = 0;

    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    public InstallationProcessInspector(final RestClient restClient,
                                        final String deploymentId,
                                        final boolean verbose,
                                        final Map<String, Integer> plannedNumberOfInstancesPerService) {
        this.restClient = restClient;
        this.deploymentId = deploymentId;
        this.verbose = verbose;
        this.plannedNumberOfInstancesPerService = plannedNumberOfInstancesPerService;
    }

    /**
     * Waits until the application/service lifecycle ends.
     * As long as the installation continues, it will print out the most recent events not yet printed.
     * @param timeout the timeout.
     * @throws InterruptedException Thrown in case the thread was interrupted.
     * @throws CLIException Thrown in case an error happened while trying to retrieve events.
     * @throws TimeoutException Thrown in case the timeout is reached.
     */
    public void waitForLifeCycleToEnd(final long timeout) throws InterruptedException, CLIException, TimeoutException {
        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

            private Map<String, Integer> currentRunningInstancesPerService =
                    initWithZeros(plannedNumberOfInstancesPerService.keySet());

            @Override
            public boolean isDone() throws CLIException, InterruptedException {
                try {
                    printInstalledInstances();
                    boolean ended = lifeCycleEnded();
                    if (!ended) {
                        List<String> latestEvents = getLatestEvents();
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

            private void printInstalledInstances() throws RestClientException {
                for (Map.Entry<String, Integer> entry : plannedNumberOfInstancesPerService.entrySet()) {
                    int runningInstances = getNumberOfRunningInstances(entry.getKey());
                    if (runningInstances > currentRunningInstancesPerService.get(entry.getKey())) {
                        // a new instance is now running
                        displayer.printEvent("succesfully_installed_instances", runningInstances,
                                entry.getValue(), entry.getKey());
                        currentRunningInstancesPerService.put(entry.getKey(), runningInstances);
                    }
                }
            }

            private Map<String, Integer> initWithZeros(final Set<String> serviceNames) {
                Map<String, Integer> currentRunningInstancesPerService = new HashMap<String, Integer>();
                for (String service : serviceNames) {
                    currentRunningInstancesPerService.put(service, 0);
                }
                return currentRunningInstancesPerService;
            }
        });
    }

    /**
     * Determines whether or not the life cycle for this installation has ended.
     * @return true if the service/application are fully running.
     * @throws RestClientException Thrown in case an error happened during a rest call.
     */
    public abstract boolean lifeCycleEnded() throws RestClientException;

    /**
     * Query the number of running instances for a particular service.
     * @param serviceName The service name.
     * @return how many instances are in running state.
     * @throws RestClientException Thrown in case an error happened during a rest call.
     */
    public abstract int getNumberOfRunningInstances(final String serviceName) throws RestClientException;

    /**
     *
     * @return the error message presented upon timeout.
     */
    public abstract String getTimeoutErrorMessage();

    private List<String> getLatestEvents() throws RestClientException {

        List<String> eventsStrings = new ArrayList<String>();

        DeploymentEvents events = restClient.getDeploymentEvents(deploymentId, lastEventIndex, -1);
        if (events == null || events.getEvents().isEmpty()) {
            return null;
        }
        Set<Integer> eventIndices = events.getEvents().keySet();

        Integer[] integers = eventIndices.toArray(new Integer[eventIndices.size()]);

        // sort by event index (corresponds to order of events on the server, pretty much)
        Arrays.sort(integers);

        for (Integer index : integers) {
            eventsStrings.add(events.getEvents().get(index).getDescription());
        }
        lastEventIndex = integers[integers.length - 1] + 1;
        return eventsStrings;
    }

    private ConditionLatch createConditionLatch(final long timeout) {
        return new ConditionLatch()
                .verbose(verbose)
                .pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
                .timeout(timeout, TimeUnit.MINUTES)
                .timeoutErrorMessage(getTimeoutErrorMessage());
    }
}
