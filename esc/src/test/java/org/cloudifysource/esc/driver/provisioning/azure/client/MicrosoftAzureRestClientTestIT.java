package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.net.MalformedURLException;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.azure.TestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkSites;
import org.junit.BeforeClass;
import org.junit.Test;

public class MicrosoftAzureRestClientTestIT {

	private static MicrosoftAzureRestClient client;

	@BeforeClass
	public static void beforeClass() throws MalformedURLException {
		client = TestUtils.createOpenStackNumergyNetworkClient();
	}

	@Test
	public void testListVirtualNetworkSites() throws MalformedURLException, MicrosoftAzureException, TimeoutException {
		VirtualNetworkSites listVirtualNetworkSites = client.listVirtualNetworkSites();

		System.out.println(listVirtualNetworkSites);

	}

}
