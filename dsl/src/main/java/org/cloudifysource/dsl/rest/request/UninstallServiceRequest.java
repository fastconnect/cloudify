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

/**
 * A POJO representing a request to uninstallService command via the REST Gateway.
 * It holds the settings for the uninstall operation.
 * 
 * A JSON serialization of this POJO may be used as the request body of the above mentioned REST command.
 * 
 * @author noak
 */
public class UninstallServiceRequest {
	
	/**
	 * The default timeout for service un-deployment.
	 */
	public static final int DEFAULT_TIMEOUT_IN_MINUTES = 5;
	
	private int timeoutInMinutes = DEFAULT_TIMEOUT_IN_MINUTES;

	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}
	
}
