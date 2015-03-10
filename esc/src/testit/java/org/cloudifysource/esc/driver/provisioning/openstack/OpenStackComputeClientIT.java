package org.cloudifysource.esc.driver.provisioning.openstack;

import java.util.List;

import org.cloudifysource.esc.driver.provisioning.openstack.rest.Hypervisor;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerRequestWithServerGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerResquest;
import org.junit.Before;
import org.junit.Test;

public class OpenStackComputeClientIT {
	OpenStackComputeClient client;

	@Before
	public void before() throws OpenstackJsonSerializationException {

		String endpoint = "http://localhost:5000/v2.0/";
		String username = "hadoop1";
		String password = "silca.15";
		String tenant = "hadoop1";
		String region = "RegionOne";

		client = new OpenStackComputeClient(endpoint, username, password, tenant, region);
	}

	@Test
	public void testHypervisors() throws Exception {
		List<Hypervisor> hypervisors = client.getHypervisors();
		System.out.println(hypervisors);
	}

	@Test
	public void testCreateServerGroup() throws Exception {
		client.createServerGroup("fctest2", "anti-affinity");
	}

	@Test
	public void testDeleteServerGroup() throws Exception {
		client.deleteServerGroup("56559f9e-b423-406b-b621-58016d3183f1");
	}

	@Test
	public void createServer() throws Exception {
		NovaServerResquest novaRequest = new NovaServerResquest();
		novaRequest.setName("testservergroups");
		novaRequest.setImageRef("66a1489e-0a57-4582-9104-c07d9b3d708f");
		novaRequest.setFlavorRef("3");
		novaRequest.addNetworks("e64d158b-4d26-4678-a3e7-147d8eed0644");
		novaRequest.addSecurityGroup("43195483-5f84-4f6a-8002-4e8a233f3d84");

		NovaServerRequestWithServerGroup request = new NovaServerRequestWithServerGroup();
		request.setServer(novaRequest);
		request.setGroup("b5925322-46cb-46ae-ab62-67dcaf8a9760");
		client.createServer(request);
	}
}
