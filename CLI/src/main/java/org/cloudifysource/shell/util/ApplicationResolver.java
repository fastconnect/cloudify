/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * Resolver for an application folder.
 * 
 * @author adaml
 *
 */
public class ApplicationResolver implements NameAndPackedFileResolver {

	private File applicationDir;
	private File overridesFile;
	private Application application;
	private boolean initialized = false;
	private File cloudConfiguration = null;
	private File packedFile;
	
	protected static final Logger logger = Logger.getLogger(ApplicationResolver.class.getName());
	
	public ApplicationResolver(final File appDir,
							   final File overrides,
							   final File cloudConfiguration) {
		this.applicationDir = appDir;
		this.overridesFile = overrides;
		this.cloudConfiguration  = cloudConfiguration;
	}
	
	@Override
	public String getName() throws CLIStatusException {
		if (!initialized) {
			try {
				init();
				//TODO:adaml change this to proper messages
			} catch (IOException e) {
				throw new CLIStatusException("");
			} catch (PackagingException e) {
				throw new CLIStatusException("");
			} catch (DSLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return application.getName();
	}
	

	@Override
	public File getPackedFile() throws CLIStatusException {
		return this.packedFile;
	}

	private void init() throws CLIStatusException, 
				IOException, PackagingException, DSLException {
		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveApplication(applicationDir)) {
			applicationDir = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("application_not_found",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

	
		final DSLReader dslReader = createDslReader();
		this.application = dslReader.readDslEntity(Application.class);

		final File cloudConfigurationZipFile = createCloudConfigurationZipFile();
		final List<File> additionalServiceFiles = new LinkedList<File>();
		if (cloudConfigurationZipFile != null) {
			additionalServiceFiles.add(cloudConfigurationZipFile);
		}
		this.packedFile = Packager.packApplication(application, applicationDir, additionalServiceFiles);

	}

	private File createCloudConfigurationZipFile()
			throws CLIStatusException, IOException {
		if (this.cloudConfiguration == null) {
			return null;
		}

		if (!this.cloudConfiguration.exists()) {
			throw new CLIStatusException("cloud_configuration_file_not_found",
					this.cloudConfiguration.getAbsolutePath());
		}

		// create a temp file in a temp directory
		final File tempDir = File.createTempFile("__Cloudify_Cloud_configuration", ".tmp");
		FileUtils.forceDelete(tempDir);
		final boolean mkdirs = tempDir.mkdirs();
		if (!mkdirs) {
			logger.info("Field to create temporary directory " + tempDir.getAbsolutePath());
		}
		final File tempFile = new File(tempDir, CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);
		logger.info("Created temporary file " + tempFile.getAbsolutePath()
				+ " in temporary directory" + tempDir.getAbsolutePath());

		// mark files for deletion on JVM exit
		tempFile.deleteOnExit();
		tempDir.deleteOnExit();

		if (this.cloudConfiguration.isDirectory()) {
			ZipUtils.zip(this.cloudConfiguration, tempFile);
		} else if (this.cloudConfiguration.isFile()) {
			ZipUtils.zipSingleFile(this.cloudConfiguration, tempFile);
		} else {
			throw new IOException(this.cloudConfiguration + " is neither a file nor a directory");
		}

		return tempFile;
	}
	
	private DSLReader createDslReader() {
		final DSLReader dslReader = new DSLReader();
		final File dslFile = DSLReader.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationDir);
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());
		dslReader.setOverridesFile(this.overridesFile);
		return dslReader;
	}
}
