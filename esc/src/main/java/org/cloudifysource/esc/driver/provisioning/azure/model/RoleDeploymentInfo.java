package org.cloudifysource.esc.driver.provisioning.azure.model;

/**
 * Used when creating a virtual machine deployment
 *
 */
public class RoleDeploymentInfo {

	private String deploymentName;
	private String cloudServiceName;
	private boolean addToDeployment;
	private boolean createCloudService;

	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public String getCloudServiceName() {
		return cloudServiceName;
	}

	public void setCloudServiceName(String cloudServiceName) {
		this.cloudServiceName = cloudServiceName;
	}

	public boolean isAddToDeployment() {
		return addToDeployment;
	}

	public void setAddToDeployment(boolean addToDeployment) {
		this.addToDeployment = addToDeployment;
	}

	public boolean isCreateCloudService() {
		return createCloudService;
	}

	public void setCreateCloudService(boolean createCloudService) {
		this.createCloudService = createCloudService;
	}

}
