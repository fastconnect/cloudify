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
package org.cloudifysource.dsl.rest.response;

import java.util.List;


/**
 * an install application response POJO.
 *  
 * @author adaml
 *
 */
public class InstallApplicationResponse {
	
	private List<String> serviceOrder;
	private List<String> deploymentIDs;
			
	public List<String> getServiceOrder() {
		return serviceOrder;
	}

	public void setServiceOrder(final List<String> serviceOrder) {
		this.serviceOrder = serviceOrder;
	}

	public List<String> getDeploymentIDs() {
		return deploymentIDs;
	}

	public void setDeploymentIDs(final List<String> deploymentIDs) {
		this.deploymentIDs = deploymentIDs;
	} 
}
