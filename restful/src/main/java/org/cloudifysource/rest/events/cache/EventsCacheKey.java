package org.cloudifysource.rest.events.cache;


/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCacheKey {

    private String deploymentId;

    public EventsCacheKey(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventsCacheKey that = (EventsCacheKey) o;

        if (!deploymentId.equals(that.deploymentId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return deploymentId.hashCode();
    }

    @Override
    public String toString() {
        return "EventsCacheKey{" +
                "deploymentId='" + deploymentId + '\'' +
                '}';
    }
}
