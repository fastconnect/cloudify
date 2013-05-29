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
import java.util.Map;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.exceptions.CLIStatusException;

/**
 * resolver for a prepared, packed application file.
 *   
 * @author adaml
 *
 */
public class PreparedApplicationPackageResolver implements NameAndPackedFileResolver {

	private Application application;
	private File packedFile;
	
	private boolean initialized = false;
	
	public PreparedApplicationPackageResolver(final File packedFile) {
		this.packedFile = packedFile;
	}
	
	@Override
	public String getName() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return application.getName();
	}

	@Override
	public File getPackedFile() throws CLIStatusException {
		if (!initialized) {
			init();
		}
		return packedFile;
	}

    @Override
    public Map<String, Integer> getPlannedNumberOfInstancesPerService() throws CLIStatusException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void init() throws CLIStatusException {
    	File applicationFile = null;
    	try {
            final File applicationFolder = ServiceReader.extractProjectFile(packedFile);
            applicationFile = DSLReader
            				.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationFolder);
            final DSLApplicationCompilatioResult result = ServiceReader.getApplicationFromFile(applicationFile);
            this.application = result.getApplication();
    	} catch (final IOException e) {
    		throw new CLIStatusException("read_dsl_file_failed", 
					applicationFile.getAbsolutePath(), e.getMessage(), e);
    	} catch (final DSLException e) {
    		throw new CLIStatusException("read_dsl_file_failed", 
    						applicationFile.getAbsolutePath(), e.getMessage(), e);
    	}
    	
	}

}
