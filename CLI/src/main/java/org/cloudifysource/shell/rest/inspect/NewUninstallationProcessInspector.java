package org.cloudifysource.shell.rest.inspect;

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
                    boolean ended = false;
                    if (!lifeCycleEnded) {
                        lifeCycleEnded = lifeCycleEnded();
                        latestEvents = getLatestEvents();
                        if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                            undeployEnded = true;
                        }
                        if (!latestEvents.isEmpty()) {
                            if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                                ended = true;
                            }
                            displayer.printEvents(latestEvents);
                        } else {
                            if (!lifeCycleEnded) {
                                displayer.printNoChange();
                            }
                        }
                        printUnInstalledInstances();
                        if (lifeCycleEnded && waitForCloudResourcesRelease) {
                            displayer.printEvent("releasing cloud resources...");
                            return ended;
                        }
                    }

                    if (lifeCycleEnded) {
                    	if (waitForCloudResourcesRelease) {
	                        // wait for cloud resources
	                        latestEvents = getLatestEvents();

                            undeployEnded = false;
                            if (latestEvents.contains(CloudifyConstants.UNDEPLOYED_SUCCESSFULLY_EVENT)) {
                                undeployEnded = true;
                            } else {
                                displayer.printNoChange();
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
                    Integer current = currentRunningInstancesPerService.get(entry.getKey());
                    if (runningInstances < current) {
                        // a new instance is now running
                        displayer.printEvent("Installed " + runningInstances + " planned " + entry.getValue());
                        currentRunningInstancesPerService.put(entry.getKey(), runningInstances);
                    }
                }
            }
        });


    }

	public void setWaitForCloudResourcesRelease(boolean waitForCloudResourcesRelease) {
		this.waitForCloudResourcesRelease = waitForCloudResourcesRelease;
	}
}
