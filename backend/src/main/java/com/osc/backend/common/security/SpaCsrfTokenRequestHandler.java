package com.osc.backend.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Single Page Application (SPA) CSRF token request handler recommended by Spring Security 6.
 * Always delegates to XorCsrfTokenRequestAttributeHandler to provide BREACH protection of the
 * CsrfToken when it is rendered into an HTML page or request attributes.
 * Resolves the raw CSRF token when supplied through the frontend HTTP header (e.g. X-XSRF-TOKEN).
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    public static final String RAW_CSRF_TOKEN_ATTR = "com.osc.backend.RAW_CSRF_TOKEN";

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        /*
         * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
         * the CsrfToken when it is rendered into an HTML page or request attributes.
         */
        this.delegate.handle(request, response, csrfToken);
        // Expose raw token supplier for REST token metadata endpoints
        request.setAttribute(RAW_CSRF_TOKEN_ATTR, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        /*
         * If the request contains a request header, use CsrfTokenRequestAttributeHandler
         * to resolve the raw value. Otherwise, delegate to XorCsrfTokenRequestAttributeHandler
         * to resolve the value (e.g. form parameter _csrf).
         */
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue))
                ? super.resolveCsrfTokenValue(request, csrfToken)
                : this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
