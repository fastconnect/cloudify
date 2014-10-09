package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "DnsServersRef")
public class DnsServersRef implements Iterable<DnsServerRef> {

	List<DnsServerRef> dnsServersRef = new ArrayList<DnsServerRef>();

	@XmlElement(name = "DnsServerRef")
	public List<DnsServerRef> getDnsServersRef() {
		return dnsServersRef;
	}

	public void setDnsServersRef(List<DnsServerRef> dnsServersRef) {
		this.dnsServersRef = dnsServersRef;
	}

	@Override
	public Iterator<DnsServerRef> iterator() {
		return dnsServersRef.iterator();
	}

	public boolean containsDnsName(String dnsName) {
		for (DnsServerRef dns : dnsServersRef) {
			if (dns.getName().equals(dnsName)) {
				return true;
			}
		}
		return false;
	}
}
