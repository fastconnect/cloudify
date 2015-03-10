package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Hypervisor {

	private String id;
	private String hypervisorHostname;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHypervisorHostname() {
		return hypervisorHostname;
	}

	public void setHypervisorHostname(String hypervisorHostname) {
		this.hypervisorHostname = hypervisorHostname;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
