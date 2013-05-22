/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.controllers;

import com.gigaspaces.client.WriteModifiers;
import net.jini.core.lease.Lease;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.context.kvstorage.spaceentries.*;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * Provides methods usefully for implementation Rest Controller <br>
 * </br> e.g. <br>
 * </br> getApplication(appName) get application by given application name
 * 
 * <ul>
 * <h3>possible response codes</h3>
 * </ul>
 * <li>200 OK – if action is successful</li> <li>4** - In case of permission
 * problem or illegal URL</li> <li>5** - In case of exception or server error</li>
 * 
 * @throws UnsupportedOperationException
 *             , org.cloudifysource.rest.controllers.RestErrorException
 * 
 * 
 * 
 *             <h3>Note :</h3>
 *             <ul>
 *             this class must be thread safe
 *             </ul>
 * 
 * @author ahmad
 * @since 2.5.0
 */

public abstract class BaseRestController {

	// thread safe
	// @see http://wiki.fasterxml.com/JacksonFAQ for more info.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    protected RestConfiguration restConfig;

	@GigaSpaceContext(name = "gigaSpace")
	protected GigaSpace gigaSpace;

	@Autowired(required = true)
	protected MessageSource messageSource;

	protected Application getApplication(final String appName) throws ResourceNotFoundException {

		Application application = restConfig.getAdmin().getApplications().getApplication(appName);
		if (application == null) {
			throw new ResourceNotFoundException(appName);
		}
		return application;

	}

	protected ProcessingUnitInstance getServiceInstance(final ProcessingUnit processingUnit,
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

	protected ProcessingUnit getService(final String appName,
			                            final String serviceName) throws ResourceNotFoundException {

        String absolutePUName = ServiceUtils.getAbsolutePUName(appName, serviceName);
        ProcessingUnit processingUnit = restConfig.getAdmin().getProcessingUnits().getProcessingUnit(absolutePUName);
		if (processingUnit == null) {
			throw new ResourceNotFoundException(serviceName);
		}
		return processingUnit;
	}

	protected String getServiceInstanceEnvVariable(final ProcessingUnitInstance serviceInstance,
                                                   final String variable) {

		if (StringUtils.isNotBlank(variable)) {
			return serviceInstance.getVirtualMachine().getDetails().getEnvironmentVariables().get(variable);
		}
		return null;
	}

	protected ProcessingUnitInstance getServiceInstance(final String appName,
			                                            final String serviceName,
                                                        final int instanceId) throws ResourceNotFoundException {
		ProcessingUnit processingUnit = getService(appName, serviceName);
		return getServiceInstance(processingUnit, instanceId);

	}

	protected Map<String, Object> getAttributes(final String appName,
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

	protected AbstractCloudifyAttribute createCloudifyAttribute(final String applicationName,
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

	protected Object deleteAttribute(final String appName,
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

	protected void setAttributes(final String appName,
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
		for (final Entry<String, Object> attrEntry : attributesMap.entrySet()) {
			final AbstractCloudifyAttribute newAttr = createCloudifyAttribute(appName, serviceName, serviceInstance, attrEntry.getKey(), null);
			gigaSpace.take(newAttr);
			newAttr.setValue(attrEntry.getValue());
			attributesToWrite[i++] = newAttr;
		}
		// write attributes
		gigaSpace.writeMultiple(attributesToWrite, Lease.FOREVER,WriteModifiers.UPDATE_OR_WRITE);

	}

    /**
     * Handles expected exception from the controller, and wrappes it nicely
     * with a {@link Response} object.
     *
     * @param response
     *            - the servlet response.
     * @param e
     *            - the thrown exception.
     * @throws IOException .
     */
    @ExceptionHandler(RestErrorException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void handleExpectedErrors(final HttpServletResponse response,
                                     final RestErrorException e) throws IOException {

        String messageId = (String) e.getErrorDescription().get("error");
        Object[] messageArgs = (Object[]) e.getErrorDescription().get(
                "error_args");
        String formattedMessage = messageSource.getMessage(messageId,
                messageArgs, Locale.US);

        Response<Void> finalResponse = new Response<Void>();
        finalResponse.setStatus("Failed");
        finalResponse.setMessage(formattedMessage);
        finalResponse.setMessageId(messageId);
        finalResponse.setResponse(null);
        finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

        String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
        response.getOutputStream().write(responseString.getBytes());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(final HttpServletResponse response,
                                                final ResourceNotFoundException e) throws IOException {

        String messageId = CloudifyMessageKeys.MISSING_RESOURCE.getName();
        Object[] messageArgs = new Object[] {e.getResourceDescription()};
        String formattedMessage = messageSource.getMessage(messageId,
                messageArgs, Locale.US);

        Response<Void> finalResponse = new Response<Void>();
        finalResponse.setStatus("Failed");
        finalResponse.setMessage(formattedMessage);
        finalResponse.setMessageId(messageId);
        finalResponse.setResponse(null);
        finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(e));

        String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
        response.getOutputStream().write(responseString.getBytes());
    }




    /**
     * Handles unexpected exceptions from the controller, and wrappes it nicely
     * with a {@link Response} object.
     *
     * @param response
     *            - the servlet response.
     * @param t
     *            - the thrown exception.
     * @throws IOException .
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleUnExpectedErrors(final HttpServletResponse response,
                                       final Throwable t) throws IOException {

        Response<Void> finalResponse = new Response<Void>();
        finalResponse.setStatus("Failed");
        finalResponse.setMessage(t.getMessage());
        finalResponse.setMessageId(CloudifyErrorMessages.GENERAL_SERVER_ERROR
                .getName());
        finalResponse.setResponse(null);
        finalResponse.setVerbose(ExceptionUtils.getFullStackTrace(t));

        String responseString = OBJECT_MAPPER.writeValueAsString(finalResponse);
        response.getOutputStream().write(responseString.getBytes());
    }

}
