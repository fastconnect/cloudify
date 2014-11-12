package org.cloudifysource.esc.driver.provisioning.azure.model;

/**
 * Used when creating a virtual machine deployment
 */
public class RoleDeploymentInfo {

	private String deploymentName;
	private boolean addToDeployment;

	private CreateHostedService createHostedService;

	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public boolean isAddToDeployment() {
		return addToDeployment;
	}

	public void setAddToDeployment(boolean addToDeployment) {
		this.addToDeployment = addToDeployment;
	}

	public CreateHostedService getCreateHostedService() {
		return createHostedService;
	}

	public void setCreateHostedService(CreateHostedService createHostedService) {
		this.createHostedService = createHostedService;
	}

}
