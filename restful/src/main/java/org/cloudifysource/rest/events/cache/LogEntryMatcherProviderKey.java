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
package org.cloudifysource.rest.events.cache;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:45 PM
 *
 * Key for log entry matchers.
 */
public class LogEntryMatcherProviderKey {

    private String containerId;
    private EventsCacheKey eventsCacheKey;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    public EventsCacheKey getEventsCacheKey() {
        return eventsCacheKey;
    }

    public void setEventsCacheKey(final EventsCacheKey eventsCacheKey) {
        this.eventsCacheKey = eventsCacheKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogEntryMatcherProviderKey that = (LogEntryMatcherProviderKey) o;

        if (!containerId.equals(that.containerId)) {
            return false;
        }
        if (!eventsCacheKey.equals(that.eventsCacheKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = containerId.hashCode();
        result = 31 * result + eventsCacheKey.hashCode();
        return result;
    }
}
