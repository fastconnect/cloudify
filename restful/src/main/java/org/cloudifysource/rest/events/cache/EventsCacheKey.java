package org.cloudifysource.rest.events.cache;

import org.openspaces.admin.gsc.GridServiceContainer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCacheKey {

    private String appName;
    private String serviceName;



    public EventsCacheKey(final String appName, final String serviceName) {
        this.appName = appName;
        this.serviceName = serviceName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventsCacheKey that = (EventsCacheKey) o;

        if (!appName.equals(that.appName)) return false;
        if (!serviceName.equals(that.serviceName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = appName.hashCode();
        result = 31 * result + serviceName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EventsCacheKey{" +
                "appName='" + appName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
