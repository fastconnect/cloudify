package org.cloudifysource.esc.driver.provisioning.azure.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * @author elip
 * 
 */
@XmlType
public class InputEndpoints {

	@Override
	public String toString() {
		return "InputEndpoints [inputEndpoints=" + inputEndpoints + "]";
	}

	private List<InputEndpoint> inputEndpoints = new ArrayList<InputEndpoint>();

	@XmlElement(name = "InputEndpoint")
	public List<InputEndpoint> getInputEndpoints() {
		return inputEndpoints;
	}

	public void setInputEndpoints(final List<InputEndpoint> inputEndpoints) {
		this.inputEndpoints = inputEndpoints;
	}

	/**
	 * Get an endPoint by public port
	 * 
	 * @param publicPort
	 *            .
	 * @return InputEndpoint object, null when not found
	 */
	public InputEndpoint getInputEndpointByPort(final int publicPort) {
		for (InputEndpoint iep : inputEndpoints) {
			if (iep.getPort() == publicPort) {
				return iep;
			}
		}
		return null;
	}

	/**
	 * Get an endpoint by name.
	 * 
	 * @param name
	 *            The name of the endpoint.
	 * @return The endpoint if exists or null.
	 */
	public InputEndpoint getInputEndpointByName(final String name) {
		for (InputEndpoint iep : inputEndpoints) {
			if (iep.getName().equals(name)) {
				return iep;
			}
		}
		return null;
	}
}
