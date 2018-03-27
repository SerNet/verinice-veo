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
 *     Alexander Ben Nasrallah - initial API and implementation
 ******************************************************************************/
package org.veo.rest.security;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;

import static org.veo.rest.security.SecurityConstants.HEADER_STRING;
import static org.veo.rest.security.SecurityConstants.TOKEN_PREFIX;

/**
 * This filters checks incoming request for JWT tokens and their validity.
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTAuthorizationFilter.class);
    private Key verificationKey;

    public JWTAuthorizationFilter(AuthenticationManager authManager) {
        super(authManager);
        try {
            this.verificationKey = JwtKeyLoader.getPublicJwtKey();
        } catch (Exception e) {
            LOG.error("Error loading public key.", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        String header = req.getHeader(HEADER_STRING);

        if (header == null || !header.toLowerCase().startsWith(TOKEN_PREFIX.toLowerCase())) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(req);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String user = parseUserFromToken(request.getHeader(HEADER_STRING));
        if (user == null) {
            return null;
        }
        return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
    }

    private String parseUserFromToken(String token) {
        return Jwts.parser()
                    .setSigningKey(verificationKey)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody()
                    .getSubject();
    }
}
