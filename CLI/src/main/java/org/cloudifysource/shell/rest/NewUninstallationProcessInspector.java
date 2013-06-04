package org.cloudifysource.shell.rest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 7:51 PM
 */
public abstract class NewUninstallationProcessInspector extends InstallationProcessInspector {

    private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    private boolean waitForCloudResourcesRelease = true;

    public NewUninstallationProcessInspector(final RestClient restClient,
                                             final String deploymentId,
                                             final boolean verbose,
                                             final Map<String, Integer> plannedNumberOfInstancesPerService,
                                             final Map<String, Integer> currentRunningInstancesPerService) {
        super(restClient,
              deploymentId,
              verbose,
              plannedNumberOfInstancesPerService,
              currentRunningInstancesPerService);
    }

    @Override
    public void waitForLifeCycleToEnd(long timeout) throws InterruptedException, CLIException, TimeoutException {


        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

            boolean lifeCycleEnded = false;
            boolean undeployEnded = false;

            @Override
            public boolean isDone() throws CLIException, InterruptedException {
                try {
                    List<String> latestEvents;
                    printUnInstalledInstances();
                    boolean ended = false;
                    if (!lifeCycleEnded) {
                        lifeCycleEnded = lifeCycleEnded();
                        latestEvents = getLatestEvents();
                        if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                            undeployEnded = true;
                        }
                        if (!latestEvents.isEmpty()) {
                            displayer.printEvents(latestEvents);
                        } else {
                            if (!lifeCycleEnded) {
                                displayer.printNoChange();
                            }
                        }
                        if (lifeCycleEnded && waitForCloudResourcesRelease) {
                            displayer.printEvent("releasing cloud resources...");
                        }
                    }

                    if (lifeCycleEnded) {
                    	if (waitForCloudResourcesRelease) {
	                        // wait for cloud resources
	                        latestEvents = getLatestEvents();

                            undeployEnded = false;
                            if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                                undeployEnded = true;
                            }
                            ended = lifeCycleEnded && undeployEnded;
                    	} else {
                    		ended = true;
                    	}
                    }

                    return ended;
                } catch (final RestClientException e) {
                    throw new CLIException(e.getMessage(), e, e.getVerbose());
                }
            }

            private void printUnInstalledInstances() throws RestClientException {
                for (Map.Entry<String, Integer> entry : plannedNumberOfInstancesPerService.entrySet()) {
                    int runningInstances = getNumberOfRunningInstances(entry.getKey());
                    if (runningInstances < currentRunningInstancesPerService.get(entry.getKey())) {
                        // a new instance is now running
                        displayer.printEvent("succesfully uninstalled 1 instance for service " + entry.getKey());
                        currentRunningInstancesPerService.put(entry.getKey(), runningInstances);
                    }
                }
            }
        });


    }

	public boolean isWaitForCloudResourcesRelease() {
		return waitForCloudResourcesRelease;
	}

	public void setWaitForCloudResourcesRelease(boolean waitForCloudResourcesRelease) {
		this.waitForCloudResourcesRelease = waitForCloudResourcesRelease;
	}
}
