/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.rest.request;

import java.util.concurrent.TimeUnit;

/**
 * POJO representation of an installApplication command request via the REST gateway.
 * 
 * @author adaml
 *
 */
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
