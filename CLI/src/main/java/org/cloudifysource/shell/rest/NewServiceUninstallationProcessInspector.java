package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: elip Date: 6/4/13 Time: 8:03 PM
 */
public class NewServiceUninstallationProcessInspector extends NewUninstallationProcessInspector {

	protected final String serviceName;
	protected final String applicationName;

	public NewServiceUninstallationProcessInspector(final RestClient restClient,
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

	public NewServiceUninstallationProcessInspector(final RestClient restClient,
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
		try {
			restClient.getServiceDescription(applicationName, serviceName);
		} catch (final RestClientResponseException e) {
			if (e.getStatusCode() == 404) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
		int instanceCount;
		try {
			ServiceDescription serviceDescription = restClient
					.getServiceDescription(applicationName, serviceName);
			instanceCount = serviceDescription.getInstanceCount();
		} catch (final RestClientResponseException e) {
			if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
				// if we got here - the service is not installed yet
				instanceCount = 0;
			} else {
				throw e;
			}
		}

		return instanceCount;
	}

	@Override
	public String getTimeoutErrorMessage() {
		return "error";
	}
}
