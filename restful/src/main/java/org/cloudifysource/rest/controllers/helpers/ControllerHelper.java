package org.cloudifysource.rest.controllers.helpers;

import com.gigaspaces.client.WriteModifiers;
import net.jini.core.lease.Lease;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.*;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 1:11 PM
 */
public class ControllerHelper {

    private GigaSpace gigaSpace;
    private Admin admin;

    public ControllerHelper(final GigaSpace gigaSpace, final Admin admin) {
        this.gigaSpace = gigaSpace;
        this.admin = admin;
    }

    public Application getApplication(final String appName) throws ResourceNotFoundException {

        Application application = admin.getApplications().getApplication(appName);
        if (application == null) {
            throw new ResourceNotFoundException(appName);
        }
        return application;

    }

    public ProcessingUnitInstance getServiceInstance(final ProcessingUnit processingUnit,
                                                        final int instanceId) throws ResourceNotFoundException {

        ProcessingUnitInstance pui = null;
        for (ProcessingUnitInstance processingUnitInstance : processingUnit.getInstances()) {
            if (processingUnitInstance.getInstanceId() == instanceId) {
                pui = processingUnitInstance;
                break;
            }
        }
        if (pui == null) {
            throw new ResourceNotFoundException(processingUnit.getName() + "[" + instanceId + "]");
        }
        return pui;
    }

    public ProcessingUnit getService(final String appName,
                                        final String serviceName) throws ResourceNotFoundException {

        String absolutePUName = ServiceUtils.getAbsolutePUName(appName, serviceName);
        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(absolutePUName);
        if (processingUnit == null) {
            throw new ResourceNotFoundException(serviceName);
        }
        return processingUnit;
    }

    public String getServiceInstanceEnvVariable(final ProcessingUnitInstance serviceInstance,
                                                   final String variable) {

        if (StringUtils.isNotBlank(variable)) {
            return serviceInstance.getVirtualMachine().getDetails().getEnvironmentVariables().get(variable);
        }
        return null;
    }

    public ProcessingUnitInstance getServiceInstance(final String appName,
                                                        final String serviceName,
                                                        final int instanceId) throws ResourceNotFoundException {
        ProcessingUnit processingUnit = getService(appName, serviceName);
        return getServiceInstance(processingUnit, instanceId);

    }

    public Map<String, Object> getAttributes(final String appName,
                                                final String serviceName, final Integer instanceId) {


        final AbstractCloudifyAttribute templateAttribute = createCloudifyAttribute(appName, serviceName, instanceId, null, null);

        // read the matching multiple attributes from the space
        final AbstractCloudifyAttribute[] currAttributes = gigaSpace.readMultiple(templateAttribute);

        // create new map for response
        Map<String, Object> attributes = new HashMap<String, Object>();

        // current attribute for application is null
        if (currAttributes == null) {
            // return empty attributes
            return attributes;

        }

        // update attribute object with current attributes
        for (AbstractCloudifyAttribute applicationCloudifyAttribute : currAttributes) {
            if (applicationCloudifyAttribute.getValue() != null) {
                attributes.put(applicationCloudifyAttribute.getKey(),
                        applicationCloudifyAttribute.getValue().toString());
            }
        }

        // return attributes
        return attributes;
    }

    public AbstractCloudifyAttribute createCloudifyAttribute(final String applicationName,
                                                                final String serviceName,
                                                                final Integer instanceId,
                                                                final String name,
                                                                final Object value) {
        // global
        if (applicationName == null) {
            return new GlobalCloudifyAttribute(name, value);
        }
        // application
        if (serviceName == null) {
            return new ApplicationCloudifyAttribute(applicationName, name,
                    value);
        }
        // service
        if (instanceId == null) {
            return new ServiceCloudifyAttribute(applicationName, serviceName,
                    name, value);
        }
        // instance
        return new InstanceCloudifyAttribute(applicationName, serviceName,
                instanceId, name, value);
    }

    public Object deleteAttribute(final String appName,
                                     final String serviceName,
                                     final Integer instanceId,
                                     final String attributeName) throws ResourceNotFoundException, RestErrorException {

        // attribute name is null
        if (StringUtils.isBlank(attributeName)) {
            throw new RestErrorException(CloudifyMessageKeys.EMPTY_ATTRIBUTE_NAME.getName());
        }

        // get attribute template
        final AbstractCloudifyAttribute attributeTemplate = createCloudifyAttribute(appName, serviceName, instanceId, attributeName, null);

        // delete value
        final AbstractCloudifyAttribute previousValue = gigaSpace.take(attributeTemplate);

        // not exist attribute name
        if (previousValue == null) {
            throw new ResourceNotFoundException(attributeName);
        }

        // return previous value for attribute that already deleted
        return previousValue.getValue();

    }

    public void setAttributes(final String appName,
                                 final String serviceName,
                                 final Integer serviceInstance,
                                 final Map<String, Object> attributesMap) throws RestErrorException {

        // validate attributes map
        if (attributesMap == null) {
            throw new RestErrorException(CloudifyMessageKeys.EMPTY_REQUEST_BODY_ERROR.getName());
        }

        // create templates attributes to write
        final AbstractCloudifyAttribute[] attributesToWrite = new AbstractCloudifyAttribute[attributesMap.size()];

        int i = 0;
        for (final Map.Entry<String, Object> attrEntry : attributesMap.entrySet()) {
            final AbstractCloudifyAttribute newAttr = createCloudifyAttribute(appName, serviceName, serviceInstance, attrEntry.getKey(), null);
            gigaSpace.take(newAttr);
            newAttr.setValue(attrEntry.getValue());
            attributesToWrite[i++] = newAttr;
        }
        // write attributes
        gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }
}
