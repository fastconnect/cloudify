package org.cloudifysource.shell.rest;

import java.util.Arrays;
import java.util.HashSet;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;

/**
 * Inspects the uninstallation process of a service.
 * @author noak, elip
 * @since 2.6.0
 */
public class ServiceUninstallationProcessInspector extends UninstallationProcessInspector {

	private final String applicationName;

	public ServiceUninstallationProcessInspector(final RestClient restClient, final String deploymentId,
			final boolean verbose, final String serviceName, final String applicationName) throws CLIException {
		super(restClient, deploymentId, verbose, new HashSet(Arrays.asList(serviceName)));
        this.applicationName = applicationName;
    }

    @Override
    protected String getTimeoutErrorMessage() {
		return ShellUtils.getFormattedMessage("service_uninstallation_timed_out");
    }

    @Override
	protected ServiceDescription getServiceDescription(final String serviceName) throws CLIException {
		
		ServiceDescription serviceDescription = null;
		try {
			serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
		} catch (RestClientException e) {
			throw new CLIException(e.getMessage(), e);
    }
		
		return serviceDescription;
	}

}
