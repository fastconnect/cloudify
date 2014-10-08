package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.ServiceNetwork;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;

public class BaseDriverTestIT {

	protected static final Logger logger = Logger.getLogger(BaseDriverTestIT.class.getName());
	protected static final int TIMEOUT = 1000 * 60 * 60; // 60 minutes

	protected void startAndStopManagementMachine(String computeTemplate) throws Exception {
		this.startAndStopManagementMachine(computeTemplate, null, new MachineDetailsAssertion());
	}

	protected void startAndStopManagementMachine(String computeTemplate, MachineDetailsAssertion additionalAssertion)
			throws Exception {
		this.startAndStopManagementMachine(computeTemplate, null, additionalAssertion);
	}

	protected void startAndStopManagementMachine(String computeTemplate, String overridesDir,
			MachineDetailsAssertion assertion) throws Exception {
		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		driverBuilder.setOverridesDir(overridesDir);
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriverAndSetConfig(computeTemplate);
		try {
			MachineDetails[] mds = driver.startManagementMachines(null, TIMEOUT, TimeUnit.MILLISECONDS);
			for (MachineDetails md : mds) {
				assertion.assertMachineDetails(md);
			}
		} finally {
			this.stopManagementMachines(driver);
		}
	}

	protected void startAndStopMachine(String computeTemplate, MachineDetailsAssertion machineDetailsAssertion)
			throws Exception {
		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder(false);
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriverAndSetConfig(computeTemplate);
		MachineDetails md = null;
		try {
			md = driver.startMachine(null, TIMEOUT, TimeUnit.MILLISECONDS);
			machineDetailsAssertion.assertMachineDetails(md);
		} finally {
			if (md != null) {
				driver.stopMachine(md.getPrivateAddress(), TIMEOUT, TimeUnit.MILLISECONDS);
			}
		}
	}

	protected MicrosoftAzureCloudDriver startAndStopMachine(String computeTemplate, ServiceNetwork serviceNetwork,
			MachineDetailsAssertion machineDetailsAssertion) throws Exception {

		AzureDriverTestBuilder driverBuilder = new AzureDriverTestBuilder();
		MicrosoftAzureCloudDriver driver = driverBuilder.createDriver(computeTemplate);
		ComputeDriverConfiguration configuration = driverBuilder.getConfiguration();
		configuration.setNetwork(serviceNetwork);
		driver.setConfig(configuration);

		MachineDetails md = null;
		try {
			md = driver.startMachine(null, TIMEOUT, TimeUnit.MILLISECONDS);
			machineDetailsAssertion.assertMachineDetails(md);
		} finally {
			if (md != null) {
				driver.stopMachine(md.getPrivateAddress(), TIMEOUT, TimeUnit.MILLISECONDS);
			}
		}
		return driver;
	}

	protected MachineDetails[] startManagementMachine(MicrosoftAzureCloudDriver driver)
			throws Exception {
		return this.startManagementMachine(driver, new MachineDetailsAssertion());
	}

	protected MachineDetails[] startManagementMachine(MicrosoftAzureCloudDriver driver,
			MachineDetailsAssertion assertion)
			throws Exception {
		MachineDetails[] mds = null;
		mds = driver.startManagementMachines(null, TIMEOUT, TimeUnit.MILLISECONDS);
		for (MachineDetails md : mds) {
			assertion.assertMachineDetails(md);
		}
		return mds;
	}

	protected void stopManagementMachines(MicrosoftAzureCloudDriver driver) {
		if (driver != null) {
			try {
				driver.stopManagementMachines();
			} catch (Exception e) {
				logger.log(Level.WARNING, "Fail to stop machine", e);
			}
		}
	}

}
