package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;

public class BaseAssertion extends MachineDetailsAssertion {

	protected ComputeTemplate computeTemplate;
	protected MicrosoftAzureRestClient azureRestClient;
	protected long endTime = 1000000;
	protected String deploymentSlot;
	protected String cloudServiceName;
	protected String managementGroup;

	public BaseAssertion() {
	}

	public BaseAssertion(MicrosoftAzureRestClient azureRestClient, ComputeTemplate computeTemplate) {
		this.azureRestClient = azureRestClient;
		this.computeTemplate = computeTemplate;
		this.cloudServiceName = (String) computeTemplate.getCustom().get("azure.cloud.service");
		this.deploymentSlot = (String) computeTemplate.getCustom().get("azure.deployment.slot");
	}

	/**
	 * Checks deployment existence, this checks also the hosted service
	 * 
	 * @param deploymentLabel
	 * @throws MicrosoftAzureException
	 * @throws TimeoutException
	 */
	public void assertCloudSeviceAndDeploymentExist(String deploymentLabel) throws MicrosoftAzureException,
			TimeoutException {
		HostedService hostedService = azureRestClient.getHostedService(cloudServiceName, true);
		Assert.assertNotNull(hostedService);
		Assert.assertNotNull(hostedService.getDeploymentByLabel(deploymentLabel));
	}

	public String getDeploymentSlot() {
		return deploymentSlot;
	}

	public void setDeploymentSlot(String deploymentSlot) {
		this.deploymentSlot = deploymentSlot;
	}

	public String getManagementGroup() {
		return managementGroup;
	}

	public void setManagementGroup(String managementGroup) {
		this.managementGroup = managementGroup;
	}

}
