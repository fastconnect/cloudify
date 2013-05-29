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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;

import java.util.Map;

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

    public ServiceInstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final Map<String, Integer> plannedNumberOfInstancesPerService,
                                               final String serviceName) {
        super(restClient, deploymentId, verbose, plannedNumberOfInstancesPerService);
        this.serviceName = serviceName;
    }

    @Override
    public boolean lifeCycleEnded() throws RestClientException {
        ServiceDescription serviceDescription = restClient
                .getServiceDescription(CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName);
        return serviceDescription.getServiceState().equals(CloudifyConstants.DeploymentState.STARTED);
    }

    @Override
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
        ServiceDescription serviceDescription = restClient
                .getServiceDescription(CloudifyConstants.DEFAULT_APPLICATION_NAME, serviceName);
        return serviceDescription.getInstanceCount();
    }

    @Override
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }
}
