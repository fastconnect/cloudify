package org.cloudifysource.dsl.rest.response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstanceDeploymentEvents {

    private Map<Integer, InstanceDeploymentEvent> eventPerIndex = new MaxSizeMap<Integer, InstanceDeploymentEvent>(100);

    public Map<Integer, InstanceDeploymentEvent> getEventPerIndex() {
        return eventPerIndex;
    }

    public void setEventPerIndex(final Map<Integer, InstanceDeploymentEvent> eventPerIndex) {
        this.eventPerIndex = eventPerIndex;
    }

    @Override
    public String toString() {
        return "InstanceDeploymentEvents{"
                + "eventPerIndex=" + eventPerIndex + '}';
    }

    public void add(final InstanceDeploymentEvents instanceDeploymentEvents) {
        for (Map.Entry<Integer, InstanceDeploymentEvent> entry : instanceDeploymentEvents.getEventPerIndex().entrySet()) {
            if (!eventPerIndex.containsKey(entry.getKey())) {
                eventPerIndex.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private class MaxSizeMap<K, V> extends LinkedHashMap<K, V> {

        private int maxSize;

        public MaxSizeMap(final int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }


}
