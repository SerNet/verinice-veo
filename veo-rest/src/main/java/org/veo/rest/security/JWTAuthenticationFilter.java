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
package org.veo.rest.security;

import static org.veo.rest.security.SecurityConstants.EXPIRATION_TIME;
import static org.veo.rest.security.SecurityConstants.HEADER_STRING;
import static org.veo.rest.security.SecurityConstants.TOKEN_PREFIX;

import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.veo.commons.VeoException;

/**
 * This filter extends authentication by added a JWToken to a successful
 * authentication response.
 */
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthenticationFilter.class);
    private final AuthenticationManager authenticationManager;
    private Key signingKey;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        try {
            this.signingKey = JwtKeyLoader.getPrivateJwtKey();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.error("Error loading private key.", e);
        }
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) {
        try {
            ApplicationUser credentials = new ObjectMapper().readValue(req.getInputStream(),
                                                                       ApplicationUser.class);

            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    credentials.getUsername(), credentials.getPassword(), new ArrayList<>()));
        } catch (IOException e) {
            throw new VeoException(VeoException.Error.AUTHENTICATION_ERROR,
                    "unable to read credentials");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res,
            FilterChain chain, Authentication auth) {

        Map<String, Object> customClaim = new HashMap<>();
        // Add some example claims to play with whats possible.
        customClaim.put("profiles", new String[] { "export", "import", "tasks" });
        String token = Jwts.builder()
                           .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                           .setSubject(((User) auth.getPrincipal()).getUsername())
                           .setIssuer("verinice.VEO")
                           .setIssuedAt(new Date())
                           .setAudience("verinice.REST clients")
                           .addClaims(customClaim)
                           .signWith(signingKey, SignatureAlgorithm.RS512)
                           .compact();
        res.addHeader(HEADER_STRING, TOKEN_PREFIX + token);
    }
}
