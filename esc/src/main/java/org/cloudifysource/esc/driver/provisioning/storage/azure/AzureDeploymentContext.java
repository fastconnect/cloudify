package org.cloudifysource.esc.driver.provisioning.storage.azure;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;

public class AzureDeploymentContext {

	private String cloudServiceName;
	private String deploymentName;
	private MicrosoftAzureRestClient azureClient;

	public AzureDeploymentContext(String cloudServiceName, String deploymentName, MicrosoftAzureRestClient azureClient) {
		this.setCloudServiceName(cloudServiceName);
		this.deploymentName = deploymentName;
		this.azureClient = azureClient;
	}

	public String getCloudServiceName() {
		return cloudServiceName;
	}

	public MicrosoftAzureRestClient getAzureClient() {
		return azureClient;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public void setCloudServiceName(String cloudServiceName) {
		this.cloudServiceName = cloudServiceName;
	}

}
