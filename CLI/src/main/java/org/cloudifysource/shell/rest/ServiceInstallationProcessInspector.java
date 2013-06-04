/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 5:38 PM
 * <br></br>
 *
 * Provides functionality for inspecting the installation process of services.
 */
public class ServiceInstallationProcessInspector extends InstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out. "
            + "Configure the timeout using the -timeout flag.";

    private String serviceName;
    private String applicationName;

    public ServiceInstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final Map<String, Integer> plannedNumberOfInstancesPerService,
                                               final String serviceName,
                                               final String applicationName) {
        super(restClient, deploymentId, verbose, plannedNumberOfInstancesPerService);
        this.serviceName = serviceName;
        this.applicationName = applicationName != null ? applicationName : CloudifyConstants.DEFAULT_APPLICATION_NAME;
    }

    @Override
    public Map<String, Integer> initNumberOfCurrentRunningInstancesPerService(Set<String> serviceNames) {
        Map<String, Integer> currentRunningInstancesPerService = new HashMap<String, Integer>();
        for (String service : serviceNames) {
            currentRunningInstancesPerService.put(service, 0);
        }
        return currentRunningInstancesPerService;
    }

    /**
     * Gets the latest events of this deployment id. Events are sorted by event index.
     * @return A list of events. If this is the first time events are requested, all events are retrieved.
     * Otherwise, only new events (that were not reported earlier) are retrieved. 
     * @throws RestClientException Indicates a failure to get events from the server.
     */
    @Override
    public boolean lifeCycleEnded() throws RestClientException {
    	
    	boolean serviceIsInstalled = false;
    	try {
            ServiceDescription serviceDescription = restClient
                    .getServiceDescription(applicationName, serviceName);
            serviceIsInstalled = serviceDescription.getServiceState().equals(CloudifyConstants.DeploymentState.STARTED);
    	} catch (RestClientResponseException e) {
    		if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
        		// the service is not available yet
    			serviceIsInstalled = false;
    		} else {
    			throw e;
    		}
    	}
        
    	return serviceIsInstalled;
    }


    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
    	int instanceCount;
    	try {
    		ServiceDescription serviceDescription = restClient
                .getServiceDescription(applicationName, serviceName);
    		instanceCount = serviceDescription.getInstanceCount();
    	} catch (RestClientResponseException e) {
    		if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
    			//if we got here - the service is not installed yet
    			instanceCount = 0;
        	} else {
        		throw e;
        	}
    	}
    	
    	return instanceCount;
    }

    /**
     * Checks if the specified service is in the target state.
     * @param targetState The desired deployment state
     * @return True - if the service is in the specified state, False otherwise
     * @throws RestClientException Indicates a failure to retrieve the service state
     */
    public boolean isServiceInState(final DeploymentState targetState) throws RestClientException {
        ServiceDescription serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
        return serviceDescription.getServiceState().equals(targetState);
    }
    
    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }
}
