package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 11:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceResolver implements NameAndPackedFileResolver {

    private File serviceDirectory;
    private File overrides;
    private String serviceFileName;

    private Service service;
    private File serviceGroovyFile;

    public ServiceResolver(final File serviceDirectory,
                           final File overrides,
                           final String serviceFileName) {
        this.serviceDirectory = serviceDirectory;
        this.overrides = overrides;
        this.serviceFileName = serviceFileName;
    }

    public String getName() {
        return service.getName();
    }

    public File getPackedFile() throws CLIStatusException, IOException, PackagingException {
        return Packager.pack(serviceGroovyFile, false, service, null);
    }

    @Override
    public void init() throws CLIStatusException, DSLException {
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
        this.service = ServiceReader.readService(serviceGroovyFile, serviceDirectory, null, null, null, false, overrides);
    }
}
