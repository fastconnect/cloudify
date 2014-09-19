package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.junit.Assert;
import org.junit.Test;

public class MicrosoftAzureCloudDriverTestIT extends BaseDriverTestIT {

	private final static Logger logger = Logger.getLogger(MicrosoftAzureCloudDriverTestIT.class.getName());

	private void assertMachineDetails(MachineDetails md) {
		Assert.assertNotNull("MachineDetails is null", md);
		Assert.assertNotNull("machineId is null", md.getMachineId());
		String privateAddress = md.getPrivateAddress();
		String publicAddress = md.getPublicAddress();
		logger.info("private ip=" + privateAddress);
		logger.info("public ip=" + publicAddress);
		Assert.assertNotNull("public address is null", publicAddress);
		Assert.assertNotNull("private address is null", privateAddress);
		// Assert.assertTrue("private address must start with 192.168.20.x, got " + privateAddress,
		// privateAddress.startsWith("192.168.20."));
		// Assert.assertTrue("public address must start with 192.168.5.x, got " + publicAddress,
		// publicAddress.startsWith("192.168.5."));
	}

	private void doTestStartManagementMachine(String computeTemplate, String overridesDir) throws Exception {
		MicrosoftAzureCloudDriver driver = this.createDriver(computeTemplate, overridesDir, true);
		try {
			MachineDetails[] mds = driver.startManagementMachines(null, TIMEOUT, TimeUnit.MILLISECONDS);
			for (MachineDetails md : mds) {
				assertMachineDetails(md);
			}
		} finally {
			if (driver != null) {
				try {
					driver.stopManagementMachines();
				} catch (Exception e) {
					logger.log(Level.WARNING, "Fail to stop machine", e);
				}
			}
		}
	}

	private void doTestStartMachine(String computeTemplate, String overridesDir) throws Exception {
		MicrosoftAzureCloudDriver driver = this.createDriver(computeTemplate, overridesDir, false);
		MachineDetails md = null;
		try {
			md = driver.startMachine(null, TIMEOUT, TimeUnit.MILLISECONDS);
			this.assertMachineDetails(md);
		} finally {
			if (md != null) {
				driver.stopMachine(md.getPrivateAddress(), TIMEOUT, TimeUnit.MILLISECONDS);
			}
		}
	}

	@Test
	public void testStartManagementMachine() throws Exception {
		this.doTestStartManagementMachine("LARGE_WIN2008R2", null);
	}

	@Test
	public void testStartMachine() throws Exception {
		this.doTestStartMachine("LINUX", null);
	}

	@Test
	public void testStartMachineUbuntuWithStaticIp() throws Exception {
		this.doTestStartMachine("UBUNTU1404", null);
	}

	@Test
	public void testStartMangementMachineUbuntuWithStaticIp() throws Exception {
		this.doTestStartManagementMachine("UBUNTU1404", null);
	}

	@Test
	public void testStartMachineWindowsWithStaticIp() throws Exception {
		this.doTestStartManagementMachine("WIN2008R2_IP", null);
	}

	@Test
	public void testStartManagementMachineWithCdiscountConfig() throws Exception {
		this.doTestStartManagementMachine("LARGE_WIN2008R2", null);
	}

}
