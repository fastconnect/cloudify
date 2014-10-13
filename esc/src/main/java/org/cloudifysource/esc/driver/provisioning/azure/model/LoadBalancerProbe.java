package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LoadBalancerProbe")
public class LoadBalancerProbe {

	private String path;
	private String port;
	private String protocol;
	private Integer intervalInSeconds; // Optional
	private Integer timeoutInSeconds; // optional

	public LoadBalancerProbe() {
	}

	public LoadBalancerProbe(String port, String protocol) {
		this.port = port;
		this.protocol = protocol;
	}

	@XmlElement(name = "Path")
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@XmlElement(name = "Port")
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	@XmlElement(name = "Protocol")
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@XmlElement(name = "IntervalInSeconds")
	public Integer getIntervalInSeconds() {
		return intervalInSeconds;
	}

	public void setIntervalInSeconds(Integer intervalInSeconds) {
		this.intervalInSeconds = intervalInSeconds;
	}

	@XmlElement(name = "TimeoutInSeconds")
	public Integer getTimeoutInSeconds() {
		return timeoutInSeconds;
	}

	public void setTimeoutInSeconds(Integer timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

}
