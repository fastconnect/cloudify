package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 5:38 PM
 */
public class ServiceInstallationProcessInspector extends InstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out. Configure the timeout using the -timeout flag.";

    private String serviceName;
    private int plannedNumberOfInstances;

    public ServiceInstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final String serviceName,
                                               final int plannedNumberOfInstances) {
        super(restClient, deploymentId, verbose);
        this.serviceName = serviceName;
        this.plannedNumberOfInstances = plannedNumberOfInstances;
    }

    @Override
    public boolean lifeCycleEnded() throws RestClientException {
        ServiceDescription serviceDescription = restClient.getServiceDescription(CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName);
        return serviceDescription.getServiceState().equals(CloudifyConstants.DeploymentState.STARTED);
    }

    @Override
    public Map<String, Integer> getPlannedNumberOfInstancesPerService() throws RestClientException {
        Map<String, Integer> plannedNumberOfInstancesPerService = new HashMap<String, Integer>();
        plannedNumberOfInstancesPerService.put(serviceName, plannedNumberOfInstances);
        return plannedNumberOfInstancesPerService;
    }

    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        ServiceDescription serviceDescription = restClient.getServiceDescription(CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName);
        return serviceDescription.getInstanceCount();
    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }
}
