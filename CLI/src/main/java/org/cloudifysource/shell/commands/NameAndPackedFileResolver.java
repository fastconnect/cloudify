package org.cloudifysource.shell.commands;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.packaging.PackagingException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 12:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NameAndPackedFileResolver {

    String getName() throws IOException, DSLException;

    File getPackedFile() throws CLIStatusException, IOException, PackagingException;

    void init() throws CLIStatusException, DSLException, IOException;
}
