package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/22/13
 * Time: 2:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Installer {

    /**
     * Executes a rest api call to install a specific service.
     * @param applicationName The name of the application.
     * @param serviceName The name of the service to install.
     * @param request The install service request.
     * @return The install service response.
     * @throws Exception .
     */
     InstallServiceResponse installService(
             final String applicationName,
             final String serviceName,
             final InstallServiceRequest request) throws Exception;
     
     /**
      * Executes a rest api call to install an application.
      * @param applicationName The name of the application.
      * @param request The install application request.
      * @return The install application response.
      * @throws Exception .
      */
     InstallApplicationResponse installApplication(
              final String applicationName,
              final InstallApplicationRequest request) throws Exception;

    /**
     * Executes a rest api call to uninstall a specific service.
     * @param applicationName The application name.
     * @param serviceName The service name.
     * @param timeoutInMinutes Timeout in minutes
     * @return The uninstall service response.
     * @throws Exception .
     */
     UninstallServiceResponse uninstallService(
             final String applicationName,
             final String serviceName,
             final int timeoutInMinutes) throws Exception;
}
