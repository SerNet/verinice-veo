/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Daniel Murygin.
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
package org.veo.core.usecase.common;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** This class provides methods to manage ETags, see: https://en.wikipedia.org/wiki/HTTP_ETag. */
public final class ETag {

  public static final String SHA256_ALGORITHM = "SHA-256";

  private static String salt;

  private ETag() {
    // do not instantiate, use public static methods
  }

  public static String from(String id, long version) {
    try {
      return "\"" + createSHA256Hash(id + "_" + salt + "_" + version) + "\"";
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String from(String compoundId1, String compundId2, long version) {
    return from(compoundId1 + "_" + compundId2, version);
  }

  public static boolean matches(String id, long version, String eTag) {
    if (id == null || eTag == null) {
      return false;
    }
    String hash = from(id, version);
    return hash.equalsIgnoreCase(padEtagIfNecessary(eTag));
  }

  private static String padEtagIfNecessary(String etag) {
    if (etag.isEmpty()) {
      return etag;
    }
    if (etag.startsWith("\"") && etag.endsWith("\"")) {
      return etag;
    }
    return "\"" + etag + "\"";
  }

  public static boolean matches(String compoundId1, String compundId2, long version, String eTag) {
    return matches(compoundId1 + "_" + compundId2, version, eTag);
  }

  private static String createSHA256Hash(String s) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
    byte[] encodedhash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
    BigInteger bigInteger = new BigInteger(1, encodedhash);
    // https://www.baeldung.com/java-byte-arrays-hex-strings#using-thebiginteger-class
    return String.format("%0" + (encodedhash.length << 1) + "x", bigInteger);
  }

  public static void setSalt(String salt) {
    if (salt == null || salt.length() == 0) {
      throw new IllegalArgumentException("Salt must not be empty");
    }
    ETag.salt = salt;
  }
}
