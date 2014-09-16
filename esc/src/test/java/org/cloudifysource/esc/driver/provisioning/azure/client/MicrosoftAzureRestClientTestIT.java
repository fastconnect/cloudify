package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.azure.TestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
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
		VirtualNetworkConfiguration listVirtualNetworkConfiguration = client.getVirtualNetworkConfiguration();
		System.out.println(listVirtualNetworkConfiguration);
	}

	@Test
	public void testRebootVirtualMachine() throws Exception {
		long timeout = 60L * 1000L * 20L;
		client.rebootVirtualMachine(new Date().getTime() + timeout);
		System.out.println("rebooted");
	}

}
