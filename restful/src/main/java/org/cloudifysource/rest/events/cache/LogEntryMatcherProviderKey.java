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

import org.openspaces.admin.gsc.GridServiceContainer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:45 PM
 * <br></br>
 * Key for log entry matchers. <br></br>
 *
 * container - the GridServiceContainer this matcher will match logs from. <br></br>
 * operationId - the operation if this matcher was deidcated to. <br></br>
 * isUndeploy - whther or not the operation was a deploy or undeploy operation.
 * this is important since we need to create different matchers for different operation types<br></br>
 */
public class LogEntryMatcherProviderKey {

    private GridServiceContainer container;
    private String operationId;
    private boolean isUndeploy;

    public GridServiceContainer getContainer() {
        return container;
    }

    public void setContainer(final GridServiceContainer container) {
        this.container = container;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(final String operationId) {
        this.operationId = operationId;
    }

    public boolean isUndeploy() {
        return isUndeploy;
    }

    public void setUndeploy(final boolean undeploy) {
        isUndeploy = undeploy;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntryMatcherProviderKey that = (LogEntryMatcherProviderKey) o;

        if (isUndeploy != that.isUndeploy) return false;
        if (!container.getUid().equals(that.container.getUid())) return false;
        if (!operationId.equals(that.operationId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = container.hashCode();
        result = 31 * result + operationId.hashCode();
        result = 31 * result + (isUndeploy ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LogEntryMatcherProviderKey{" + "container=" + container
                + ", operationId='" + operationId + '\''
                + ", isUndeploy=" + isUndeploy + '}';
    }
}
