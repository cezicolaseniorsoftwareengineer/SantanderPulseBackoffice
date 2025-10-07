package com.santander.pulse.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Stores OAuth2 authorization requests in secure HTTP-only cookies so the flow
 * works even when server-side sessions are disabled.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String OAUTH2_REDIRECT_URI_COOKIE_NAME = "oauth2_redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        CookieUtils.addCookie(
                response,
                OAUTH2_AUTH_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS
        );

        String redirectUriAfterLogin = request.getParameter("redirect_uri");
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtils.addCookie(
                    response,
                    OAUTH2_REDIRECT_URI_COOKIE_NAME,
                    redirectUriAfterLogin,
                    COOKIE_EXPIRE_SECONDS
            );
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, OAUTH2_REDIRECT_URI_COOKIE_NAME);
    }
}