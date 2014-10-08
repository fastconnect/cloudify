package org.cloudifysource.esc.driver.provisioning.azure;

import java.net.MalformedURLException;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.junit.Assert;

class StorageAssertion extends BaseAssertion {
	private String roleSuffix;
	private String storageName;
	private boolean checkPublicAddress;

	public StorageAssertion(String roleSuffix, String storageName, boolean checkPublicAddress) {
		this.roleSuffix = roleSuffix;
		this.storageName = storageName;
		this.checkPublicAddress = checkPublicAddress;
	}

	@Override
	public void assertMachineDetails(MachineDetails md) throws Exception {
		Assert.assertNotNull("MachineDetails is null", md);
		Assert.assertNotNull("machineId is null", md.getMachineId());
		String privateAddress = md.getPrivateAddress();
		String publicAddress = md.getPublicAddress();
		logger.info("private ip=" + privateAddress);
		if (checkPublicAddress) {
			logger.info("public ip=" + publicAddress);
			Assert.assertNotNull("public address is null", publicAddress);
		}
		Assert.assertNotNull("private address is null", privateAddress);
		additionalAssertions(md);
	}

	public void assertNbDataDisk() throws MalformedURLException,
			MicrosoftAzureException, TimeoutException {
		MicrosoftAzureRestClient client = AzureTestUtils.createMicrosoftAzureRestClient();
		String roleName = String.format("%s%s", this.managementGroup, roleSuffix);
		int nbDiskAttachedToVM = 0;
		Disks disks = client.listDisks();
		for (Disk disk : disks.getDisks()) {
			if (disk.getMediaLink().contains(storageName)) {
				Assert.assertEquals(roleName, disk.getAttachedTo().getRoleName());
				nbDiskAttachedToVM++;
			}
		}
		Assert.assertEquals(2, nbDiskAttachedToVM);
	}
}
