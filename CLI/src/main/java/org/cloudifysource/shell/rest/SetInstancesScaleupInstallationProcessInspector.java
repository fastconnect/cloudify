package org.cloudifysource.shell.rest;

import org.cloudifysource.restclient.RestClient;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/4/13
 * Time: 12:33 PM
 */
public class SetInstancesScaleupInstallationProcessInspector extends ServiceInstallationProcessInspector {

    public SetInstancesScaleupInstallationProcessInspector(final RestClient restClient,
                                                           final String deploymentId,
                                                           final boolean verbose,
                                                           final String serviceName,
                                                           final int plannedNumberOfInstances,
                                                           final String applicationName) {
        super(restClient,
              deploymentId,
              verbose,
              serviceName,
              plannedNumberOfInstances,
              applicationName);
    }

    @Override
    public Map<String, Integer> initNumberOfCurrentRunningInstancesPerService(final Set<String> serviceNames) {
        return super.initNumberOfCurrentRunningInstancesPerService(serviceNames);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
