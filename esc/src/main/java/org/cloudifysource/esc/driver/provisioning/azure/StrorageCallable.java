package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;

public class StrorageCallable implements Callable<Boolean> {

	private MicrosoftAzureRestClient restClient;
	private String affinityGroup;
	private String name;
	private long endTime;

	public StrorageCallable(MicrosoftAzureRestClient restClient, String affinityGroup, String name, long endTime) {
		this.restClient = restClient;
		this.affinityGroup = affinityGroup;
		this.name = name;
		this.endTime = endTime;
	}

	@Override
	public Boolean call() throws TimeoutException, InterruptedException, MicrosoftAzureException {
		restClient.createStorageAccount(affinityGroup, name, endTime);
		return true;
	}
}
