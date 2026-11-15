package br.com.conectabairro.config;

import br.com.conectabairro.model.CategoriaComercio;
import br.com.conectabairro.model.CategoriaComercio.TipoPainel;
import br.com.conectabairro.repository.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Garante que as categorias do sistema existam (cria só as que faltarem). Assim, ao atualizar o
 * projeto, categorias novas (ex.: Cabeleireiro(a)) aparecem sem precisar rodar SQL no banco.
 * Roda a cada inicialização e não altera categorias que já existem.
 */
@Configuration
public class DadosIniciaisConfig {

    @Bean
    CommandLineRunner garantirCategorias(CategoriaRepository repo) {
        return args -> {
            garantir(repo, "Mercado", TipoPainel.PRODUTOS, null);
            garantir(repo, "Açougue", TipoPainel.PRODUTOS, null);
            garantir(repo, "Pet Shop", TipoPainel.PRODUTOS, null);
            garantir(repo, "Loja de Roupas", TipoPainel.PRODUTOS, null);
            garantir(repo, "Barbearia", TipoPainel.AGENDAMENTO, "Corte;Barba;Corte + Barba");
            garantir(repo, "Cabeleireiro(a)", TipoPainel.AGENDAMENTO, "Corte;Escova;Coloração;Hidratação;Penteado");
        };
    }

    private void garantir(CategoriaRepository repo, String nome, TipoPainel tipo, String servicos) {
        if (repo.findByNome(nome).isPresent()) return;
        CategoriaComercio c = new CategoriaComercio();
        c.setNome(nome);
        c.setTipoPainel(tipo);
        c.setServicos(servicos);
        repo.save(c);
    }
}
