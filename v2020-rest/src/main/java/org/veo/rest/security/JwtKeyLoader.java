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
 *
 * Contributors:
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.rest.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class provides access to public and private keys in resources.
 */
public class JwtKeyLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JwtKeyLoader.class);
    private static final String PUBLIC_KEY_FILENAME = "public.der";
    private static final String PRIVATE_KEY_FILENAME = "private.der";

    private JwtKeyLoader() {
    }

    public static PublicKey getPublicJwtKey()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] keyBytes = getBytesFromResource(PUBLIC_KEY_FILENAME);

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey getPrivateJwtKey()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] keyBytes = getBytesFromResource(PRIVATE_KEY_FILENAME);

        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static byte[] getBytesFromResource(String filename) throws IOException {
        ClassPathResource r = new ClassPathResource(filename);
        try (InputStream openStream = r.getInputStream()) {
            DataInputStream dis = new DataInputStream(openStream);
            int contentLength = openStream.available();
            byte[] keyBytes = new byte[contentLength];
            dis.readFully(keyBytes);
            return keyBytes;
        }
    }
}
