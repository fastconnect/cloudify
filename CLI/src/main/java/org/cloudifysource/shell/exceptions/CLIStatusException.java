/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.exceptions;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * @author noak
 * @since 2.0.0
 * 
 *        Extends {@link CLIException}, includes more details to support
 *        formatted messages. This exception is intended mainly for validations on the client side.
 *
 */
public class CLIStatusException extends CLIException {

	private static final long serialVersionUID = -399277091070772297L;

	private final String reasonCode;
	private final Object[] args;
	private final String verboseData;


	/**
	 * Constructor.
	 * 
	 * @param reasonCode
	 *            A reason code, by which a formatted message can be retrieved
	 *            from the message bundle
	 * @param args
	 *            Optional arguments to embed in the formatted message
	 */
	public CLIStatusException(final String reasonCode, final Object... args) {
		this.reasonCode = reasonCode;
		this.args = args;
        this.verboseData = null;
	}

    /**
     * Constructor.
     *
     * @param reasonCode
     *            A reason code, by which a formatted message can be retrieved
     *            from the message bundle
     * @param args
     *            Optional arguments to embed in the formatted message
     */
    public CLIStatusException(final Throwable cause, final String reasonCode, final Object... args) {
        this.reasonCode = reasonCode;
        this.args = args;
        this.verboseData = ExceptionUtils.getFullStackTrace(cause);
    }

	/**
	 * Gets the reason code.
	 * 
	 * @return A reason code, by which a formatted message can be retrieved from
	 *         the message bundle
	 */
	public String getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets the arguments that complete the reason-code based message.
	 * 
	 * @return An array of arguments
	 */
	public Object[] getArgs() {
		return args;
	}

	public String getVerboseData() {
		return verboseData;
	}
}
