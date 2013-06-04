package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 9:08 PM
 */
public class NewApplicationUninstallationProcessInspector extends NewUninstallationProcessInspector {

    private String applicationName;
    private List<ServiceDescription> serviceDescriptionList;

    public NewApplicationUninstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final boolean verbose,
            final Map<String, Integer> currentRunningInstancesPerService,
            final String applicationName,
            final int nextEventId) {
        super(restClient, deploymentId, verbose, initWithZeros(currentRunningInstancesPerService.keySet()), currentRunningInstancesPerService);
        this.applicationName = applicationName;
        setEventIndex(nextEventId);
    }

    public void setServiceDescriptionList(final List<ServiceDescription> serviceDescriptionList) {
        this.serviceDescriptionList = serviceDescriptionList;
    }

    private static Map<String, Integer> initWithZeros(final Set<String> serviceNames) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (String serviceName : serviceNames) {
            map.put(serviceName, 0);
        }
        return map;
    }

    @Override
    public boolean lifeCycleEnded() throws RestClientException {
        for (ServiceDescription serviceDescription : serviceDescriptionList) {
            try {
                restClient.getServiceDescription(applicationName, serviceDescription.getServiceName());
                return false;
            } catch (final RestClientResponseException e) {
                // this means the service is gone.
            }
        }
        return true;
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
                //if we got here - the service is not installed yet
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

    public String getApplicationName() {
        return applicationName;
    }
}
