package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
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

    private DeploymentEvents events = new DeploymentEvents();

    public EventsProvider(final String deploymentId,
                          final RestClient restClient) {
        this.deploymentId = deploymentId;
        this.restClient = restClient;

    }

    public DeploymentEvent next() throws RestClientException {

        if (events.getEvents().isEmpty()) {
            // get all events
            events = restClient.getDeploymentEvents(deploymentId, index, -1);
        }


        DeploymentEvents serviceDeploymentEvents = restClient.getDeploymentEvents(deploymentId, index, -1);
        if (serviceDeploymentEvents.getEvents().isEmpty()) {
            return null;
        } else {
            return serviceDeploymentEvents.getEvents().get(index++);
        }
    }
}
