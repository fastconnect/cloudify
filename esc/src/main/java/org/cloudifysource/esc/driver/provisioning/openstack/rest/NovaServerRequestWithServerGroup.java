package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonProperty;

public class NovaServerRequestWithServerGroup {

	private NovaServerResquest server;
	@JsonProperty("os:scheduler_hints")
	private SchedulerHint schedulerHint;

	public NovaServerResquest getServer() {
		return server;
	}

	public void setServer(NovaServerResquest server) {
		this.server = server;
	}

	public SchedulerHint getGroup() {
		return schedulerHint;
	}

	public void setGroup(String groupId) {
		this.schedulerHint = new SchedulerHint(groupId);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
