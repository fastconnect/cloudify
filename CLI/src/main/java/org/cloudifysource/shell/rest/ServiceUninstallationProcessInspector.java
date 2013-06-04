package org.cloudifysource.shell.rest;

import java.util.Map;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 1:19 PM
 */
public class ServiceUninstallationProcessInspector extends UninstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service uninstallation timed out. "
            + "Configure the timeout using the -timeout flag.";

    private String applicationName;

    public ServiceUninstallationProcessInspector(final RestClient restClient,
                                                 final String deploymentId,
                                                 final boolean verbose,
                                                 final Map<String, Integer> initialNumberOfInstancesPerService,
                                                 final String applicationName) {
        super(restClient, deploymentId, verbose, initialNumberOfInstancesPerService);
        this.applicationName = applicationName;
    }


    @Override
    protected String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }

    @Override
    protected ServiceDescription getServiceDescription(final String serviceName) throws RestClientException {
        return restClient.getServiceDescription(applicationName, serviceName);
    }
}
