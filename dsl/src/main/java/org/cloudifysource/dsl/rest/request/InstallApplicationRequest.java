package org.cloudifysource.dsl.rest.request;

import java.util.concurrent.TimeUnit;

public class InstallApplicationRequest {
	
	private String applcationFileUploadKey;
	
	private String applicationOverridesUploadKey;
	
	private String cloudOverridesUploadKey;
	
	private String applicationName;

	private boolean isSelfHealing;
	
	private String authGroups;
	
	private boolean isDebugAll;
	
	private String debugModeString;
	
	private String debugEvents;
	
	private int timeout;
	
	private TimeUnit timeUnit;

	public String getApplcationFileUploadKey() {
		return applcationFileUploadKey;
	}

	public void setApplcationFileUploadKey(final String applcationFileUploadKey) {
		this.applcationFileUploadKey = applcationFileUploadKey;
	}

	public String getCloudOverridesUploadKey() {
		return cloudOverridesUploadKey;
	}

	public void setCloudOverridesUploadKey(final String cloudOverridesUploadKey) {
		this.cloudOverridesUploadKey = cloudOverridesUploadKey;
	}

	public String getApplicationOverridesUploadKey() {
		return applicationOverridesUploadKey;
	}

	public void setApplicationOverridesUploadKey(
			final String applicationOverridesUploadKey) {
		this.applicationOverridesUploadKey = applicationOverridesUploadKey;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public boolean isSelfHealing() {
		return isSelfHealing;
	}

	public void setSelfHealing(final boolean isSelfHealing) {
		this.isSelfHealing = isSelfHealing;
	}

	public String getAuthGroups() {
		return authGroups;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public boolean isDebugAll() {
		return isDebugAll;
	}

	public void setDebugAll(final boolean isDebugAll) {
		this.isDebugAll = isDebugAll;
	}

	public String getDebugModeString() {
		return debugModeString;
	}

	public void setDebugModeString(final String debugModeString) {
		this.debugModeString = debugModeString;
	}

	public String getDebugEvents() {
		return debugEvents;
	}

	public void setDebugString(final String debugEvents) {
		this.debugEvents = debugEvents;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(final TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}
}
