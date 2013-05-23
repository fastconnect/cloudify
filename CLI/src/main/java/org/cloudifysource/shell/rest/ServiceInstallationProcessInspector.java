package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 5:38 PM
 */
public class ServiceInstallationProcessInspector {

    private String applicationName;
    private String serviceName;
    private RestClient restClient;
    private String deploymentId;

    private int lastEventIndex = 0;

    public ServiceInstallationProcessInspector(
            final RestClient restClient,
            final String deploymentId,
            final String applicationName,
            final String serviceName) {
        this.applicationName = applicationName;
        this.serviceName = serviceName;
        this.restClient = restClient;
        this.deploymentId = deploymentId;
    }

    public List<String> getLatestEvents() throws RestClientException {

        List<String> eventsStrings = new ArrayList<String>();

        ServiceDeploymentEvents events = restClient.getServiceDeploymentEvents(deploymentId, lastEventIndex, -1);
        if (events == null || events.getEvents().isEmpty()) {
            return null;
        }
        Set<Integer> eventIndices = events.getEvents().keySet();

        Integer[] integers = eventIndices.toArray(new Integer[eventIndices.size()]);

        // sort by event index (corresponds to order of events on the server, pretty much)
        Arrays.sort(integers);

        for (Integer index : integers) {
            eventsStrings.add(events.getEvents().get(index).getDescription());
        }
        lastEventIndex = integers[integers.length - 1] + 1;
        return eventsStrings;
    }

    public boolean lifeCycleEnded() throws RestClientException {
        ServiceDescription serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
        return serviceDescription.getServiceState().equals(CloudifyConstants.DeploymentState.STARTED);
    }
}
