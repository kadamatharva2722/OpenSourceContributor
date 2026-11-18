package com.osc.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that forces resolution of the deferred CsrfToken on every request,
 * guaranteeing that the cookie-based CSRF token (XSRF-TOKEN) is set on the response
 * for client-side JavaScript applications.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Render the token value to the cookie by causing the deferred token to be loaded
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
