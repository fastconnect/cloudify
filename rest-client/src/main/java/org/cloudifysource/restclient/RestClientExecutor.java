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


import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientHttpException;
import org.cloudifysource.restclient.exceptions.RestClientIOException;
import org.cloudifysource.restclient.exceptions.RestClientResponseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 
 * @author yael
 * 
 */
public class RestClientExecutor {

    private static final String FORWARD_SLASH = "/";
    private static final String SERIALIZATION_ERROR = "serialization_error";
    private static final String EXECUTION_FAILURE_CODE = "execute_request_failed";

    private final DefaultHttpClient httpClient;
    private static final String HTTP_FAILURE_CODE = "http_failure";
    private String urlStr;

	public RestClientExecutor(final DefaultHttpClient httpClient,
                              final URL url) {
		this.httpClient = httpClient;
		this.urlStr = url.toExternalForm();
		if (!this.urlStr.endsWith(FORWARD_SLASH)) {
			this.urlStr += FORWARD_SLASH;
		}
	}

	/**
	 * Executes HTTP post over REST on the given (relative) URL with the given postBody.
	 * 
	 * @param url
	 *            The URL to post to.
	 * @param postBody
	 *            The content of the post.
	 * @param responseTypeReference
	 *            The type reference of the response.
	 * @param <T> The type of the response.
	 * @return The response object from the REST server.
	 * @throws org.cloudifysource.restclient.exceptions.RestClientException
	 *             Reporting failure to post the file.
	 */
	public <T> T postObject(final String url,
                            final Object postBody,
			                final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
		final HttpEntity stringEntity;
		try {
			final String jsonStr = new ObjectMapper().writeValueAsString(postBody);
			stringEntity = new StringEntity(jsonStr, "UTF-8");
		} catch (final IOException e) {
			throw new RestClientIOException(SERIALIZATION_ERROR,
					                        "Failed creating post entity for " + url,
                                            e);
		}
		return post(url, responseTypeReference, stringEntity);
	}

	/**
	 * 
	 * @param relativeUrl
	 *          The URL to post to.
	 * @param fileToPost
	 * 			The file to post.
	 * @param partName
	 * 			The name of the request parameter (the posted file) to bind to.
	 * @param responseTypeReference
	 *          The type reference of the response.
	 * @param <T> The type of the response.
	 * @return The response object from the REST server.
	 * @throws RestClientException
	 *             Reporting failure to post the file.
	 */
	public <T> T postFile(final String relativeUrl,
                          final File fileToPost,
			              final String partName,
                          final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
		final MultipartEntity multipartEntity = new MultipartEntity();
		final FileBody fileBody = new FileBody(fileToPost);
		multipartEntity.addPart(partName, fileBody);
		return post(relativeUrl, responseTypeReference, multipartEntity);

	}

    /**
     *
     * @param relativeUrl
     *          The URL to send the get request to.
     * @param responseTypeReference
     *          The type reference of the response.
     * @param <T> The type of the response.
     * @return The response object from the REST server.
     * @throws RestClientException .
     */
    public <T> T get(final String relativeUrl,
                     final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
        final HttpGet getRequest = new HttpGet(getFullUrl(relativeUrl));
        return executeRequest(getRequest, responseTypeReference);
    }

    /**
     *
     * @param relativeUrl
     *          The URL to send the delete request to.
     * @param responseTypeReference
     *          The type reference of the response.
     * @param <T> The type of the response.
     * @return The response object from the REST server.
     * @throws RestClientException .
     */
    public <T> T delete(final String relativeUrl,
                        final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
        final HttpDelete getRequest = new HttpDelete(getFullUrl(relativeUrl));
        return executeRequest(getRequest, responseTypeReference);
    }

	private <T> T post(final String relativeUrl,
                       final TypeReference<Response<T>> responseTypeReference,
			           final HttpEntity entity) throws RestClientException {
		final HttpPost postRequest = new HttpPost(getFullUrl(relativeUrl));
        if (entity instanceof StringEntity) {
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        }
		postRequest.setEntity(entity);
		return executeRequest(postRequest, responseTypeReference);
	}

    private static String getResponseBody(final HttpResponse response) throws IOException {

        InputStream instream = null;
        try {
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            instream = entity.getContent();
            return StringUtils.getStringFromStream(instream);
        } finally {
            if (instream != null) {
                instream.close();
            }
        }
    }

	private <T> T executeRequest(final HttpRequestBase request, 
			                     final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
		try {
			final HttpResponse httpResponse = httpClient.execute(request);
			checkForError(httpResponse);
			return getResponseObject(responseTypeReference, httpResponse);
		} catch (final IOException e) {
			throw new RestClientIOException(EXECUTION_FAILURE_CODE, 
					                        "Failed reading response object from response",
                                            e);
		} finally {
			request.abort();
		}
	}

	private void checkForError(final HttpResponse response) throws RestClientException {
		StatusLine statusLine = response.getStatusLine();
		final int statusCode = statusLine.getStatusCode();
		String reasonPhrase = statusLine.getReasonPhrase();
		String responseBody;
		if (statusCode != HttpStatus.SC_OK) {
			try {
				responseBody = getResponseBody(response);
            } catch (final IOException e) {
                // this means we couldn't transform the response into string, very unlikely
                throw new RestClientIOException(HTTP_FAILURE_CODE,
                                                "Failed reading response from server",
                                                e);
            }
            try {
                // this means we managed to read the response
                final Response<Void> entity = new ObjectMapper().readValue(responseBody, new TypeReference<Response<Void>>() {});
                // we also have the response in the proper format.
                // remember, we only got here because some sort of error happened on the server.
                throw new RestClientResponseException(entity.getMessageId(),
                                                      entity.getMessage(),
                                                      statusCode,
                                                      reasonPhrase,
                                                      entity.getVerbose());

            } catch (final IOException e) {
                // this means we got the response, but it is not in the correct format.
                // so some kind of error happened on the spring side.
                throw new RestClientHttpException(HTTP_FAILURE_CODE,
                                                  "Unexpected failure",
                                                  statusCode, reasonPhrase, responseBody, e);

            }
        }
	}

	private <T> T getResponseObject(final TypeReference<Response<T>> typeReference,
                                    final HttpResponse httpResponse) throws IOException {
		final String responseBody = getResponseBody(httpResponse);
		final Response<T> response = new ObjectMapper()
                .readValue(responseBody, typeReference);
		return response.getResponse();
	}

	/**
	 * Appends the given relative URL to the basic rest-service URL.
	 *
	 * @param relativeUrl
	 *            URL to add to the basic URL
	 * @return full URL as as String
	 */
	private String getFullUrl(final String relativeUrl) {
		String safeRelativeURL = relativeUrl;
		if (safeRelativeURL.startsWith(FORWARD_SLASH)) {
			safeRelativeURL = safeRelativeURL.substring(1);
		}
		return urlStr + safeRelativeURL;
	}

    public void setCredentials(final String username, final String password) {
        if (StringUtils.notEmpty(username) && StringUtils.notEmpty(password)) {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                    new UsernamePasswordCredentials(username, password));
        }
    }
}
