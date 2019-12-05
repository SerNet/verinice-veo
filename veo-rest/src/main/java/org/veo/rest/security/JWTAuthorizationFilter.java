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

import static org.veo.rest.security.SecurityConstants.HEADER_STRING;
import static org.veo.rest.security.SecurityConstants.TOKEN_PREFIX;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import io.jsonwebtoken.Jwts;

/**
 * This filters checks incoming request for JWT tokens and their validity.
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthorizationFilter.class);
    private Key verificationKey;
    private static final Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile("^"
            + TOKEN_PREFIX, Pattern.CASE_INSENSITIVE);

    public JWTAuthorizationFilter(AuthenticationManager authManager) {
        super(authManager);
        try {
            this.verificationKey = JwtKeyLoader.getPublicJwtKey();
        } catch (Exception e) {
            LOG.error("Error loading public key.", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
            FilterChain chain) throws IOException, ServletException {
        String header = req.getHeader(HEADER_STRING);

        if (header == null) {
            chain.doFilter(req, res);
            return;
        }
        Matcher m = AUTHORIZATION_HEADER_PATTERN.matcher(header);
        if (!m.find()) {
            chain.doFilter(req, res);
            return;
        }
        String user = parseUserFromToken(header.substring(m.end()));

        UsernamePasswordAuthenticationToken authentication = user != null
                ? new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
                : null;

        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    private String parseUserFromToken(String token) {
        return Jwts.parser()
                   .setSigningKey(verificationKey)
                   .parseClaimsJws(token)
                   .getBody()
                   .getSubject();
    }
}
