package org.cloudifysource.restclient;

import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.codehaus.jackson.type.TypeReference;

public class ResponseTypeReferenceFactory {

	public static TypeReference<Response<ServiceDetails>> newServiceDetailsResponse() {
		return new TypeReference<Response<ServiceDetails>>() {};
	}
	
	public static TypeReference<Response<Void>> newVoidResponse() {
		return new TypeReference<Response<Void>>() {};
	}
	

}
