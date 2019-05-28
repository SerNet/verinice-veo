/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.commons;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Exception class used for veo.
 *
 * The message of a class can be parameterized using a Map<string, string>. If
 * the key of the map is found in the message surrounded by '%' it will be
 * replaced by the value. E.g.
 *
 * message: "error on element with uuid %uuid%" map: [ "uuid": "deadbeef" ]
 *
 * results in the message: "error on element with uuid deadbeef"
 *
 * The parameters map could be passed to a client for client side error/message
 * localization.
 */
public class VeoException extends RuntimeException {

    private static final long serialVersionUID = 5687387925339897969L;
    private static final String DELIMETER = "%";

    private final Map<String, String> parameters;
    private final Error error;

    public VeoException(Error error, String message) {
        this(error, message, (Map<String, String>) null);
    }

    public VeoException(Error error, String message, Map<String, String> params) {
        super(message);
        this.error = error;
        this.parameters = params;
    }

    public VeoException(Error error, String message, Throwable cause) {
        this(error, message, null, cause);
    }

    public VeoException(Error error, String message, Map<String, String> params, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.parameters = params;
    }

    /**
     * Convenient constructor, which creates a Map of length 1 using the key and
     * value.
     *
     * @param message
     *            parameterized message
     * @param key
     *            key for the parameter map
     * @param value
     *            value for the parameter map
     */
    public VeoException(Error error, String message, String key, String value, Throwable cause) {
        this(error, message, Collections.singletonMap(key, value), cause);
    }

    /**
     * Convenient constructor, which creates a Map of length 1 using the key and
     * value.
     *
     * @param message
     *            parameterized message
     * @param key
     *            key for the parameter map
     * @param value
     *            value for the parameter map
     */
    public VeoException(Error error, String message, String key, String value) {
        this(error, message, Collections.singletonMap(key, value));
    }

    public Error getError() {
        return error;
    }

    @Override
    public String getMessage() {
        if (parameters == null) {
            return super.getMessage();
        }
        String message = super.getMessage();
        for (Entry<String, String> param : parameters.entrySet()) {
            message = message.replaceAll(DELIMETER + param.getKey() + DELIMETER, param.getValue());
        }
        return message;
    }

    public enum Error {
        ELEMENT_NOT_FOUND, ELEMENT_EXISTS, AUTHENTICATION_REQUIRED, UNKNOWN, AUTHENTICATION_ERROR
    }
}
