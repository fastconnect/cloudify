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
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * 
 * @author yael
 * 
 */
public class RestClientExecuter {

	private static final String FORWARD_SLASH = "/";

	private URL url;
	private String urlStr;

	public RestClientExecuter(final DefaultHttpClient httpClient, final URL url) {
		this.httpClient = httpClient;
		this.url = url;
		this.urlStr = url.toExternalForm();
		if (!this.urlStr.endsWith(FORWARD_SLASH)) {
			this.urlStr += FORWARD_SLASH;
		}
	}

	private final DefaultHttpClient httpClient;

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
	 * @throws RestClientException
	 *             Reporting failure to post the file.
	 * @throws TimeoutException .
	 * @throws IOException .
	 */
	public <T> T postObject(final String url, final Object postBody,
			final TypeReference<Response<T>> responseTypeReference)
					throws RestClientException, TimeoutException, IOException {
		final String jsonStr = new ObjectMapper().writeValueAsString(postBody);
		final HttpEntity stringEntity = new StringEntity(jsonStr, "UTF-8");
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
	 * @throws IOException .
	 * @throws RestClientException
	 *             Reporting failure to post the file.
	 * @throws TimeoutException .
	 */
	public <T> T postFile(final String relativeUrl, final File fileToPost, 
			final String partName, final TypeReference<Response<T>> responseTypeReference) 
					throws IOException, RestClientException, TimeoutException {
		final MultipartEntity multipartEntity = new MultipartEntity();
		final FileBody fileBody = new FileBody(fileToPost);
		multipartEntity.addPart(partName, fileBody);
		return post(relativeUrl, responseTypeReference, multipartEntity);

	}

	private <T> T post(final String relativeUrl, final TypeReference<Response<T>> responseTypeReference, 
			final HttpEntity entity) throws RestClientException, TimeoutException, IOException {
		final HttpPost postRequest = new HttpPost(getFullUrl(relativeUrl));
		//postRequest.setHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_HEADER_VALUE);
		postRequest.setEntity(entity);
		return executeRequest(postRequest, responseTypeReference);
	}

	/**
	 * 
	 * @param relativeUrl
	 *          The URL to send the get request to.
	 * @param responseTypeReference
	 *          The type reference of the response.
	 * @param <T> The type of the response.
	 * @return The response object from the REST server.
	 * @throws IOException .
	 * @throws RestClientException .
	 */
	public <T> T get(final String relativeUrl, final TypeReference<Response<T>> responseTypeReference) 
			throws IOException, RestClientException {
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
	 * @throws IOException .
	 * @throws RestClientException .
	 */
	public <T> T delete(final String relativeUrl, final TypeReference<Response<T>> responseTypeReference) 
			throws IOException, RestClientException {
		final HttpDelete getRequest = new HttpDelete(getFullUrl(relativeUrl));
		return executeRequest(getRequest, responseTypeReference);
	}

	private <T> T executeRequest(final HttpRequestBase request, 
			final TypeReference<Response<T>> responseTypeReference)
					throws IOException, RestClientException {
		try {
			final HttpResponse httpResponse = httpClient.execute(request);
			checkForError(httpResponse);
			return getResponseObject(responseTypeReference, httpResponse);
		} finally {
			request.abort();
		}
	}

	private void checkForError(final HttpResponse response)
			throws RestClientException, IOException {
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != CloudifyConstants.HTTP_STATUS_CODE_OK) {
			final String responseBody = getResponseBody(response);
			final Response<Void> entity = 
					new ObjectMapper().readValue(responseBody, new TypeReference<Response<Void>>() { });
			String messageId = entity.getMessageId();
			throw new RestClientException(response.getStatusLine().getStatusCode(), entity.getMessage());
//			throw new RestClientException(
//					response.getStatusLine().getStatusCode(),
//					response.getStatusLine().getReasonPhrase());
		}
	}

	private <T> T getResponseObject(final TypeReference<Response<T>> typeReference, final HttpResponse httpResponse)
			throws IOException {
		final String responseBody = getResponseBody(httpResponse);
		final Response<T> response = 
				new ObjectMapper().readValue(responseBody, typeReference);
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

	/**
	 * Gets the HTTP response's body as a String.
	 * 
	 * @param response
	 *            The HttpResponse object to analyze
	 * @return the body of the given HttpResponse object, as a string
	 * @throws IOException
	 *             Reporting a failure to read the response's content
	 */
	public static String getResponseBody(final HttpResponse response)
			throws IOException {

		InputStream instream = null;
		try {
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				// final RestClientException e =
				// new RestClientException(
				// CloudifyErrorMessages.RESPONSE_ENTITY_NULL.getName(),
				// httpMethod.getURI());
				// logger.log(Level.FINE, CloudifyErrorMessages.RESPONSE_ENTITY_NULL.getName(), e);
				// throw e;
				return null;
			}
			instream = entity.getContent();
			return StringUtils.getStringFromStream(instream);
		} finally {
			if (instream != null) {
				try {
					instream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
