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
 ******************************************************************************/
package org.cloudifysource.restclient;


import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.restclient.messages.RestClientMessageKeys;
import org.codehaus.jackson.type.TypeReference;

/**
 * 
 * @author yael
 *
 */
public class RestClient {

	private static final Logger logger = Logger.getLogger(RestClient.class.getName());

    private static final String FAILED_CREATING_CLIENT = "failed_creating_client";

	private RestClientExecutor executor;
	
	private static final String UPLOAD_CONTROLLER_URL = "/upload/";
	private static final String DEPLOYMENT_CONTROLLER_URL = "/deployments/";
	private String versionedDeploymentControllerUrl;
	private String versionedUploadControllerUrl;

	private static MessagesUtils messageHandler;


	private static final String HTTPS = "https";

    public RestClient(final URL url,
                      final String username,
                      final String password,
                      final String apiVersion) throws RestClientException {

        this.executor = createExecutor(url, username, password, apiVersion);
    }

    public void setCredentials(final String username,
                               final String password) {
        executor.setCredentials(username, password);
    }


	/**
	 * Returns a HTTP client configured to use SSL.
	 * @param url 
	 * 
	 * @return HTTP client configured to use SSL
	 * @throws org.cloudifysource.restclient.exceptions.RestClientException
	 *             Reporting different failures while creating the HTTP client
	 */
	private DefaultHttpClient getSSLHttpClient(
			final URL url) 
					throws RestClientException {
		try {
			final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			// TODO : support self-signed certs if configured by user upon
			// "connect"
			trustStore.load(null, null);

			final SSLSocketFactory sf = new RestSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			final HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			final SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme(HTTPS, sf, url.getPort()));

			final ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (final Exception e) {
			throw new RestClientException(FAILED_CREATING_CLIENT,
                                          "Failed creating http client",
                                          ExceptionUtils.getFullStackTrace(e));
		}
	}

	/**
	 * Executes a rest api call to install a specific service.
	 * @param applicationName The name of the application.
	 * @param serviceName The name of the service to install.
	 * @param request The install service request.
	 * @return The install service response.
	 * @throws RestClientException .
	 */
	public InstallServiceResponse installService(
			final String applicationName,
			final String serviceName,
			final InstallServiceRequest request) 
					throws RestClientException {
		final String installServiceUrl = 
				versionedDeploymentControllerUrl + applicationName + "/services/" + serviceName;
		return executor.postObject(
				installServiceUrl,
				request, 
				new TypeReference<Response<InstallServiceResponse>>() { }
				);
	}

	/**
	 * 
	 * @param applicationName .
	 * @param serviceName .
	 * @param timeoutInMinutes .
	 * @return .
	 * @throws RestClientException .
	 */
    public UninstallServiceResponse uninstallService(final String applicationName, final String serviceName, 
    		final int timeoutInMinutes) throws RestClientException {
    	
        final String url = versionedDeploymentControllerUrl + applicationName + "/services/" + serviceName;
        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put(CloudifyConstants.REQ_PARAM_TIMEOUT_IN_MINUTES, new Integer(timeoutInMinutes));
        
        return executor.delete(url, requestParams, new TypeReference<Response<UninstallServiceResponse>>() { });
    }

	/**
	 * Uploads a file to the repository.
	 * @param fileName The name of the file to upload.
	 * @param file The file to upload.
	 * @return upload response.
	 * @throws RestClientException .
	 */
	public UploadResponse upload(
			final String fileName, 
			final File file) 
					throws RestClientException {
		validateFile(file);
		String finalFileName = fileName == null ? file.getName() : fileName;
		logger.fine("uploading file " + file.getAbsolutePath() + " with name " + finalFileName);		
		final String uploadUrl = versionedUploadControllerUrl + finalFileName;
		UploadResponse response = executor.postFile(
				uploadUrl, 
				file, 
				CloudifyConstants.UPLOAD_FILE_PARAM_NAME, 
				new TypeReference<Response<UploadResponse>>() {
		});
		return response;
	}

    public void connect() throws RestClientException {
        executor.get(versionedDeploymentControllerUrl + "/testrest", new TypeReference<Response<Void>>() {});
    }
        
        
        private void validateFile(
    		final File file) 
    				throws RestClientException {
		if (file == null) {
			throw MessagesUtils.createRestClientException(
					RestClientMessageKeys.UPLOAD_FILE_MISSING.getName());
		}		
		String absolutePath = file.getAbsolutePath();
		if (!file.exists()) {
			throw MessagesUtils.createRestClientException(
					RestClientMessageKeys.UPLOAD_FILE_DOESNT_EXIST.getName(), 
					absolutePath);
		}
		if (!file.isFile()) {
			throw MessagesUtils.createRestClientException(
					RestClientMessageKeys.UPLOAD_FILE_NOT_FILE.getName(), 
					absolutePath);
		}
		long length = file.length();
		if (length > CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES) {
			throw MessagesUtils.createRestClientException(
					RestClientMessageKeys.UPLOAD_FILE_SIZE_LIMIT_EXCEEDED.getName(), 
					absolutePath, 
					length, 
					CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES);
		}
    }

    private RestClientExecutor createExecutor(final URL url,
                                              final String username,
                                              final String password,
                                              final String apiVersion) throws RestClientException {
        DefaultHttpClient httpClient;
        if (HTTPS.equals(url.getProtocol())) {
            httpClient = getSSLHttpClient(url);
        } else {
            httpClient = new DefaultHttpClient();
        }
        final HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
        versionedDeploymentControllerUrl = apiVersion + DEPLOYMENT_CONTROLLER_URL;
        versionedUploadControllerUrl = apiVersion + UPLOAD_CONTROLLER_URL;
        return new RestClientExecutor(httpClient, url);
    }
}
