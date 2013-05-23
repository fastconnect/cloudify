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
package org.cloudifysource.rest.controllers.helpers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * 
 * @author yael
 * 
 */
public class PropertiesOverridesMerger {

	private String rePackFileName;
	private File rePackFolder;
	private File originPackedFile;
	private File destMergedPropertiesFile;
	private final LinkedHashMap<File, String> mergeFilesAndComments = new LinkedHashMap<File, String>();

	/**
	 * Adds the file to be merged. 
	 * The file will be merged after files that were added before, 
	 * meaning this file’s properties will overrides properties of files that were added before.
	 * @param file The file to merge.
	 * @param comment The comment will be appended to the beginning of the file to merge.
	 * @return the updated merger.
	 */
	public PropertiesOverridesMerger addFileToMerge(final File file, final String comment) {
		if (file != null) {
			mergeFilesAndComments.put(file, comment);
		}
		return this;
	}

	public void setRePackFileName(final String rePackFileName) {
		this.rePackFileName = rePackFileName;
	}

	public void setRePackFolder(final File rePackFolder) {
		this.rePackFolder = rePackFolder;
	}

	public void setOriginPackedFile(final File packedFile) {
		this.originPackedFile = packedFile;
	}

	public void setDestMergedPropertiesFile(final File destMergedPropertiesFile) {
		this.destMergedPropertiesFile = destMergedPropertiesFile;
	}

	/**
	 * Merge application properties file with service properties and overrides files.
	 * 
	 * @return the updated packed file or the original one if no merge needed.
	 * @throws RestErrorException .
	 */
	public File merge() throws RestErrorException {
		// check if merge is necessary
		if (mergeFilesAndComments == null || mergeFilesAndComments.isEmpty()) {
			return this.originPackedFile;
		} 
		try {
			// append application properties, service properties and overrides files
			new FileAppender("finalPropertiesFile.properties")
			.appendAll(destMergedPropertiesFile, mergeFilesAndComments);
			return Packager.createZipFile(rePackFileName, rePackFolder);
		} catch (final IOException e) {
			throw new RestErrorException(CloudifyMessageKeys.FAILED_TO_MERGE_OVERRIDES.getName(),
					rePackFileName, e.getMessage());

		}
	}

}
