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
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.codehaus.jackson.type.TypeReference;

/**
 * 
 * @author yael
 *
 */
public class RestClient {

	private static final Logger logger = Logger.getLogger(RestClient.class.getName());

	private final RestClientExecuter executer;
	
	private static final String UPLOAD_CONTROLLER_URL = "/upload/";
	private static final String DEPLOYMENT_CONTROLLER_URL = "/deployments/";
	private final String versionedDeploymentControllerUrl; 


	private static final String HTTPS = "https";

	/**
	 * Ctor.
	 * 
	 * @param username
	 *            Username for the HTTP client, optional.
	 * @param password
	 *            Password for the HTTP client, optional.
	 * @param apiVersion
	 *            cloudify api version of the client
	 * @throws RestException
	 *             Reporting failure to create a SSL HTTP client.
	 */
	public RestClient(final String username, final String password, final String apiVersion, final URL url)
			throws RestException {

		DefaultHttpClient httpClient;
		if (HTTPS.equals(url.getProtocol())) {
			httpClient = getSSLHttpClient(url);
		} else {
			httpClient = new DefaultHttpClient();
		}
		final HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);

		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {

			@Override
			public void process(final HttpRequest request, final HttpContext context)
					throws HttpException, IOException {
				request.addHeader(CloudifyConstants.REST_API_VERSION_HEADER, apiVersion);
			}
		});
		setCredentials(username, password, httpClient);
		versionedDeploymentControllerUrl = apiVersion + DEPLOYMENT_CONTROLLER_URL;
		executer = new RestClientExecuter(httpClient, url);
	}

	/**
	 * Returns a HTTP client configured to use SSL.
	 * @param url 
	 * 
	 * @return HTTP client configured to use SSL
	 * @throws RestException
	 *             Reporting different failures while creating the HTTP client
	 */
	private DefaultHttpClient getSSLHttpClient(final URL url) throws RestException {
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
		} catch (final KeyStoreException e) {
			throw new RestException(e);
		} catch (final NoSuchAlgorithmException e) {
			throw new RestException(e);
		} catch (final CertificateException e) {
			throw new RestException(e);
		} catch (final IOException e) {
			throw new RestException(e);
		} catch (final KeyManagementException e) {
			throw new RestException(e);
		} catch (final UnrecoverableKeyException e) {
			throw new RestException(e);
		}
	}

	/**
	 * Sets username and password for the HTTP client.
	 * 
	 * @param username
	 *            Username for the HTTP client.
	 * @param password
	 *            Password for the HTTP client.
	 * @param httpClient 
	 */
	private void setCredentials(final String username, final String password, final AbstractHttpClient httpClient) {
		// TODO use userdetails instead of user/pass
		if (StringUtils.notEmpty(username) && StringUtils.notEmpty(password)) {
			httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
					new UsernamePasswordCredentials(username, password));
		}
	}

	/**
	 * 
	 * @param applicationName
	 * 			The name of the application.
	 * @param serviceName
	 * 			The name of the service to install.
	 * @param request
	 * 			The install service request.
	 * @return The install service response.
	 * @throws RestClientException .
	 * @throws TimeoutException .
	 * @throws IOException .
	 */
	public InstallServiceResponse installService(final String applicationName, 
			final String serviceName, final InstallServiceRequest request) 
					throws RestClientException , TimeoutException , IOException {
		final String installServiceUrl = 
				versionedDeploymentControllerUrl + applicationName + "/services/" + serviceName;
		InstallServiceResponse response = 
				executer.postObject(
						installServiceUrl, 
						request, 
						new TypeReference<InstallServiceResponse>() { }
						);
		return response;
	}

	/**
	 * Uploads a file to the repository.
	 * @param fileName
	 * 		The name of the file to upload.
	 * @param file
	 * 		The file to upload.
	 * @return upload response.
	 * @throws IOException .
	 * @throws RestClientException .
	 * @throws TimeoutException .
	 */
	public UploadResponse upload(final String fileName, final File file) 
			throws IOException, RestClientException, TimeoutException {
		final String uploadUrl = 
				UPLOAD_CONTROLLER_URL + fileName;
		UploadResponse response = executer.postFile(
				uploadUrl, 
				file, 
				CloudifyConstants.UPLOAD_FILE_PARAM_NAME, 
				new TypeReference<UploadResponse>() {
		});
		return response;
	}

}
