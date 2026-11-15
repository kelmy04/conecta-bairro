package br.com.conectabairro.service;

import br.com.conectabairro.dto.RegistroForm;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.model.Usuario;
import br.com.conectabairro.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Regras de negócio da conta do lojista: criar conta e excluir conta. */
@Service @RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final ComercioService comercios;

    /** ETAPA 1: cria a conta. A senha é guardada como hash BCrypt, nunca em texto puro. */
    @Transactional
    public Usuario registrar(RegistroForm f) {
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);   // padroniza o e-mail
        if (repo.findByEmailIgnoreCase(email).isPresent())
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");

        Usuario u = new Usuario();
        u.setNome(f.getNome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(f.getSenha()));
        try {
            return repo.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {   // dois cadastros simultâneos com o mesmo e-mail
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
    }

    /**
     * ETAPA 2: exclui a conta. O JPA remove em CASCATA os comércios do usuário e, por sua vez,
     * os produtos e agendamentos de cada um (ver @OneToMany(cascade = REMOVE) nas entidades).
     * As imagens só são apagadas do disco depois que o banco confirma a exclusão.
     */
    @Transactional
    public void excluirConta(String email) {
        Usuario u = repo.findByEmail(email)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        List<String> fotos = new ArrayList<>();
        for (Comercio c : u.getComercios()) fotos.addAll(comercios.coletarFotos(c));
        repo.delete(u);                                  // cascata: comércios -> produtos e agendamentos
        comercios.removerArquivosAposCommit(fotos);
    }
}
