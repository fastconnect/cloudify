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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author yael
 * 
 */
public class PropertiesOverridesMergerTest {
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static File workingDir;
	private static final String MERGED_FILE_NAME = "propertiesOverridesMergerTest";
	private static final String TEST_TEMP_DIR_PATH = CloudifyConstants.REST_FOLDER + File.separator + MERGED_FILE_NAME;
	private static final Map<String, String> appProps = new HashMap<String, String>();
	static {
		appProps.put("key1", "AP1");
		appProps.put("key2", "AP2");
		appProps.put("key3", "AP3");


	};
	private static Map<String, String> serviceProps = new HashMap<String, String>();
	static {
		serviceProps.put("key2", "SP2");
		serviceProps.put("key4", "SP4");
		serviceProps.put("key5", "SP5");

	};
	private static Map<String, String> serviceOverrides = 
			new HashMap<String, String>();
	static
	{
		serviceOverrides.put("key3", "SO3");
		serviceOverrides.put("key4", "SO4");
		serviceOverrides.put("key6", "SO6");

	};

	private static Map<String, String> expectedMergedTwoPropsWithOverrides = 
			new HashMap<String, String>();
	static {
		expectedMergedTwoPropsWithOverrides.put("key1", "AP1");
		expectedMergedTwoPropsWithOverrides.put("key2", "SP2");
		expectedMergedTwoPropsWithOverrides.put("key3", "SO3");
		expectedMergedTwoPropsWithOverrides.put("key4", "SO4");
		expectedMergedTwoPropsWithOverrides.put("key5", "SP5");
		expectedMergedTwoPropsWithOverrides.put("key6", "SO6");


	};

	private static Map<String, String> expectedMergedAppPropsWithServiceOverrides = 
			new HashMap<String, String>(); 
	static {
		expectedMergedAppPropsWithServiceOverrides.put("key1", "AP1");
		expectedMergedAppPropsWithServiceOverrides.put("key2", "AP2");
		expectedMergedAppPropsWithServiceOverrides.put("key3", "SO3");
		expectedMergedAppPropsWithServiceOverrides.put("key4", "SO4");
		expectedMergedAppPropsWithServiceOverrides.put("key6", "SO6");
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
	
	@Before
	public void beforeTest() {
		workingDir = new File(tempDir, "workingDir");
		workingDir.mkdirs();
	}
	
	@After
	public void afterTest() throws IOException {
		FileUtils.deleteDirectory(workingDir);
	}

	@Test
	public void mergeAppPropsWithServicePropsAndServiceOverridesTest() throws RestErrorException, IOException {
		FileUtils.copyFileToDirectory(createPropertiesFile(SERVICE_PROPERTIES_FILE_NAME, serviceProps), workingDir);
		final File servicePropertiesFile = new File(workingDir, SERVICE_PROPERTIES_FILE_NAME);
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(workingDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergeFile(servicePropertiesFile);
		merger.setApplicationPropertiesFile(createPropertiesFile(APP_PROPERTIES_FILE_NAME, appProps));
		merger.setServicePropertiesFile(servicePropertiesFile);
		merger.setOverridesFile(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides));
		final File updatedFile = merger.merge();

		validateMergedFile(updatedFile, expectedMergedTwoPropsWithOverrides);
	}

	@Test
	public void mergeNothingTest() throws RestErrorException, IOException {
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(workingDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setDestMergeFile(new File(workingDir, SERVICE_PROPERTIES_FILE_NAME));
		final File updatedFile = merger.merge();
		Assert.assertNull(updatedFile);
		Assert.assertFalse(new File(workingDir, SERVICE_PROPERTIES_FILE_NAME).exists());
	}

	/**
	 * test merge of application properties and service overrides files. (service properties file doesn't exist before
	 * merging).
	 * 
	 * @throws RestErrorException
	 * @throws IOException
	 */
	@Test
	public void mergeAppPropsWithServiceOverridesTest() throws RestErrorException, IOException {
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(workingDir);
		merger.setRePackFileName(MERGED_FILE_NAME);

		merger.setApplicationPropertiesFile(createPropertiesFile(APP_PROPERTIES_FILE_NAME, appProps));
		merger.setOverridesFile(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides));
		merger.setDestMergeFile(new File(workingDir, SERVICE_PROPERTIES_FILE_NAME));
		final File updatedFile = merger.merge();

		validateMergedFile(updatedFile, expectedMergedAppPropsWithServiceOverrides);
	}

	/**
	 * test merge when there is only overrides file. (service properties file doesn't exist before merging).
	 * 
	 * @throws RestErrorException
	 * @throws IOException
	 */
	@Test
	public void onlyOverridesFileExistTest() throws RestErrorException, IOException {
		final PropertiesOverridesMerger merger = new PropertiesOverridesMerger();
		merger.setRePackFolder(workingDir);
		merger.setRePackFileName(MERGED_FILE_NAME);
		merger.setOverridesFile(createPropertiesFile(OVERRIDES_FILE_NAME, serviceOverrides));
		merger.setDestMergeFile(new File(workingDir, SERVICE_PROPERTIES_FILE_NAME));
		final File updatedFile = merger.merge();

		validateMergedFile(updatedFile, serviceOverrides);
	}

	private void validateMergedFile(final File updatedFile, final Map<String, String> expectedProps)
			throws IOException {
		final File unzipFolder = new File(tempDir, "unzip");
		unzipFolder.mkdirs();
		ZipUtils.unzip(updatedFile, unzipFolder);
		final File mergedFile = new File(unzipFolder, SERVICE_PROPERTIES_FILE_NAME);
		Assert.assertTrue(mergedFile.exists());
		final Properties props = new Properties();
		final FileInputStream inStream = new FileInputStream(mergedFile);
		try {
			props.load(inStream);
			final ConfigObject parse = new ConfigSlurper().parse(mergedFile.toURI().toURL());
			Assert.assertEquals(expectedProps.size(), parse.size());
			final Set<Entry<String, String>> entrySet = expectedProps.entrySet();
			for (final Entry<String, String> entry : entrySet) {
				final String value = (String) parse.get(entry.getKey());
				Assert.assertEquals(entry.getValue(), value);
			}
		} finally {
			inStream.close();
		}
	}

	private File createPropertiesFile(final String fileName, final Map<String, String> map)
			throws IOException {
		final File propertiesFile = new File(tempDir, fileName);
		final Writer writer = new FileWriter(propertiesFile);
		try {
			final Set<Entry<String, String>> entrySet = map.entrySet();
			for (final Entry<String, String> entry : entrySet) {
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
