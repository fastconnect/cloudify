package org.cloudifysource.shell.commands;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.exceptions.CLIStatusException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 11:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class PreparedPackageResolver implements NameAndPackedFileResolver {

    private File zipFile;
    private Service service;

    public PreparedPackageResolver(final File zipFile) {
        this.zipFile = zipFile;
    }

    public String getName() throws IOException, DSLException {
        return service.getName();
    }

    public File getPackedFile() {
        return zipFile;
    }

    @Override
    public void init() throws CLIStatusException, DSLException, IOException {
        this.service = ServiceReader.readServiceFromZip(zipFile);
    }
}
