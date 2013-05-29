package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/29/13
 * Time: 1:54 PM
 */
public class ApplicationInstallationProcessInspector extends InstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Application installation timed out. Configure the timeout using the -timeout flag.";

    private String applicationName;
    private Map<String, Integer> plannedNumberOfInstancesPerService;

    public ApplicationInstallationProcessInspector(final RestClient restClient,
                                                   final String deploymentId,
                                                   final String applicationName,
                                                   final boolean verbose,
                                                   final Map<String, Integer> plannedNumberOfInstancesPerService) {
        super(restClient, deploymentId, verbose, plannedNumberOfInstancesPerService);
        this.applicationName = applicationName;
        this.plannedNumberOfInstancesPerService = plannedNumberOfInstancesPerService;
    }

    @Override
    public boolean lifeCycleEnded() throws RestClientException {
        ApplicationDescription applicationDescription = restClient.getApplicationDescription(applicationName);
        return applicationDescription.getApplicationState().equals(CloudifyConstants.DeploymentState.STARTED);
    }


    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        ServiceDescription serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
        return serviceDescription.getInstanceCount();

    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }
}
