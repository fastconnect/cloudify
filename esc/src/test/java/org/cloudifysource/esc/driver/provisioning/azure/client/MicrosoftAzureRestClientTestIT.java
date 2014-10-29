package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.net.MalformedURLException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.azure.AzureTestUtils;
import org.cloudifysource.esc.driver.provisioning.azure.model.AffinityGroups;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.VirtualNetworkConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class MicrosoftAzureRestClientTestIT {

	private static final Logger LOGGER = Logger.getLogger(MicrosoftAzureRestClientTestIT.class.getName());

	private static final long DEFAULT_TIMEOUT = 1000L * 60L * 30L; // 30 minutes
	private static MicrosoftAzureRestClient client;

	@BeforeClass
	public static void beforeClass() throws MalformedURLException {
		client = AzureTestUtils.createMicrosoftAzureRestClient();
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

	@Test
	@Ignore("for dev testing")
	public void attachNewDataDiskToVM() throws MicrosoftAzureException, TimeoutException, InterruptedException {
		String serviceName = "frackthis";
		String deploymentName = "frackubuntu";
		String roleName = "frackubuntu";
		String storageAccountName = "vkcfystore01";
		// String datadiskName = "frackyou";
		String vhdFilename = "alibaba.vhd";
		int diskSize = 5;
		int lun = 1;
		long endTime = createDefaultEndTime();
		client.addDataDiskToVM(serviceName, deploymentName, roleName, storageAccountName, vhdFilename,
				diskSize, lun, endTime);
	}

	@Test
	@Ignore("for dev testing")
	public void attachExistingDataDiskToVM() throws MicrosoftAzureException, TimeoutException, InterruptedException {
		String serviceName = "frackthis";
		String deploymentName = "frackubuntu";
		String roleName = "frackubuntu";
		String dataDiskName = "frackubuntu-youpi-sotrage";
		int lun = 1;
		long endTime = createDefaultEndTime();
		client.addExistingDataDiskToVM(serviceName, deploymentName, roleName, dataDiskName, lun, endTime);
	}

	@Test
	@Ignore("for dev testing")
	public void testRemoveDataDisk() throws Exception {
		String serviceName = "frackthis";
		String deploymentName = "frackubuntu";
		String roleName = "frackubuntu";
		int lun = 1;
		long endTime = createDefaultEndTime();
		client.removeDataDisk(serviceName, deploymentName, roleName, lun, endTime);
	}

	@Test
	@Ignore("for dev testing")
	public void deleteDisk() throws MicrosoftAzureException, TimeoutException, InterruptedException {
		String diskName = "frackubuntu-frackubuntu-0-201410281236250867";
		long endTime = createDefaultEndTime();
		client.deleteDisk(diskName, false, endTime);
	}

	@Test
	@Ignore("for dev testing")
	public void listDeployment() throws MicrosoftAzureException, TimeoutException, InterruptedException {
		Deployment listDeploymentsBySlot =
				client.listDeploymentsBySlot("vic14adm001", "Production", createDefaultEndTime());
		System.out.println(listDeploymentsBySlot);
	}

	@Test
	@Ignore("for dev testing")
	public void testUpdateDataDiskName() throws MicrosoftAzureException, TimeoutException, InterruptedException {
		client.updateDataDiskLabel("vic14adm001-vic14storage001-0-201410291505140312", "welcomeWorld",
				createDefaultEndTime());
	}

	@Test
	@Ignore("for dev testing")
	public void listDisks() throws Exception {
		Disks listDisks = client.listDisks();

		for (Disk disk : listDisks.getDisks()) {
			System.out.println(disk.getName() + " | " + disk.getLabel());
		}
	}
}
