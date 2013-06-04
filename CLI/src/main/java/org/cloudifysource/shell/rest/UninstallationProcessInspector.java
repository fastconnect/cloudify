package org.cloudifysource.shell.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.ServiceDescription;
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
 * @author elip, noak
 * @since 2.6.0
 */
public abstract class UninstallationProcessInspector {

    private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;
    protected static final int RESOURCE_NOT_FOUND_EXCEPTION_CODE = 404;

    protected RestClient restClient;
    private String deploymentId;
	private final boolean verbose;
	private final Set<String> serviceNames;
	private final Map<String, Integer> initialNumberOfInstancesPerService;
	private final Map<String, Integer> baseLineNumberOfRunningInstancesPerService;

	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();
    private int lastEventIndex = 0;

	/**
	 * Gets the error message returned when timeout occurs.
	 * 
	 * @return timeout error message
	 */
    protected abstract String getTimeoutErrorMessage();

	public UninstallationProcessInspector(final RestClient restClient, final String deploymentId,
			final boolean verbose, final String applicationName) throws CLIException {
		this.restClient = restClient;
		this.deploymentId = deploymentId;
		this.verbose = verbose;
		this.serviceNames = getServiceNames(applicationName);
		this.initialNumberOfInstancesPerService = getInitialNumberOfInstancesPerService(serviceNames);
		this.baseLineNumberOfRunningInstancesPerService = initialNumberOfInstancesPerService;
	}

	public UninstallationProcessInspector(final RestClient restClient, final String deploymentId,
			final boolean verbose, final Set<String> serviceNames) throws CLIException {
        this.restClient = restClient;
        this.deploymentId = deploymentId;
        this.verbose = verbose;
		this.serviceNames = serviceNames;
		this.initialNumberOfInstancesPerService = getInitialNumberOfInstancesPerService(serviceNames);
        this.baseLineNumberOfRunningInstancesPerService = initialNumberOfInstancesPerService;
    }

    /**
	 * Sets the deployment id.
	 * 
	 * @param deploymentId
	 *            The deployment ID to use for retrieving events.
     */
	public void setDeploymentId(final String deploymentId) {
		this.deploymentId = deploymentId;
	}

	/**
	 * Gets the latest events of this deployment id. Events are sorted by event
	 * index.
	 * 
	 * @return A list of events. If this is the first time events are requested,
	 *         all events are retrieved. Otherwise, only new events (that were
	 *         not reported earlier) are retrieved.
	 * @throws CLIException
	 *             Indicates a failure to get events from the server.
	 */
	public List<String> getLatestEvents() throws CLIException {

		final List<String> eventsStrings = new ArrayList<String>();

		final DeploymentEvents events;
		try {
			events = restClient.getDeploymentEvents(deploymentId, lastEventIndex, -1);
		} catch (final RestClientException e) {
			throw new CLIException(e.getMessage(), e);
		}

        if (events == null || events.getEvents().isEmpty()) {
            return eventsStrings;
        }

		for (final DeploymentEvent event : events.getEvents()) {
            eventsStrings.add(event.getDescription());
        }
        lastEventIndex = events.getEvents().get(events.getEvents().size() - 1).getIndex() + 1;
        return eventsStrings;
    }

	/**
	 * Waits for each service's lifecycle to end. This includes 3 steps: 1. The
	 * service should become unavailable 2. cloud resources are being relesed 3.
	 * Undeployment is completed, indicated by the event:
	 * "Internal event - Service undeployed successfully"
	 * 
	 * @param timeout
	 *            Timeout in minutes
	 * @throws InterruptedException
	 *             Indicates the thread was interrupted
	 * @throws CLIException
	 *             Indicates the operation failed
	 * @throws TimeoutException
	 *             Indicates timeout was reached
	 */
    public void waitForLifeCycleToEnd(final long timeout) throws InterruptedException, CLIException, TimeoutException {

		final ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {

			private final Map<String, Boolean> endedPerService = initWithFalse(serviceNames);
			private final Map<String, Boolean> resourcesMsgPrintedPerService = initWithFalse(serviceNames);

			private Map<String, Boolean> initWithFalse(final Set<String> keys) {
				final Map<String, Boolean> stringBooleanMap = new HashMap<String, Boolean>();
				for (final String stringKey : keys) {
					stringBooleanMap.put(stringKey, false);
                }
				return stringBooleanMap;
            }

            @Override
            public boolean isDone() throws CLIException, InterruptedException {

				for (final String serviceName : endedPerService.keySet()) {

					ServiceDescription serviceDescription;
                    try {
						serviceDescription = getServiceDescription(serviceName);
					} catch (final CLIException e) {
						if (e.getCause() instanceof RestClientResponseException
								&& ((RestClientResponseException) e.getCause()).getStatusCode() 
								== RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
							// if we got here - the service is not installed
							// anymore
							serviceDescription = null;
                        } else {
                            throw new CLIException(e.getMessage(), e);
                        }
                    }

                        List<String> latestEvents;

					printUninstalledInstances(serviceDescription);
                        latestEvents = getLatestEvents();
                        if (latestEvents == null || latestEvents.isEmpty()) {
                            displayer.printNoChange();
                        } else {
						// If the event "Service undeployed successfully" is
						// found - service undeployment ended.
						// This is an "internal" event and should not be
						// printed; it is removed from the list.
						// Even if undeploy has completed - all lifecycle events
						// should be printed first.
						if (latestEvents.contains(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY_EVENT)) {
							latestEvents.remove(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY_EVENT);
                                endedPerService.remove(serviceName);
                            }
                            displayer.printEvents(latestEvents);
                        }

					// If the service's zone is no longer found - USM lifecycle
					// events are not expected anymore.
					// Print "Releasing cloud resources for service <service name>".
					if (serviceDescription == null && !resourcesMsgPrintedPerService.get(serviceName)) {
						displayer.printEvent(ShellUtils.getFormattedMessage("releasing_cloud_resources", serviceName));
                        resourcesMsgPrintedPerService.put(serviceName, true);
                    }

                    }
                return endedPerService.isEmpty();
            }

			private void printUninstalledInstances(final ServiceDescription serviceDescription) {

				int initialNumberOfInstances;
				int baseLineNumberOfRunningInstances;
				int updatedNumberOfRunningInstances;

                if (serviceDescription != null) {
					final String serviceName = serviceDescription.getServiceName();

					initialNumberOfInstances = initialNumberOfInstancesPerService.get(serviceName);
					baseLineNumberOfRunningInstances = baseLineNumberOfRunningInstancesPerService.get(serviceName);
                    updatedNumberOfRunningInstances = serviceDescription.getInstanceCount();

					if (baseLineNumberOfRunningInstances > updatedNumberOfRunningInstances) {
                        // another instance was uninstalled
						final int numberOfRemovedInstance = initialNumberOfInstances - updatedNumberOfRunningInstances;
						displayer.printEvent("succesfully_uninstalled_instances", numberOfRemovedInstance,
								initialNumberOfInstances, serviceName);
						baseLineNumberOfRunningInstancesPerService.put(serviceName, updatedNumberOfRunningInstances);
                    }
                }
            }

        });

	}

	/**
	 * Gets a populated {@link ServiceDescription} object containing the service
	 * details. If the service does not exist, an exception with status code 404
	 * is thrown.
	 * 
	 * @param serviceName
	 *            The service name
	 * @return a populated {@link ServiceDescription} object
	 * @throws CLIException
	 *             Indicates a failure to retrieve data from the server
	 */
	protected abstract ServiceDescription getServiceDescription(String serviceName) throws CLIException;

	// gets the initial number of instances per service
	private Map<String, Integer> getInitialNumberOfInstancesPerService(final Set<String> serviceNames)
			throws CLIException {

		for (final String serviceName : serviceNames) {
			int instanceCount;
			try {
				final ServiceDescription serviceDescription = getServiceDescription(serviceName);
				instanceCount = serviceDescription.getInstanceCount();
			} catch (final CLIException e) {
				if (e.getCause() instanceof RestClientResponseException
						&& ((RestClientResponseException) e.getCause()).getStatusCode() 
						== RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
					// if we got here - the service is not installed anymore
					instanceCount = 0;
				} else {
					throw new CLIException(e.getMessage(), e);
    }
			}

			initialNumberOfInstancesPerService.put(serviceName, instanceCount);
		}

		return null;
	}

    /**
	 * Creates a {@link org.cloudifysource.shell.ConditionLatch} object with the
	 * given timeout (in minutes), using a polling interval of 500 ms.
	 * 
	 * @param timeout
	 *            Timeout, in minutes.
	 * @return a configured {@link org.cloudifysource.shell.ConditionLatch}
	 *         object
     */
    public ConditionLatch createConditionLatch(final long timeout) {
		return new ConditionLatch().verbose(verbose)
                .pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
				.timeout(timeout, TimeUnit.MINUTES).timeoutErrorMessage(getTimeoutErrorMessage());
    }

	/**
	 * Gets a populated {@link ApplicationDescription} object containing
	 * application details.
	 * 
	 * @param applicationName
	 *            The application name
	 * @return a populated {@link ApplicationDescription} object
	 * @throws CLIException
	 *             Indicates a failure to retrieve information from the server
	 */
	protected ApplicationDescription getApplicationDescription(final String applicationName) throws CLIException {

		ApplicationDescription applicationDescription;

		try {
			applicationDescription = restClient.getApplicationDescription(applicationName);
		} catch (final RestClientException e) {
			throw new CLIException(e.getMessage(), e);
		}

		return applicationDescription;
	}

	/**
	 * Gets a list of names representing the services comprising the given
	 * application.
	 * 
	 * @param applicationName
	 *            The application name
	 * @return a set of service names
	 * @throws CLIException
	 *             Indicating a failure to retrieve information from the server
	 */
	protected Set<String> getServiceNames(final String applicationName) throws CLIException {

		final Set<String> serviceNames = new HashSet<String>();

		final ApplicationDescription applicationDescription = getApplicationDescription(applicationName);
		for (final ServiceDescription serviceDescription : applicationDescription.getServicesDescription()) {
			serviceNames.add(serviceDescription.getServiceName());
		}

		return serviceNames;
	}

}
