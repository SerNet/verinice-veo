/*******************************************************************************
 * Copyright (c) 2020 Daniel Murygin.
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
package org.veo.core.usecase.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * This class provides methods to manage ETags, see:
 * https://en.wikipedia.org/wiki/HTTP_ETag.
 */
public final class ETag {

    public static final String SHA256_ALGORITHM = "SHA-256";

    private static String salt = "gN8Yk<w8fV3WucF";

    private ETag() {
        // do not instantiate, use public static methods
    }

    public static final String from(String id, long version) {
        try {
            String hash = createSHA256Hash(id + "_" + salt + "_" + Long.toString(version));
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final boolean matches(String id, long version, String eTag) {
        if (id == null || eTag == null) {
            return false;
        }
        String hash = from(id, version);
        return hash.equalsIgnoreCase(eTag);
    }

    private static String createSHA256Hash(String s) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
        byte[] encodedhash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return DatatypeConverter.printHexBinary(encodedhash)
                                .toLowerCase();
    }

    public static void setSalt(String salt) {
        ETag.salt = salt;
    }

}
