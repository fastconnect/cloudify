package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;

/**
 * Inspects the uninstallation process of an application.
 * 
 * @author noak
 * @since 2.6.0
 */
public class ApplicationUninstallationProcessInspector extends UninstallationProcessInspector {
	
	private String applicationName;

	public ApplicationUninstallationProcessInspector(final RestClient restClient, final String deploymentId,
			final boolean verbose, final String applicationName) throws CLIException {
		super(restClient, deploymentId, verbose, applicationName);
		this.applicationName = applicationName;
	}

    public String getApplicationName() {
        return applicationName;
    }

    @Override
	protected String getTimeoutErrorMessage() {
		return ShellUtils.getFormattedMessage("application_uninstallation_timed_out");
	}

	@Override
	protected ServiceDescription getServiceDescription(final String serviceName) throws CLIException {
		
		ServiceDescription serviceDescription;
		try {
			serviceDescription = restClient.getServiceDescription(applicationName, serviceName);	
		} catch (RestClientException e) {
			throw new CLIException(e.getMessage(), e, e.getVerbose());
		}
		
		return serviceDescription;
	}


}
