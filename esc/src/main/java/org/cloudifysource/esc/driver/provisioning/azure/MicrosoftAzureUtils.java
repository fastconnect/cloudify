package org.cloudifysource.esc.driver.provisioning.azure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.StorageServices;

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

	/**
	 * TODO Checks this method if the format of the VM OS disk media link changes
	 */
	public static String getBalancedStorageAccount(List<String> storageAccounts, MicrosoftAzureRestClient azureClient)
			throws MicrosoftAzureException,
			TimeoutException {

		StorageServices storageServices = azureClient.listStorageServices();
		Map<String, Integer> disksByStorageMap = new HashMap<String, Integer>();

		List<String> existingStorageAccounts = new ArrayList<String>();
		List<String> notExistingStorageAccounts = new ArrayList<String>();

		// check if the specified storage accounts exist in the subscription
		for (String storage : storageAccounts) {
			if (storageServices.contains(storage)) {
				existingStorageAccounts.add(storage);

				// init counter map
				disksByStorageMap.put(storage, 0);
			} else {
				notExistingStorageAccounts.add(storage);
			}
		}

		// all specified SACC don't exist, choose one form the non existing list
		if (existingStorageAccounts.isEmpty()) {
			return notExistingStorageAccounts.get(0);
		}

		// calculate the number of disk for each existing SACC
		for (Disk disk : azureClient.listDisks().getDisks()) {
			String storageName = StringUtils.substringBetween(disk.getMediaLink(), "https://", ".");
			if (existingStorageAccounts.contains(storageName)) {
				Integer currentCount = disksByStorageMap.get(storageName);
				disksByStorageMap.put(storageName, ++currentCount);
			}
		}

		// choose an existing SACC if it dosn't have any disk instead of creating a new one from the not
		// existing list
		if (!notExistingStorageAccounts.isEmpty()) {
			for (String existingStorage : existingStorageAccounts) {
				if (disksByStorageMap.get(existingStorage) == 0) {
					return existingStorage;
				}
			}

			// otherwise choose one from the not existing list
			return notExistingStorageAccounts.get(0);
		}

		// at this point we have to select the existing SACC with a minimum number of disk

		String balancedStorageAccount = null;

		if (notExistingStorageAccounts.isEmpty()) {

			// init by choosing the first element
			balancedStorageAccount = disksByStorageMap.keySet().iterator().next();
			int min = disksByStorageMap.values().iterator().next();

			// get the storage account name that has the minimum number of disks
			for (Entry<String, Integer> entry : disksByStorageMap.entrySet()) {
				String storageName = entry.getKey();
				if (entry.getValue() <= min) {
					min = entry.getValue();
					balancedStorageAccount = storageName;
				}
			}
		}

		return balancedStorageAccount;
	}
}
