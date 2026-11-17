package br.com.conectabairro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras básicas de acesso (Etapa 3):
 *  - PÚBLICO: vitrine ("/"), página de cada comércio ("/comercio/**", inclusive agendar),
 *    CSS, tela de login, página de erro e consultas (GET) da API.
 *  - EXIGE LOGIN: painel do lojista ("/painel/**"), cadastro de comércio e qualquer escrita
 *    (POST/PUT/DELETE) na API.
 * PRÓXIMO PASSO: conferir se o lojista logado é o DONO do comércio da URL (@PreAuthorize).
 * O CSRF fica ativo (padrão): os formulários com th:action já enviam o token sozinhos.
 */
@Configuration
@EnableMethodSecurity   // ETAPA 4: habilita @PreAuthorize (trava de dono no PainelController)
public class SecurityConfig {

    /** BCrypt: guarda senhas como hash com "sal"; não dá para recuperar a senha original. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                // Editar e excluir comércio exigem login e PRECISA vir antes da regra pública de /comercio/**
                .requestMatchers("/comercio/*/editar", "/comercio/*/excluir").authenticated()
                .requestMatchers("/", "/comercio/**", "/css/**", "/img/**", "/uploads/**", "/login", "/registrar", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()   // somente consultas
                .requestMatchers("/painel/**").authenticated()
                .anyRequest().authenticated())                            // /cadastrar, escritas da API...
            .formLogin(f -> f
                .loginPage("/login")             // nossa tela em português (templates/login.html)
                .defaultSuccessUrl("/meu-painel")   // ETAPA 2: após o login, o lojista cai em "Meus comércios"
                .permitAll())
            .logout(l -> l.logoutSuccessUrl("/"));
        return http.build();
    }
}
