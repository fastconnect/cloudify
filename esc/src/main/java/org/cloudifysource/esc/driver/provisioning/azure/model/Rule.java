package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Rule", propOrder = { "order", "action", "remoteSubnet", "description" })
public class Rule {

	private Integer order;
	private String action;
	private String remoteSubnet;
	private String description;

	public Rule() {
	}

	public Rule(Integer order, String action, String remoteSubnet, String description) {
		this.order = order;
		this.action = action;
		this.remoteSubnet = remoteSubnet;
		this.description = description;
	}

	@XmlElement(name = "Order")
	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	@XmlElement(name = "Action")
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	@XmlElement(name = "RemoteSubnet")
	public String getRemoteSubnet() {
		return remoteSubnet;
	}

	public void setRemoteSubnet(String remoteSubnet) {
		this.remoteSubnet = remoteSubnet;
	}

	@XmlElement(name = "Description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
