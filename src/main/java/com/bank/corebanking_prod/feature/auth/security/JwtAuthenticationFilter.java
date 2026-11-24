package com.bank.corebanking_prod.feature.auth.security;

import com.bank.corebanking_prod.feature.auth.service.JwtTokenService;
import com.bank.corebanking_prod.feature.auth.service.UserIdentityDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserIdentityDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserIdentityDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        try {
            if (StringUtils.hasText(token) && jwtTokenService.validateToken(token)) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(
                            jwtTokenService.getUsernameFromToken(token)
                    );
                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (UsernameNotFoundException ignored) {
                    // A removed identity invalidates its access tokens; protected routes return 401.
                }
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // The token may expire between validation and claim extraction; treat it as unauthenticated.
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader)
                && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
