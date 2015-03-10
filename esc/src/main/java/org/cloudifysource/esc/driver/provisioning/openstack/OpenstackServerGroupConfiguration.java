package org.cloudifysource.esc.driver.provisioning.openstack;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;

public class OpenstackServerGroupConfiguration {

	public static final String ANTI_AFFINITY = "anti-affinity";
	public static final String AFFINITY = "affinity";

	private static final String SG = "-sg-";
	private static AtomicInteger index = new AtomicInteger(1);

	private boolean management;
	private String serverGroupPolicy;
	private String serverGroupPrefixName;
	private String serverGroupFinalName;

	public OpenstackServerGroupConfiguration(final ComputeDriverConfiguration configuration,
			final OpenStackResourcePrefixes prefixes)
			throws CloudProvisioningException {

		final Map<String, ComputeTemplate> computeTemplates = configuration.getCloud().getCloudCompute().getTemplates();
		final ComputeTemplate computeTemplate = computeTemplates.get(configuration.getCloudTemplate());
		this.serverGroupPolicy = (String) computeTemplate.getOptions().get(OpenStackCloudifyDriver.OPT_SERVER_GROUPS);

		if (serverGroupPolicy != null) {
			this.checkServerGroupPolicyValue();
			this.initServerGroupName(prefixes);
		}
	}

	private void initServerGroupName(final OpenStackResourcePrefixes prefixes) {
		if (this.management) {
			serverGroupPrefixName = prefixes.getManagementName() + SG;
		} else {
			serverGroupPrefixName = prefixes.getServiceName() + SG;
		}
		this.serverGroupFinalName = serverGroupPrefixName + index.get();
	}

	private void checkServerGroupPolicyValue() throws CloudProvisioningException {
		this.serverGroupPolicy = StringUtils.lowerCase(this.serverGroupPolicy);
		validateServerGroupPolicyValue(this.serverGroupPolicy);
	}

	public static void validateServerGroupPolicyValue(String serverGroupPolicyValue) throws CloudProvisioningException {
		if (!(AFFINITY.equals(serverGroupPolicyValue) || ANTI_AFFINITY.equals(serverGroupPolicyValue))) {
			throw new CloudProvisioningException("Expected values for option '"
					+ OpenStackCloudifyDriver.OPT_SERVER_GROUPS + "': " + new String[] { AFFINITY, ANTI_AFFINITY });
		}
	}

	public boolean isServerGroupConfigured() {
		return serverGroupPolicy != null;
	}

	public String increaseServerGroupNameCounter() {
		this.serverGroupFinalName = serverGroupPrefixName + index.incrementAndGet();
		return serverGroupFinalName;
	}

	public String getServerGroupName() {
		return this.serverGroupFinalName;
	}

	public String getServerGroupPrefixName() {
		return serverGroupPrefixName;
	}
}
