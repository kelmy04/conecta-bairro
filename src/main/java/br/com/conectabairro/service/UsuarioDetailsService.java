package br.com.conectabairro.service;

import br.com.conectabairro.model.Usuario;
import br.com.conectabairro.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ensina o Spring Security a buscar o lojista na tabela "usuario" pelo e-mail.
 * O Spring compara a senha digitada com o hash BCrypt guardado em senha_hash.
 */
@Service @RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        return User.withUsername(u.getEmail())
                .password(u.getSenhaHash())     // hash BCrypt (nunca a senha em texto puro)
                .roles("LOJISTA")
                .build();
    }
}
