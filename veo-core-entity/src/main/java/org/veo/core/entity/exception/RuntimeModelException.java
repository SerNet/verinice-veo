/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity.exception;

/**
 * @author urszeidler
 */
public class RuntimeModelException extends RuntimeException {

    private static final long serialVersionUID = -4909523663112766890L;

    public RuntimeModelException() {
        super();
    }

    public RuntimeModelException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RuntimeModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeModelException(String message) {
        super(message);
    }

    public RuntimeModelException(Throwable cause) {
        super(cause);
    }

}
