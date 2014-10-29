package org.cloudifysource.esc.driver.provisioning.storage.azure;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;

public class AzureDeploymentContext {

	private final String cloudServiceName;
	private final String deploymentName;
	private MicrosoftAzureRestClient azureClient;

	public AzureDeploymentContext(String cloudServiceName, String deploymentName, MicrosoftAzureRestClient azureClient) {
		this.cloudServiceName = cloudServiceName;
		this.deploymentName = deploymentName;
		this.azureClient = azureClient;
	}

	public String getCloudServiceName() {
		return cloudServiceName;
	}

	public String getDeploymentName() {
		return deploymentName;
	}

	public MicrosoftAzureRestClient getAzureClient() {
		return azureClient;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
