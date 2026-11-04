package com.jobtracker.common;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** Id de l'utilisateur connecte, lu dans le claim "uid" du JWT. */
    public static Long id() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Object uid = jwt.getClaim("uid");
        return ((Number) uid).longValue();
    }
}
