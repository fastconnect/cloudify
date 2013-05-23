/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.shell.exceptions.CLIStatusException;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 11:42 AM
 *
 * Resolves file name and packed file for a regular service directory.
 *
 * @since 2.6.0
 */
public class ServiceResolver implements NameAndPackedFileResolver {

    private File serviceDirectory;
    private File overrides;
    private String serviceFileName;

    private Service service;
    private File serviceGroovyFile;

    private boolean initialized = false;

    public ServiceResolver(final File serviceDirectory,
                           final File overrides,
                           final String serviceFileName) {
        this.serviceDirectory = serviceDirectory;
        this.overrides = overrides;
        this.serviceFileName = serviceFileName;
    }

    @Override
    public String getName() throws CLIStatusException {
        if (!initialized) {
            init();
        }
        return service.getName();
    }

    @Override
    public File getPackedFile() throws CLIStatusException {
        try {
            if (!initialized) {
                init();
            }
            return Packager.pack(serviceGroovyFile, false, service, null);
        } catch (final Exception e) {
            throw new CLIStatusException(e, "failed_to_package_service",
                    serviceGroovyFile);
        }
    }

    private void init() throws CLIStatusException {
        if (serviceFileName != null) {
            // use overriden service file if defined.
            final File fullPathToServiceFile = new File(serviceDirectory.getAbsolutePath(), serviceFileName);
            if (!fullPathToServiceFile.exists()) {
                throw new CLIStatusException("service_file_doesnt_exist", fullPathToServiceFile.getPath());
            } else {
                serviceGroovyFile = fullPathToServiceFile;
            }
        } else {
            // use default
            serviceGroovyFile = DSLReader.findDefaultDSLFile(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, serviceDirectory);
        }
        try {
            this.service = ServiceReader.readService(serviceGroovyFile,
                    serviceDirectory, null, null, null, false, overrides);
        } catch (final DSLException e) {
            throw new CLIStatusException(e, "read_dsl_file_failed",
                    serviceGroovyFile, e.getMessage());
        }
        this.initialized = true;
    }
}
