package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 7:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventsProvider {

    private String deploymentId;
    private RestClient restClient;

    private int index = 0;

    private ServiceDeploymentEvents events = new ServiceDeploymentEvents();

    public EventsProvider(final String deploymentId,
                          final RestClient restClient) {
        this.deploymentId = deploymentId;
        this.restClient = restClient;

    }

    public ServiceDeploymentEvent next() throws RestClientException {

        if (events.getEvents().isEmpty()) {
            // get all events
            events = restClient.getServiceDeploymentEvents(deploymentId, index, -1);
        }


        ServiceDeploymentEvents serviceDeploymentEvents = restClient.getServiceDeploymentEvents(deploymentId, index, -1);
        if (serviceDeploymentEvents.getEvents().isEmpty()) {
            return null;
        } else {
            return serviceDeploymentEvents.getEvents().get(index++);
        }
    }
}
