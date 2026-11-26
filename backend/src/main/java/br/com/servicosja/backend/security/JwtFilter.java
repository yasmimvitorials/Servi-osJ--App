package br.com.servicosja.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//Lê o header "Authorization: Bearer <token>" e autentica o usuário na requisição.//

public class JwtFilter extends OncePerRequestFilter {
	private final TokenService tokenService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtFilter(TokenService tokenService, UsuarioDetailsService usuarioDetailsService) {
        this.tokenService = tokenService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (tokenService.valido(token)) {
                try {
                    UserDetails detalhes = usuarioDetailsService.loadUserByUsername(tokenService.extrairEmail(token));
                    if (detalhes.isEnabled()) {
                        UsernamePasswordAuthenticationToken autenticacao =
                                new UsernamePasswordAuthenticationToken(detalhes, null, detalhes.getAuthorities());
                        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    }
                } catch (UsernameNotFoundException e) {
                    // usuário do token não existe mais: segue sem autenticar (resultará em 401)
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
