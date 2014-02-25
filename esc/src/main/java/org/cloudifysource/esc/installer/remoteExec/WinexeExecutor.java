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

package org.cloudifysource.esc.installer.remoteExec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/********
 * Remote Executor implementation for Windows Remote Management, using the
 * powershell command.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class WinexeExecutor implements RemoteExecutor {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(WinexeExecutor.class.getName());

    private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";

    private static Pattern pattern = Pattern.compile(CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX);

    @Override
    public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
    }

    /****************
     * Given a path of the type /C$/PATH - indicating an absolute cifs path,
     * returns /PATH. If the string does not match, returns the original
     * unmodified string.
     * 
     * @param str
     *            the input path.
     * @return the input path, adjusted to remove the cifs drive letter, if it
     *         exists, or the original path if the drive letter is not present.
     */
    public static String normalizeCifsPath(final String str) {

        if (pattern.matcher(str).matches()) {
            final char drive = str.charAt(1);
            return drive + ":\\" + str.substring("/c$/".length()).replace('/', '\\');
        }
        return str;
    }

    @Override
    public void execute(final String targetHost, final InstallationDetails details, final String scriptPath,
            final long endTimeMillis) throws InstallerException, TimeoutException, InterruptedException {

        final String fullCommand = "cmd.exe /c " + normalizeCifsPath(scriptPath);
        executeWinexeCommand(targetHost, details.getUsername(), details.getPassword(), fullCommand);

    }

    private void executeWinexeCommand(final String host, String username, String password, String command) throws InstallerException {
        try {
            // winexe -U Administrateur%topsecret //192.168.5.210 'cmd.exe /c echo "hello world"'
            // Runtime.getRuntime().
            // Process exec = Runtime.getRuntime().exec(String.format("winexe -U %s\\%%s //%s '%s'", username, password, host, command));
            logger.info("Execute winexe on host: " + host + " command: " + command);
            ProcessBuilder pb = new ProcessBuilder("winexe", "-U", username + "%" + password, "//" + host, command);
            Process p = pb.start();
            StreamGobbler inputGobbler = new StreamGobbler(p.getInputStream(), host, "INPUT");
            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), host, "ERROR");
            new Thread(inputGobbler).start();
            new Thread(errorGobbler).start();
            p.waitFor();
        } catch (final Exception e) {
            throw new InstallerException("Failed to execute powershell remote command", e);
        }
    }

    private class StreamGobbler extends Thread {
        private InputStream is;
        private String type;
        private String host;

        private StreamGobbler(InputStream is, String host, String type) {
            this.is = is;
            this.type = type;
            this.host = host;
        }

        @Override
        public void run() {
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    logger.info("[" + host + "][" + type + "] " + line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "", e);
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "", e);
                    }
                }
            }
        }
    }
}
