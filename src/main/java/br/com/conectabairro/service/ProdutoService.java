package br.com.conectabairro.service;

import br.com.conectabairro.dto.ProdutoForm;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.*;
import br.com.conectabairro.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

/** Regras de negócio dos produtos e gerador de "Post de Divulgação". */
@Service @RequiredArgsConstructor
public class ProdutoService {
    private final ProdutoRepository repo;
    private final ComercioService comercios;
    private final ArmazenamentoService armazenamento;

    public List<Produto> listar(Long comercioId) { return repo.findByComercioIdOrderByNome(comercioId); }

    public Produto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
    }

    /**
     * Cadastro vindo do painel (formulário validado). Salva a imagem enviada (se houver) e grava
     * no banco o caminho dela (ex.: /uploads/uuid.jpg) na coluna foto_url.
     */
    public Produto cadastrar(Long comercioId, ProdutoForm f) {
        Comercio comercio = comercios.buscar(comercioId);          // 404 se o comércio não existir
        String foto = armazenamento.salvar(f.getFotoProduto());    // valida e grava a imagem (null se não enviou)
        Produto p = new Produto();
        p.setComercio(comercio);
        p.setNome(f.getNome().trim());
        p.setPreco(f.getPreco());
        p.setEstoque(comercio.isServico() ? 0 : f.getEstoque());   // serviço não tem estoque
        p.setDescricao(limpar(f.getDescricao()));
        p.setFotoUrl(foto);
        try {
            return repo.save(p);
        } catch (RuntimeException e) {
            armazenamento.remover(foto);   // se o banco falhar, não deixa arquivo "órfão" na pasta
            throw e;
        }
    }

    public Produto salvar(Long comercioId, Produto p) {
        p.setId(null);                                  // ignora id do JSON (não sobrescreve produto existente)
        p.setComercio(comercios.buscar(comercioId));   // vincula ao comércio da URL
        p.setFotoUrl(armazenamento.validarLinkExterno(p.getFotoUrl(), null));   // pela API: só link externo
        return repo.save(p);
    }

    /** Busca o produto SOMENTE se ele pertencer ao comércio informado (404 caso contrário). */
    public Produto buscarDoComercio(Long comercioId, Long produtoId) {
        return repo.findByIdAndComercioId(produtoId, comercioId)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
    }

    /**
     * ITEM 3: edição pelo painel. Se veio uma foto nova, salva a nova e apaga a antiga do disco;
     * se não veio, mantém a foto atual.
     */
    public Produto editar(Long comercioId, Long produtoId, ProdutoForm f) {
        Produto p = buscarDoComercio(comercioId, produtoId);
        String fotoAntiga = p.getFotoUrl();
        String fotoNova = armazenamento.salvar(f.getFotoProduto());   // null = nenhuma foto enviada

        p.setNome(f.getNome().trim());
        p.setPreco(f.getPreco());
        p.setEstoque(p.getComercio().isServico() ? 0 : f.getEstoque());   // serviço não tem estoque
        p.setDescricao(limpar(f.getDescricao()));
        if (fotoNova != null) p.setFotoUrl(fotoNova);                  // senão, mantém a antiga

        Produto salvo;
        try {
            salvo = repo.save(p);
        } catch (RuntimeException e) {
            armazenamento.remover(fotoNova);                           // sem arquivo órfão se o banco falhar
            throw e;
        }
        if (fotoNova != null) armazenamento.remover(fotoAntiga);       // troca concluída: apaga a antiga
        return salvo;
    }

    public Produto atualizar(Long id, Produto novo) {
        Produto p = buscar(id);
        p.setNome(novo.getNome());
        p.setPreco(novo.getPreco());
        p.setEstoque(novo.getEstoque());
        String fotoAntiga = p.getFotoUrl();
        p.setFotoUrl(armazenamento.validarLinkExterno(novo.getFotoUrl(), fotoAntiga));
        Produto salvo = repo.save(p);
        if (!Objects.equals(fotoAntiga, salvo.getFotoUrl())) armazenamento.remover(fotoAntiga);   // sem arquivo órfão
        return salvo;
    }

    /**
     * ETAPA 4: exclusão pelo painel. Só apaga se o produto pertencer ao comércio da URL
     * (antes, o lojista do comércio A podia apagar produto do comércio B trocando o id).
     */
    public void excluir(Long comercioId, Long id) {
        Produto p = repo.findByIdAndComercioId(id, comercioId)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
        repo.delete(p);
        armazenamento.remover(p.getFotoUrl());
    }

    public void excluir(Long id) {
        Produto p = buscar(id);
        repo.delete(p);
        armazenamento.remover(p.getFotoUrl());   // apaga também o arquivo da imagem
    }

    /** Texto vazio vira null (campo opcional não preenchido). */
    private String limpar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    /** Monta o texto pronto para colar no WhatsApp/Instagram (há um modelo para produtos e outro para serviços). */
    public String gerarPost(Long produtoId) {
        Produto p = buscar(produtoId);
        Comercio c = p.getComercio();
        // preço vazio (serviço "a combinar") não pode quebrar o texto do post
        String preco = p.getPreco() == null ? null
                : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(p.getPreco());
        String descricao = (p.getDescricao() == null || p.getDescricao().isBlank()) ? "" : p.getDescricao() + "\n";

        if (c.isServico()) {   // prestador de serviço: preço "a partir de" e convite para pedir orçamento
            return "🛠️ *" + c.getNome().toUpperCase() + "*\n\n"
                 + "✅ " + p.getNome() + (preco == null ? " (preço a combinar)" : " a partir de *" + preco + "*") + "\n"
                 + descricao
                 + "\n📍 " + c.getEndereco()
                 + "\n💬 Peça seu orçamento pelo WhatsApp: https://wa.me/" + c.getWhatsapp()
                 + "\n\n#ConectaBairro #ServiçosDoBairro";
        }
        // "Últimas unidades" só aparece quando ainda há de 1 a 5 em estoque
        String urgencia = (p.getEstoque() >= 1 && p.getEstoque() <= 5)
                ? "⚠️ Últimas " + p.getEstoque() + " unidades!\n" : "";
        return "🔥 *OFERTA NO " + c.getNome().toUpperCase() + "!* 🔥\n\n"
             + "👉 " + p.getNome() + (preco == null ? " (consulte o preço)" : " por apenas *" + preco + "*") + "\n"
             + descricao
             + urgencia
             + "\n📍 " + c.getEndereco()
             + "\n💬 Peça pelo WhatsApp: https://wa.me/" + c.getWhatsapp()
             + "\n\n#ConectaBairro #ComércioLocal";
    }
}
