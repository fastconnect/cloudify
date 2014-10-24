package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

/**
 * used in custom script extension
 */
public class CustomScriptExtensionInfo {

	private List<String> fileNames = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	private String storageAccountName;
	private String containerName;

	public CustomScriptExtensionInfo() {
	}

	public CustomScriptExtensionInfo(List<String> fileNames, List<String> arguments, String storageAccountName,
			String containerName) {
		this.fileNames = fileNames;
		this.arguments = arguments;
		this.storageAccountName = storageAccountName;
		this.containerName = containerName;
	}

	public List<String> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}

	public String getStorageAccountName() {
		return storageAccountName;
	}

	public void setStorageAccountName(String storageAccountName) {
		this.storageAccountName = storageAccountName;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

}
