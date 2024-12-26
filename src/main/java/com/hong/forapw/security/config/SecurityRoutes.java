package com.hong.forapw.security.config;

public class SecurityRoutes {

    private SecurityRoutes() {
    }

    protected static final String[] AUTHENTICATED_ROUTES = {
            "/api/accounts/profile",
            "/api/accounts/password/**",
            "/api/accounts/role",
            "/api/accounts/withdraw",
            "/api/shelters/import",
            "/api/animals/like",
            "/api/accounts/withdraw/code",
            "/api/animals/*/like",
            "/api/animals/*/apply"
    };

    protected static final String[] PUBLIC_ROUTES = {
            "/api/groups/*/detail",
            "/api/chat/*/read",
            "/ws/**",
            "/api/auth/**",
            "/api/accounts/**",
            "/api/animals/**",
            "/api/shelters/**",
            "/api/groups",
            "/api/groups/local",
            "/api/groups/new",
            "/api/home",
            "/api/search/**",
            "/api/validate/accessToken",
            "/api/posts/adoption",
            "/api/posts/fostering",
            "/api/posts/question",
            "/api/posts/popular",
            "/api/groups/localAndNew",
            "/api/faq",
            "/shelters/addr"
    };
}
