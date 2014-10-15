package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LocalNetworkSites")
public class LocalNetworkSites implements Iterable<LocalNetworkSite> {

	private List<LocalNetworkSite> localNetworkSites = new ArrayList<LocalNetworkSite>();

	@Override
	public Iterator<LocalNetworkSite> iterator() {
		return getLocalNetworkSites().iterator();
	}

	@XmlElement(name = "LocalNetworkSite")
	public List<LocalNetworkSite> getLocalNetworkSites() {
		return localNetworkSites;
	}

	public void setLocalNetworkSites(List<LocalNetworkSite> localNetworkSites) {
		this.localNetworkSites = localNetworkSites;
	}

}
