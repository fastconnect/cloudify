package org.cloudifysource.dsl.rest.response;

import org.apache.commons.lang.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstanceDeploymentEvent {

    private String descirption;

    public String getDescirption() {
        return descirption;
    }

    public void setDescirption(final String descirption) {
        this.descirption = descirption;
    }

    @Override
    public String toString() {
        return "InstanceDeploymentEvent{" +
                "descirption='" + descirption + '\'' +
                '}';
    }
}
