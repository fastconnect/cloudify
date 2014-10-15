package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ConnectionsToLocalNetwork")
public class ConnectionsToLocalNetwork implements Iterable<LocalNetworkSiteRef> {

	private List<LocalNetworkSiteRef> localNetworkSiteRefs = new ArrayList<LocalNetworkSiteRef>();

	@Override
	public Iterator<LocalNetworkSiteRef> iterator() {
		return getLocalNetworkSiteRefs().iterator();
	}

	@XmlElement(name = "LocalNetworkSiteRef")
	public List<LocalNetworkSiteRef> getLocalNetworkSiteRefs() {
		return localNetworkSiteRefs;
	}

	public void setLocalNetworkSiteRefs(List<LocalNetworkSiteRef> localNetworkSiteRefs) {
		this.localNetworkSiteRefs = localNetworkSiteRefs;
	}

}
