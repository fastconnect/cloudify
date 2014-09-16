package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Dns")
public class Dns {

	private DnsServers dnsServers;

	@XmlElement(name = "DnsServers")
	public DnsServers getDnsServers() {
		return dnsServers;
	}

	public void setDnsServers(DnsServers dnsServers) {
		this.dnsServers = dnsServers;
	}

}
