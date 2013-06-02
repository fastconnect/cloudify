package org.cloudifysource.dsl.rest.response;

import org.cloudifysource.dsl.MaxSizeHashMap;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceDeploymentEvents {

    private Map<Integer, ServiceDeploymentEvent> events = new MaxSizeHashMap<Integer, ServiceDeploymentEvent>(100);

    public Map<Integer, ServiceDeploymentEvent> getEvents() {
        return events;
    }

    public void setEvents(final Map<Integer, ServiceDeploymentEvent> events) {
        this.events = events;
    }
    
    /**
     * Add an event to the map. Might override an existing event if the index is already in use.
     * @param index The event index. Higher index indicates a recent event. 
     * @param event
     */
    public void addEvent(final Integer index, final ServiceDeploymentEvent event) {
    	events.put(index, event);
    }
}
