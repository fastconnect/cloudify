package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Rules")
public class Rules implements Iterable<Rule> {

	private List<Rule> rules = new ArrayList<Rule>();

	@XmlElement(name = "Rule")
	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	@Override
	public Iterator<Rule> iterator() {
		return rules.iterator();
	}

}
