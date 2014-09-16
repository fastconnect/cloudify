package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "DnsServers")
public class DnsServers {

	private List<DnsServer> dnsServers = new ArrayList<DnsServer>();

	@XmlElement(name = "DnsServer")
	public List<DnsServer> getDnsServers() {
		return dnsServers;
	}

	public void setDnsServers(List<DnsServer> dnsServers) {
		this.dnsServers = dnsServers;
	}

}
