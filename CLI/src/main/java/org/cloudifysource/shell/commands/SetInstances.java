/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.commands;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.SetInstancesScaledownInstallationProcessInspector;
import org.cloudifysource.shell.rest.SetInstancesScaleupInstallationProcessInspector;
import org.fusesource.jansi.Ansi.Color;

/************
 * Manually sets the number of instances for a specific service.
 *
 * @author barakme
 *
 */
@Command(scope = "cloudify", name = "set-instances",
		description = "Sets the number of services of an elastic service")
public class SetInstances extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 10;

	@Argument(index = 0, name = "service-name", required = true, description = "the service to scale")
	private String serviceName;

	@Argument(index = 1, name = "count", required = true, description = "the target number of instances")
	private int count;

	@Option(required = false, name = "-timeout",
			description = "number of minutes to wait for instances. Default is set to 1 minute")
	protected int timeout = DEFAULT_TIMEOUT_MINUTES;

	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
			+ "Try to increase the timeout using the -timeout flag";

	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	@Override
	protected Object doExecute() throws Exception {
		final String applicationName = resolveApplicationName();

		final RestClient client = ((RestAdminFacade) this.adminFacade).getNewRestClient();

		final ServiceDescription serviceDescription = client.getServiceDescription(applicationName, serviceName);

		final String deploymentId = serviceDescription.getDeploymentId();

		final int initialNumberOfInstances = serviceDescription.getPlannedInstances();
		if (initialNumberOfInstances == count) {
			return getFormattedMessage("num_instances_already_met", count);
		}

		final int nextEventId = getNextEventId(client, deploymentId);



		SetServiceInstancesRequest request = new SetServiceInstancesRequest();
		request.setCount(count);
		request.setLocationAware(false);
		request.setTimeout(this.timeout);
		// REST API call to server
		client.setServiceInstances(applicationName, serviceName, request);

		if (count > initialNumberOfInstances) {

			return waitForScaleOut(deploymentId, count, nextEventId,
                    initialNumberOfInstances);
		} else {
			return waitForScaleIn(deploymentId, count, nextEventId, initialNumberOfInstances);
		}

		// if(this.count < initialNumberOfInstances) {
		// return waitForInstancesToDecrease();
		// }

		// return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);

		// String pollingID = response.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
		// RestLifecycleEventsLatch lifecycleEventsPollingLatch =
		// this.adminFacade.getLifecycleEventsPollingLatch(pollingID, TIMEOUT_ERROR_MESSAGE);
		// boolean isDone = false;
		// boolean continuous = false;
		// while (!isDone) {
		// try {
		// if (!continuous) {
		// lifecycleEventsPollingLatch.waitForLifecycleEvents(timeout, TimeUnit.MINUTES);
		// } else {
		// lifecycleEventsPollingLatch.continueWaitForLifecycleEvents(timeout, TimeUnit.MINUTES);
		// }
		// isDone = true;
		// } catch (TimeoutException e) {
		// if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
		// throw e;
		// }
		// boolean continueInstallation = promptWouldYouLikeToContinueQuestion();
		// if (!continueInstallation) {
		// throw new CLIStatusException(e, "application_installation_timed_out_on_client",
		// applicationName);
		// } else {
		// continuous = continueInstallation;
		// }
		// }
		// }

		// return getFormattedMessage("set_instances_completed_successfully", Color.GREEN, serviceName, count);
	}

	private int getNextEventId(final RestClient client, final String deploymentId) throws RestClientException {
		int lastEventId = 0;
		final DeploymentEvents lastDeploymentEvents = client.getLastEvent(deploymentId);
		if (!lastDeploymentEvents.getEvents().isEmpty()) {
			lastEventId = lastDeploymentEvents.getEvents().iterator().next().getIndex();
		}
		return lastEventId + 1;
	}

	private String waitForScaleIn(String deploymentID, int plannedNumberOfInstnaces, int lastEventIndex, int currentNumberOfInstances) throws InterruptedException, CLIException, IOException {
		SetInstancesScaledownInstallationProcessInspector inspector =
				new SetInstancesScaledownInstallationProcessInspector(
						((RestAdminFacade) adminFacade).getNewRestClient(),
						deploymentID,
                        verbose,
                        serviceName,
                        plannedNumberOfInstnaces,
						getCurrentApplicationName(),
						lastEventIndex,
                        currentNumberOfInstances);

		int actualTimeout = this.timeout;
		boolean isDone = false;
		displayer.printEvent("installing_service", serviceName, plannedNumberOfInstnaces);
		displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);
		while (!isDone) {
			try {

				inspector.waitForLifeCycleToEnd(actualTimeout);
				isDone = true;

			} catch (final TimeoutException e) {

				// if non interactive, throw exception
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw new CLIException(e.getMessage(), e);
				}

				// ask the user whether to continue viewing the installation or to stop
				displayer.printEvent("");
				boolean continueViewing = promptWouldYouLikeToContinueQuestion();
				if (continueViewing) {
					// prolong the polling timeouts
					actualTimeout = DEFAULT_TIMEOUT_MINUTES;
				} else {
					throw new CLIStatusException(e,
							"service_installation_timed_out_on_client",
							serviceName);
				}
			}
		}

		// drop one line before printing the last message
		displayer.printEvent("");
		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);

	}

	private String waitForScaleOut(String deploymentID, final int plannedNumberOfInstnaces,
			int lastEventIndex, int currentNumberOfInstances) throws InterruptedException,
			CLIException, IOException {
		SetInstancesScaleupInstallationProcessInspector inspector =
				new SetInstancesScaleupInstallationProcessInspector(
						((RestAdminFacade) adminFacade).getNewRestClient(),
						deploymentID,
                        verbose,
                        serviceName,
                        plannedNumberOfInstnaces,
						getCurrentApplicationName(),
						lastEventIndex,
                        currentNumberOfInstances);

		int actualTimeout = this.timeout;
		boolean isDone = false;
		displayer.printEvent("installing_service", serviceName, plannedNumberOfInstnaces);
		displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);
		while (!isDone) {
			try {

				inspector.waitForLifeCycleToEnd(actualTimeout);
				isDone = true;

			} catch (final TimeoutException e) {

				// if non interactive, throw exception
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw new CLIException(e.getMessage(), e);
				}

				// ask the user whether to continue viewing the installation or to stop
				displayer.printEvent("");
				boolean continueViewing = promptWouldYouLikeToContinueQuestion();
				if (continueViewing) {
					// prolong the polling timeouts
					actualTimeout = DEFAULT_TIMEOUT_MINUTES;
				} else {
					throw new CLIStatusException(e,
							"service_installation_timed_out_on_client",
							serviceName);
				}
			}
		}

		// drop one line before printing the last message
		displayer.printEvent("");
		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);

	}

	private String resolveApplicationName() {
		String applicationName = this.getCurrentApplicationName();
		if (applicationName == null) {
			applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
		}
		return applicationName;
	}

	private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
		return ShellUtils.promptUser(session, "would_you_like_to_continue_polling_on_instance_lifecycle_events");
	}

}
