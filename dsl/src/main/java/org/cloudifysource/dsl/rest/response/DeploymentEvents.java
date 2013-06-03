/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.dsl.rest.response;

import org.cloudifysource.dsl.MaxSizeHashMap;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 4:27 PM
 * <br></br>
 *
 * Represents all deployment events. the deployment can either be a service/application installation/uninstallation.
 */
public class DeploymentEvents {

    private Map<Integer, DeploymentEvent> events = new MaxSizeHashMap<Integer, DeploymentEvent>(100);

    public Map<Integer, DeploymentEvent> getEvents() {
        return events;
    }

    public void setEvents(final Map<Integer, DeploymentEvent> events) {
        this.events = events;
    }
    
    /**
     * Add an event to the map. Might override an existing event if the index is already in use.
     * @param index The event index. Higher index indicates a recent event. 
     * @param event The event object to add at the given index.
     */
    public void addEvent(final Integer index, final DeploymentEvent event) {
    	events.put(index, event);
    }
}