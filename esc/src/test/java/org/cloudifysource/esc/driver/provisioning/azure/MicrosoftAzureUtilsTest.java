package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.junit.Assert;
import org.junit.Test;

public class MicrosoftAzureUtilsTest {

	@Test
	@SuppressWarnings("serial")
	public void testParseDnsServersStringToMap() throws Exception {
		Map<String, String> map1 = new HashMap<String, String>() {
			{
				put("dns_serveur1", "123.0.0.1");
			}
		};
		Map<String, String> map2 = new HashMap<String, String>() {
			{
				put("dns_serveur1", "123.0.0.1");
				put("dns_serveur2", "10.0.0.1");
			}
		};
		Map<String, String> map3 = new HashMap<String, String>() {
			{
				put("dns_serveur1", "123.0.0.1");
				put("dns_serveur2", "127.0.0.1");
				put("dns_serveur3", "192.168.6.15");
			}
		};

		testParse("dns_serveur1:123.0.0.1", map1);
		testParse("dns_serveur1 : 123.0.0.1", map1);
		testParse("	dns_serveur1:123.0.0.1  ", map1);
		testParse("dns_serveur1:123.0.0.1,	  dns_serveur2:10.0.0.1", map2);
		testParse("	dns_serveur1:123.0.0.1   ,	  dns_serveur2:10.0.0.1  ", map2);
		testParse("	dns_serveur1:123.0.0.1,dns_serveur2:10.0.0.1,dns_serveur3:192.168.6.15", map3);
		testParse("	dns_serveur1:123.0.0.1   ,	  dns_serveur2:10.0.0.1 , dns_serveur3:192.168.6.15			", map3);
		testParse(null, new HashMap<String, String>());
		try {
			testParse("dns", null);
			Assert.fail("Expected exception as the input string cannot be parsed");
		} catch (CloudProvisioningException e) {
			// okay
		}
	}

	private void testParse(String input, Map<String, String> expected) throws CloudProvisioningException {
		Map<String, String> map = MicrosoftAzureUtils.parseDnsServersStringToMap(input);
		Assert.assertEquals(input, expected.size(), map.size());
		for (Entry<String, String> entry : expected.entrySet()) {
			Assert.assertTrue(input, map.containsKey(entry.getKey()));
			Assert.assertEquals(input, expected.get(entry.getKey()), entry.getValue());
		}
	}
}
