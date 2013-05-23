package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvent;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 5:38 PM
 */
public class ServiceInstallationProcessInspector {

    private EventsIterator eventsIterator;
    private String applicationName;
    private String serviceName;
    private RestClient restClient;

    public ServiceInstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final String applicationName,
            final String serviceName) {
        this.eventsIterator = new EventsIterator(deploymentId, restClient);
        this.applicationName = applicationName;
        this.serviceName = serviceName;
        this.restClient = restClient;
    }

    public boolean lifeCycleStarted() {
        return false;

    }

    public String getLatestEvent() throws RestClientException {
        ServiceDeploymentEvent next = eventsIterator.next();
        if (next == null) {
            return null;
        }
        return next.getDescription();
    }

    public boolean lifeCycleEnded() {
        return false;
    }
}
