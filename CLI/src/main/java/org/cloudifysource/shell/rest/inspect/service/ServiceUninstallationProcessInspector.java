package org.cloudifysource.shell.rest.inspect.service;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.rest.inspect.UninstallationProcessInspector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: elip Date: 6/4/13 Time: 8:03 PM
 */
public class ServiceUninstallationProcessInspector extends UninstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service un-installation timed out. "
            + "Configure the timeout using the -timeout flag.";


    protected final String serviceName;
	protected final String applicationName;

	public ServiceUninstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final boolean verbose,
            final int currentNumberOfRunningInstance,
            final String serviceName,
            final String applicationName,
            final int nextEventIndex) {
		super(restClient, deploymentId, verbose, createOneEntryMap(serviceName, 0), createOneEntryMap(serviceName,
				currentNumberOfRunningInstance));
		this.applicationName = applicationName;
		this.serviceName = serviceName;
		setEventIndex(nextEventIndex);
	}

	public ServiceUninstallationProcessInspector(final RestClient restClient,
                                                 final String deploymentId,
                                                 final boolean verbose,
                                                 final int currentNumberOfRunningInstance,
                                                 final String serviceName,
                                                 final String applicationName,
                                                 final int nextEventIndex,
                                                 final int plannedNumberOfRunningInstance) {
		super(restClient, deploymentId, verbose,
				createOneEntryMap(serviceName, plannedNumberOfRunningInstance),
				createOneEntryMap(serviceName, currentNumberOfRunningInstance));
		this.applicationName = applicationName;
		this.serviceName = serviceName;
		setEventIndex(nextEventIndex);
	}

	private static Map<String, Integer> createOneEntryMap(final String serviceName, final int numberOfInstances) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(serviceName, numberOfInstances);
		return map;
	}

    public String getServiceName() {
        return serviceName;
    }

	@Override
	public boolean lifeCycleEnded() throws RestClientException {
        return restClient.getServicesDescription(deploymentId).isEmpty();
	}

	@Override
	public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        List<ServiceDescription> servicesDescription = restClient
                .getServicesDescription(deploymentId);
        // there should only be one service
        if (servicesDescription.isEmpty()) {
            return 0;
        }
        if (servicesDescription.size() > 1)  {
            throw new IllegalStateException("Got more than one services for deployment id " + deploymentId);
        }
        return servicesDescription.get(0).getInstanceCount();
	}

	@Override
	public String getTimeoutErrorMessage() {
		return TIMEOUT_ERROR_MESSAGE;
	}
}
