package br.com.conectabairro.config;

import br.com.conectabairro.model.CategoriaComercio;
import br.com.conectabairro.model.CategoriaComercio.TipoPainel;
import br.com.conectabairro.repository.CategoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Garante que as categorias do sistema existam (cria só as que faltarem). Assim, ao atualizar o
 * projeto, categorias novas aparecem sem precisar rodar SQL no banco.
 * Roda a cada inicialização e não altera categorias que já existem.
 */
@Configuration
public class DadosIniciaisConfig {
    private static final Logger log = LoggerFactory.getLogger(DadosIniciaisConfig.class);

    @Bean
    CommandLineRunner garantirCategorias(CategoriaRepository repo, JdbcTemplate jdbc) {
        return args -> {
            liberarTipoServicos(jdbc);   // 1º: o banco precisa aceitar o novo tipo SERVICOS

            garantir(repo, "Mercado", TipoPainel.PRODUTOS, null);
            garantir(repo, "Açougue", TipoPainel.PRODUTOS, null);
            garantir(repo, "Pet Shop", TipoPainel.PRODUTOS, null);
            garantir(repo, "Loja de Roupas", TipoPainel.PRODUTOS, null);
            // Alimentação e produção: usam o painel de PRODUTOS (cardápio / catálogo com foto e preço)
            garantir(repo, "Indústria", TipoPainel.PRODUTOS, null);
            garantir(repo, "Lanchonete", TipoPainel.PRODUTOS, null);
            garantir(repo, "Bar", TipoPainel.PRODUTOS, null);
            garantir(repo, "Restaurante", TipoPainel.PRODUTOS, null);
            garantir(repo, "Barbearia", TipoPainel.AGENDAMENTO, "Corte;Barba;Corte + Barba");
            garantir(repo, "Cabeleireiro(a)", TipoPainel.AGENDAMENTO, "Corte;Escova;Coloração;Hidratação;Penteado");
            // Serviços terceirizados: prestadores com lista de serviços (preço "a partir de") e orçamento pelo WhatsApp
            garantir(repo, "Eletricista", TipoPainel.SERVICOS, null);
            garantir(repo, "Encanador", TipoPainel.SERVICOS, null);
            garantir(repo, "Diarista e Limpeza", TipoPainel.SERVICOS, null);
            garantir(repo, "Pedreiro e Reformas", TipoPainel.SERVICOS, null);
            garantir(repo, "Pintor", TipoPainel.SERVICOS, null);
            garantir(repo, "Jardinagem", TipoPainel.SERVICOS, null);
            garantir(repo, "Técnico de Informática e Celular", TipoPainel.SERVICOS, null);
            garantir(repo, "Frete e Mudanças", TipoPainel.SERVICOS, null);
            garantir(repo, "Outros Serviços", TipoPainel.SERVICOS, null);
        };
    }

    private void garantir(CategoriaRepository repo, String nome, TipoPainel tipo, String servicos) {
        try {
            if (repo.findByNome(nome).isPresent()) return;
            CategoriaComercio c = new CategoriaComercio();
            c.setNome(nome);
            c.setTipoPainel(tipo);
            c.setServicos(servicos);
            repo.save(c);
        } catch (Exception e) {
            // Um problema numa categoria não pode impedir o site de abrir
            log.warn("Não foi possível criar a categoria {}: {}", nome, e.getMessage());
        }
    }

    /**
     * Bancos criados antes deste recurso têm uma restrição (CHECK) que só aceita PRODUTOS e AGENDAMENTO,
     * e o Hibernate não a atualiza sozinho. Este comando troca a restrição para aceitar também SERVICOS.
     * É seguro repetir: se a restrição já aceita SERVICOS, nada acontece.
     */
    private void liberarTipoServicos(JdbcTemplate jdbc) {
        try {
            jdbc.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint
                                  WHERE conrelid = 'categoria_comercio'::regclass AND contype = 'c'
                                    AND pg_get_constraintdef(oid) ILIKE '%SERVICOS%') THEN
                    FOR r IN SELECT conname FROM pg_constraint
                              WHERE conrelid = 'categoria_comercio'::regclass AND contype = 'c'
                                AND pg_get_constraintdef(oid) ILIKE '%tipo_painel%' LOOP
                      EXECUTE 'ALTER TABLE categoria_comercio DROP CONSTRAINT ' || quote_ident(r.conname);
                    END LOOP;
                    ALTER TABLE categoria_comercio ADD CONSTRAINT categoria_comercio_tipo_painel_check
                      CHECK (tipo_painel IN ('PRODUTOS', 'AGENDAMENTO', 'SERVICOS'));
                  END IF;
                END $$;
                """);
        } catch (Exception e) {
            log.warn("Não foi possível atualizar a restrição de tipo_painel: {}", e.getMessage());
        }
    }
}
