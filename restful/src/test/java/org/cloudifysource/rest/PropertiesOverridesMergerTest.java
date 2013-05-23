/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.controllers.helpers.PropertiesOverridesMerger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author yael
 * 
 */
public class PropertiesOverridesMergerTest {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private static final String MERGED_FILE_NAME = "propertiesOverridesMergerTest";
	private static final String TEST_TEMP_DIR_PATH = CloudifyConstants.REST_FOLDER + File.separator + MERGED_FILE_NAME;	
	private static final Map<String, String> appProps = new HashMap<String, String>() {
		{
			put("key1", "AP1");
			put("key2", "AP2");
			put("key3", "AP3");
		}
	};
	private static final Map<String, String> serviceProps = new HashMap<String, String>() {
		{
			put("key2", "SP2");
			put("key4", "SP4");
			put("key5", "SP5");
		}
	};
	private static final Map<String, String> serviceOverrides = new HashMap<String, String>() {
		{
			put("key3", "SO3");
			put("key4", "SO4");
			put("key6", "SO6");
		}
	};
	private static final Map<String, String> expectedMergedTwoPropsWithOverrides = new HashMap<String, String>() {
		{
			put("key1", "AP1");
			put("key2", "SP2");
			put("key3", "SO3");
			put("key4", "SO4");
			put("key5", "SP5");
			put("key6", "SO6");

		}
	};
	private static final Map<String, String> expectedMergedAppPropsWithServiceOverrides = new HashMap<String, String>() {
		{
			put("key1", "AP1");
			put("key2", "AP2");
			put("key3", "SO3");
			put("key4", "SO4");
			put("key6", "SO6");

		}
	};
	private static final String OVERRIDES_FILE_NAME = "serviceOverrides.properties";
	private static final String APP_PROPERTIES_FILE_NAME = "applicationProeprties.properties";
	private static final String SERVICE_PROPERTIES_FILE_NAME = "serviceProperties.properties";

	private static File tempDir;

	@BeforeClass
	public static void init() {
		tempDir = new File(TEST_TEMP_DIR_PATH);
		tempDir.mkdirs();
	}

	@AfterClass
	public static void destroy() throws IOException {
		FileUtils.deleteDirectory(tempDir);
	}


	@Test
	public void mergeAppPropsWithServicePropsAndServiceOverridesTest() throws RestErrorException, IOException {		
		File srcDir = new File(tempDir, "srcDir");
		srcDir.mkdirs();
		FileUtils.copyFileToDirectory(createPropertiesFile(SERVICE_PROPERTIES_FILE_NAME, serviceProps), srcDir);
		File servicePropertiesFile = new File(srcDir, SERVICE_PROPERTIES_FILE_NAME);		
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(srcDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergedPropertiesFile(servicePropertiesFile);

		merger.addFileToMerge(createPropertiesFile(APP_PROPERTIES_FILE_NAME, appProps), APP_PROPERTIES_FILE_NAME)
		.addFileToMerge(servicePropertiesFile, SERVICE_PROPERTIES_FILE_NAME)
		.addFileToMerge(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides), OVERRIDES_FILE_NAME);
		File updatedFile = merger.merge(null);	

		validateMergedFile(updatedFile, expectedMergedTwoPropsWithOverrides);	
	}

	@Test
	public void mergeNothingTest() throws RestErrorException, IOException {		
		File srcDir = new File(tempDir, "srcDir");
		srcDir.mkdirs();
		FileUtils.copyFileToDirectory(createPropertiesFile(SERVICE_PROPERTIES_FILE_NAME, serviceProps), srcDir);
		File servicePropertiesFile = new File(srcDir, SERVICE_PROPERTIES_FILE_NAME);		
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(srcDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergedPropertiesFile(servicePropertiesFile);
		File updatedFile = merger.merge(null);	
		Assert.assertNull(updatedFile);
	}

	/**
	 * test merge of application properties and service overrides files.
	 * (service properties file doesn't exist before merging).
	 * @throws RestErrorException
	 * @throws IOException
	 */
	@Test
	public void mergeAppPropsWithServiceOverridesTest() throws RestErrorException, IOException {		
		File srcDir = new File(tempDir, "srcDir");
		srcDir.mkdirs();
		File servicePropertiesFile = new File(srcDir, SERVICE_PROPERTIES_FILE_NAME);		
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(srcDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergedPropertiesFile(servicePropertiesFile);

		merger.addFileToMerge(createPropertiesFile(APP_PROPERTIES_FILE_NAME, appProps), APP_PROPERTIES_FILE_NAME)
		.addFileToMerge(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides), OVERRIDES_FILE_NAME);
		File updatedFile = merger.merge(null);	

		validateMergedFile(updatedFile, expectedMergedAppPropsWithServiceOverrides);	
	}

	/**
	 * test merge when there is only overrides file.
	 * (service properties file doesn't exist before merging).
	 * @throws RestErrorException
	 * @throws IOException
	 */
	@Test
	public void onlyOverridesFileExistTest() throws RestErrorException, IOException {		
		File srcDir = new File(tempDir, "srcDir");
		srcDir.mkdirs();
		File servicePropertiesFile = new File(srcDir, SERVICE_PROPERTIES_FILE_NAME);		
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(srcDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergedPropertiesFile(servicePropertiesFile);
		merger.addFileToMerge(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides), OVERRIDES_FILE_NAME);
		File updatedFile = merger.merge(null);	

		validateMergedFile(updatedFile, serviceOverrides);	
	}
	
	private void validateMergedFile(final File updatedFile, final Map<String , String> expectedProps) 
			throws IOException {
		File unzipFolder = new File(tempDir, "unzip");
		unzipFolder.mkdirs();
		ZipUtils.unzip(updatedFile, unzipFolder);
		File mergedFile = new File(unzipFolder, SERVICE_PROPERTIES_FILE_NAME);
		Assert.assertTrue(mergedFile.exists());
		Properties props = new Properties();
		FileInputStream inStream = new FileInputStream(mergedFile);
		try {
			props.load(inStream);
			final ConfigObject parse = new ConfigSlurper().parse(mergedFile.toURI().toURL());
			Assert.assertEquals(expectedProps.size(), parse.size());
			Set<Entry<String, String>> entrySet = expectedProps.entrySet();
			for (Entry<String, String> entry : entrySet) {
				String value = (String) parse.get(entry.getKey());
				Assert.assertEquals(entry.getValue(), value);
			}
		} finally {
			inStream.close();
		}
	}

	private File createPropertiesFile(final String fileName, final Map<String, String> map) 
			throws IOException {
		File propertiesFile = new File(tempDir, fileName);
		Writer writer = new FileWriter(propertiesFile);
		try {
			Set<Entry<String, String>> entrySet = map.entrySet();
			for (Entry<String, String> entry : entrySet) {
				writer.append(entry.getKey() + "=\"" + entry.getValue() + "\"");
				writer.append(LINE_SEPARATOR);
			}
			return propertiesFile;
		} finally {
			writer.flush();
			writer.close();
		}
	}
}
