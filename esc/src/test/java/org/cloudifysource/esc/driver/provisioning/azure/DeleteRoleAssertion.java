package org.cloudifysource.esc.driver.provisioning.azure;

import junit.framework.Assert;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;

public class DeleteRoleAssertion extends BaseAssertion {

	private String deploymentLabel = null;

	public DeleteRoleAssertion() {
	}

	public DeleteRoleAssertion(MicrosoftAzureRestClient azureRestClient, String cloudTemplateName,
			ComputeTemplate computeTemplate) {
		super(azureRestClient, computeTemplate);
	}

	@Override
	public void assertMachineDetails(MachineDetails md) throws Exception {
		HostedServices cloudServices = azureRestClient.listHostedServices();
		Assert.assertTrue(cloudServices.contains(cloudServiceName));
		Deployment deployment = azureRestClient.getDeploymentByIp(md.getPrivateAddress(), true);
		Assert.assertNotNull(deployment);
		this.deploymentLabel = deployment.getLabel();

	}

	public String getDeploymentLabel() {
		return deploymentLabel;
	}

}
