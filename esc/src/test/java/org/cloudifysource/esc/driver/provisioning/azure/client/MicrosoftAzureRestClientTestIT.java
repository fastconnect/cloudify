package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.azure.TestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class MicrosoftAzureRestClientTestIT {

	private static final Logger LOGGER = Logger.getLogger(MicrosoftAzureRestClientTestIT.class.getName());

	private static final long DEFAULT_TIMEOUT = 1000L * 60L * 30L; // 30 minutes
	private static MicrosoftAzureRestClient client;

	@BeforeClass
	public static void beforeClass() throws MalformedURLException {
		client = TestUtils.createOpenStackNumergyNetworkClient();
	}

	private long createDefaultEndTime() {
		return System.currentTimeMillis() + DEFAULT_TIMEOUT;
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

	@Test
	public void testStorageAccount() throws Exception {
		long endTime = createDefaultEndTime();
		String storageAccountName = "testitstorageaccount";
		String affinityGroupName = storageAccountName;
		String location = "West Europe";
		try {

			client.createAffinityGroup(affinityGroupName, location, endTime);
			client.createStorageAccount(affinityGroupName, storageAccountName, endTime);

			StorageServices list = client.listStorageServices();
			Assert.assertTrue(list.contains(storageAccountName));
		} finally {
			// clean resources
			client.deleteStorageAccount(storageAccountName, endTime);
			StorageServices list = client.listStorageServices();
			if (list.contains(storageAccountName)) {
				LOGGER.warning("The storage '" + storageAccountName + "' has not been deleted.");
			}

			client.deleteAffinityGroup(affinityGroupName, endTime);
			AffinityGroups affinityGroups = client.listAffinityGroups();
			if (affinityGroups.contains(affinityGroupName)) {
				LOGGER.warning("The affinity group '" + affinityGroupName + "' has not been deleted.");
			}
		}
	}
}
