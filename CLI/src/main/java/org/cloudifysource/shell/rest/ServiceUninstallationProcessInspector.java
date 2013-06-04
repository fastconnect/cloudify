package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This class inspects the uninstallation process of a service. It retrieves the relevant events from the server,
 * waits for the uninstallation lifecycle to end and prints progress messages.
 * 
 * @author noak
 * @since 2.6.0
 */
public class ServiceUninstallationProcessInspector extends UninstallationProcessInspector {

    private static final String TIMEOUT_ERROR_MESSAGE = "Service uninstallation timed out. "
            + "Configure the timeout using the -timeout flag.";

    private int initialNumberOfInstances;
    private int baseLineNumberOfRunningInstances;
    private String serviceName;
    private String applicationName;
    private RestClient restClient;
    
    private CLIEventsDisplayer displayer = new CLIEventsDisplayer();

    public ServiceUninstallationProcessInspector(final RestClient restClient,
                                               final String deploymentId,
                                               final boolean verbose,
                                               final String serviceName,
                                               final String applicationName) throws RestClientException {
    	super(restClient, deploymentId, verbose, null /*plannedNumberOfInstancesPerService*/);
        this.serviceName = serviceName;
        this.applicationName = applicationName != null ? applicationName : CloudifyConstants.DEFAULT_APPLICATION_NAME;
        this.restClient = restClient;
        initialNumberOfInstances = getNumberOfRunningInstances(serviceName);
        baseLineNumberOfRunningInstances = initialNumberOfInstances;
    }

    
    /**
     * Waits for the service uninstallation lifecycle to end, either by the server reaching the uninstalled state,
     * or by reaching the timeout. 
     * @param timeout timeout in minutes
     * @throws InterruptedException Thrown in case the thread was interrupted.
     * @throws CLIException Thrown in case an error happened while trying to retrieve events.
     * @throws TimeoutException Thrown in case the timeout is reached.
     */
    @Override
    public void waitForLifeCycleToEnd(final long timeout) throws InterruptedException, CLIException, TimeoutException {

        ConditionLatch conditionLatch = createConditionLatch(timeout);

        conditionLatch.waitFor(new ConditionLatch.Predicate() {
        	
        	private boolean resourcesMsgPrinted = false;
        	private ServiceDescription serviceDescription = null;
        	
            @Override
            public boolean isDone() throws CLIException, InterruptedException {

                try {
                	boolean ended = false;
                	List<String> latestEvents;
                	
                	try {
                		serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
                	} catch (RestClientResponseException e) {
                		if (e.getStatusCode() == RESOURCE_NOT_FOUND_EXCEPTION_CODE) {
                			//if we got here - the service is not installed anymore
                			serviceDescription = null;
                		} else {
                			throw e;
                		}
                	}
                	
                	printUninstalledInstances(serviceDescription);
                	latestEvents = getLatestEvents();
                	if (latestEvents == null || latestEvents.isEmpty()) {
                		displayer.printNoChange();
                	} else {
                		// If the event "Service undeployed successfully" is found - the server undeploy-thread ended.
                		// This is an "internal" event and should not be printed; it is removed from the list.
                		// Even if undeploy has completed - all lifecycle events should be printed first.
                		if (latestEvents.contains(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY)) {
                			latestEvents.remove(CloudifyConstants.SERVICE_UNDEPLOYED_SUCCESSFULLY);
                			ended = true;
                		}
                		displayer.printEvents(latestEvents);
                    }


                	
                	// If the service' zone is no longer found - USM lifecycle events are not expected anymore.
                	// Print that cloud resources are being released.
            		if (serviceDescription == null && !resourcesMsgPrinted) {
                   		displayer.printEvent(ShellUtils.getFormattedMessage("releasing_cloud_resources"));
                       	resourcesMsgPrinted = true;
               		}
                	
                    return ended;
                } catch (final RestClientException e) {
                    throw new CLIException(e.getMessage(), e);
                }
            }
            
            
            private void printUninstalledInstances(final ServiceDescription serviceDescription)
            		throws RestClientException {

            	int updatedNumberOfRunningInstances;
            	if (serviceDescription != null) {
            		updatedNumberOfRunningInstances = serviceDescription.getInstanceCount();
            		
            		//displayer.printEvent("num_of_running_instances", baseLineNumberOfRunningInstances);
                	//displayer.printEvent("updated_num_of_running_instances", updatedNumberOfRunningInstances);
                	
                    if (baseLineNumberOfRunningInstances > updatedNumberOfRunningInstances) {
                    	int numberOfRemovedInstance = initialNumberOfInstances - updatedNumberOfRunningInstances;
                        // another instance was uninstalled
                        displayer.printEvent("succesfully_uninstalled_instances", numberOfRemovedInstance,
                        		initialNumberOfInstances, serviceName);
                        baseLineNumberOfRunningInstances = updatedNumberOfRunningInstances;
                    }
            	}
            }

        });

    }
    

    /**
     * Gets the number of running instances of the specified service.
     * @param serviceName The service name
     * @return the number of running instances of the specified service
     * @throws RestClientException Indicates a failure to retrieve information from the server
     */
    public int getNumberOfRunningInstances(final String serviceName) throws RestClientException {
    	
    	int instanceCount;
    	try {
            ServiceDescription serviceDescription = restClient.getServiceDescription(applicationName, serviceName);
            instanceCount = serviceDescription.getInstanceCount();    		
    	} catch (RestClientResponseException e) {
    		if (e.getStatusCode() == (RESOURCE_NOT_FOUND_EXCEPTION_CODE)) {
    			//if we got here - the service is not installed anymore
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
    
    public String getTimeoutErrorMessage() {
        return TIMEOUT_ERROR_MESSAGE;
    }

}
