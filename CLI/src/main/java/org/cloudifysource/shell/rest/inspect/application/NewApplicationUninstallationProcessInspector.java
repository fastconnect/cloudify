package org.cloudifysource.shell.rest.inspect.application;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.rest.inspect.NewUninstallationProcessInspector;

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

    private static final String TIMEOUT_ERROR_MESSAGE = "Application un-installation timed out. "
            + "Configure the timeout using the -timeout flag.";


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
        // all services before undeploy are still present
        return restClient.getServicesDescription(deploymentId).isEmpty();
    }

    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        List<ServiceDescription> servicesDescription = restClient.getServicesDescription(deploymentId);
        for (ServiceDescription serviceDescription : servicesDescription) {
            if (serviceDescription.getServiceName().contains(serviceName)) {
                return serviceDescription.getInstanceCount();
            }
        }
        return 0;
    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
