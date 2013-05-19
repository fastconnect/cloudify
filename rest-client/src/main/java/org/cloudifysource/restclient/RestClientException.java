package org.cloudifysource.restclient;

public class RestClientException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int status;
	private final String message;
	private final Object[] args;
	
	public RestClientException(final int status, final String message, final Object... args) {
		this.message = message;
		this.status = status;
		this.args = args;
	}
	
	public RestClientException(final String message, final Object... args) {
		this.message = message;
		this.args = args;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public Object[] getArgs() {
		return args;
	}
}
