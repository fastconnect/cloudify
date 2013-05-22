package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.response.UploadResponse;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Uploader {

    /**
     * Uploads a file to the rest server.
     * @param fileName The final file name. may be null, in this case original file name will be used.
     * @param file The file to upload.
     * @return File upload response containing an upload key.
     * @throws Exception .
     */
    UploadResponse upload(final String fileName, final File file) throws Exception;


}
