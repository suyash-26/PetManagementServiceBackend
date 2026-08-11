package com.example.petManagementService.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Mirrors authService's JwtAuthenticationFilter, but the principal is built straight
// from the token's claims instead of loaded via a UserDetailsService — Core has no
// users table to load from. A valid signature + unexpired token is the whole trust
// boundary; there is no round trip back to authService on the hot path.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearerToken.substring(7);

        if (SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);
            Long id = jwtService.extractClaim(token, "id", Long.class);
            String name = jwtService.extractClaim(token, "name", String.class);
            String role = jwtService.extractClaim(token, "role", String.class);

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(id, email, name, role);

            // Mirrors authService's CustomUserDetails.getAuthorities(): ROLE_<role> built
            // straight off the claim, since there's no local user row to derive it from.
            // Tokens issued before the role claim existed (or with a null role) simply
            // authenticate with no authorities — @PreAuthorize role checks deny them.
            List<GrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
