package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;

public class MicrosoftAzureUtils {

	public static Map<String, String> parseDnsServersStringToMap(String input) throws CloudProvisioningException {
		Map<String, String> dnsServers = new HashMap<String, String>();
		if (input != null) {
			String[] split = input.split(",");

			for (String string : split) {
				String regex = "\\s*([a-zA-Z_0-9]+)\\s*:\\s*(([1-9]\\d{0,2}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}))\\s*";
				Pattern compile = Pattern.compile(regex);
				Matcher matcher = compile.matcher(string);
				if (matcher.matches()) {
					for (int i = 0; i < matcher.groupCount(); i++) {
						dnsServers.put(matcher.group(1), matcher.group(2));
					}
				} else {
					throw new CloudProvisioningException("Cannot parse dns servers : " + input
							+ ". The value should be like: 'dnsname1:ip,dnsname2:ip'");
				}
			}
		}
		return dnsServers;
	}
}
