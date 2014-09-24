package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.junit.Assert;
import org.junit.Test;

public class MicrosoftAzureCloudDriverTestIT extends BaseDriverTestIT {

	private final static Logger LOGGER = Logger.getLogger(MicrosoftAzureCloudDriverTestIT.class.getName());

	@Test
	public void testStartWindowsManagementMachine() throws Exception {
		this.doStartManagementMachine("win2012");
	}

	@Test
	public void testStartMachineWindowsWithStaticIp() throws Exception {
		this.doStartManagementMachine("win2012_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

	@Test
	public void testStartUbuntuManagementMachine() throws Exception {
		this.doStartManagementMachine("ubuntu1410");
	}

	@Test
	public void testStartMachineUbuntuWithStaticIp() throws Exception {
		this.doStartManagementMachine("ubuntu1410_ipfixed", new MachineDetailsAssertion() {
			@Override
			public void additionalAssertions(MachineDetails md) {
				Assert.assertEquals("10.0.0.12", md.getPrivateAddress());
			}
		});
	}

}
