package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 *
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConfigurationSets")
public class ConfigurationSets implements Iterable<ConfigurationSet> {

	@XmlElement(name = "ConfigurationSet")
	private List<ConfigurationSet> configurationSets = new ArrayList<ConfigurationSet>();

	public List<ConfigurationSet> getConfigurationSets() {
		return configurationSets;
	}

	public void setConfigurationSets(final List<ConfigurationSet> configurationSets) {
		this.configurationSets = configurationSets;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<ConfigurationSet> iterator() {
		return configurationSets.iterator();
	}

	/**
	 * Get NetworkConfigurationSet <br />
	 * When getting deployment information for a cloud service, a Role has one confugurationSet which is the
	 * NetworkConfigurationSet.
	 * 
	 * @return NetworkConfigurationSet object, null when not found (cast error...)
	 */
	public NetworkConfigurationSet getNetworkConfigurationSet() {

		for (ConfigurationSet cs : configurationSets) {
			try {
				NetworkConfigurationSet networkConfigurationSet = (NetworkConfigurationSet) cs;
				return networkConfigurationSet;
			} catch (Exception e) {
				// TODO: do nothing
			}
		}

		return null;
	}
}
