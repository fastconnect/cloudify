/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author elip
 * 
 */

@XmlRootElement(name = "HostedService")
@XmlType(propOrder = { "url", "serviceName", "hostedServiceProperties", "deployments" })
public class HostedService {

	private String url;
	private String serviceName;
	private HostedServiceProperties hostedServiceProperties;
	private Deployments deployments;

	@XmlElement(name = "HostedServiceProperties")
	public HostedServiceProperties getHostedServiceProperties() {
		return hostedServiceProperties;
	}

	public void setHostedServiceProperties(HostedServiceProperties hostedServiceProperties) {
		this.hostedServiceProperties = hostedServiceProperties;
	}

	@XmlElement(name = "Deployments")
	public Deployments getDeployments() {
		return deployments;
	}

	public void setDeployments(final Deployments deployments) {
		this.deployments = deployments;
	}

	@XmlElement(name = "Url")
	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	@XmlElement(name = "ServiceName")
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Find a deployment identified by a label
	 * 
	 * @param deploymentLabel
	 * @return Deployment object if found, null otherwise.
	 */
	public Deployment getDeploymentByLabel(String deploymentLabel) {

		for (Deployment d : deployments) {
			if (d.getLabel().equals(deploymentLabel)) {
				return d;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
