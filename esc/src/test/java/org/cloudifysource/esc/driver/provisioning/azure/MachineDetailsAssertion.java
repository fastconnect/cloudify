package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.junit.Assert;

public class MachineDetailsAssertion {
	protected static final Logger logger = Logger.getLogger(BaseDriverTestIT.class.getName());

	public void assertMachineDetails(MachineDetails md) throws Exception {
		Assert.assertNotNull("MachineDetails is null", md);
		Assert.assertNotNull("machineId is null", md.getMachineId());
		String privateAddress = md.getPrivateAddress();
		String publicAddress = md.getPublicAddress();
		logger.info("private ip=" + privateAddress);
		logger.info("public ip=" + publicAddress);
		// FIXME public addresses may be null with agents because no endpoints are defined
		// Assert.assertNotNull("public address is null", publicAddress);
		Assert.assertNotNull("private address is null", privateAddress);
		additionalAssertions(md);
	}

	public void additionalAssertions(MachineDetails md) throws Exception {
	}

}
