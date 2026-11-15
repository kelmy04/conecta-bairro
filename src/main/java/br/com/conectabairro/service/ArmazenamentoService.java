package br.com.conectabairro.service;

import br.com.conectabairro.exception.ArquivoInvalidoException;
import br.com.conectabairro.model.Imagem;
import br.com.conectabairro.repository.ImagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/**
 * Guarda as imagens enviadas pelos lojistas NO BANCO DE DADOS (tabela "imagem").
 * Nas tabelas de comércio e produto continua o CAMINHO público (ex.: /uploads/3f2a...jpg),
 * que o ImagemController entrega ao navegador. Os outros serviços não precisaram mudar.
 *
 * Por que no banco? Hospedagens gratuitas apagam o disco do site a cada publicação; o banco não.
 * Antes de gravar, o ProcessadorImagem reduz a foto (máx. 1280 px) e remove o EXIF (GPS), o que
 * mantém cada foto em ~100-250 KB e cabe muita coisa no plano gratuito do banco.
 */
@Service @RequiredArgsConstructor
public class ArmazenamentoService {
    private static final long TAMANHO_MAXIMO = 3L * 1024 * 1024;   // 3 MB por imagem enviada
    static final String PREFIXO = "/uploads/";                     // mesma rota do ImagemController

    private final ImagemRepository imagens;

    /**
     * Valida, reduz e salva a imagem. Devolve o caminho público (ex.: /uploads/uuid.jpg)
     * ou null se nenhum arquivo foi enviado (o campo da foto é opcional).
     */
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;

        if (arquivo.getSize() > TAMANHO_MAXIMO)
            throw new ArquivoInvalidoException("A imagem deve ter no máximo 3 MB.");

        byte[] original;
        try {
            original = arquivo.getBytes();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a imagem. Tente novamente.");
        }

        // confere o CONTEÚDO (um .exe renomeado para .jpg é recusado), corrige a rotação e reduz
        ProcessadorImagem.Resultado pronta = ProcessadorImagem.processar(original);

        String nome = UUID.randomUUID() + pronta.extensao();   // nome aleatório: nunca usamos o nome enviado
        imagens.save(new Imagem(nome, pronta.contentType(), pronta.bytes()));
        return PREFIXO + nome;
    }

    /**
     * API REST: o campo de foto só aceita um LINK externo (http/https) ou o valor que já estava
     * salvo. Sem isso, um usuário poderia gravar no SEU registro o caminho "/uploads/xxx.jpg" da
     * foto de OUTRO lojista e, ao excluir o próprio registro, apagar a imagem do outro.
     * Para enviar arquivo, usa-se a tela do sistema (upload validado).
     * Devolve o link normalizado (null se vazio).
     */
    public String validarLinkExterno(String link, String atual) {
        if (link == null || link.isBlank()) return null;
        link = link.trim();
        if (link.equals(atual)) return link;                       // não mudou
        if (!(link.startsWith("http://") || link.startsWith("https://")))
            throw new ArquivoInvalidoException("Pela API, a foto deve ser um link http(s). Para enviar arquivo, use a tela do sistema.");
        return link;
    }

    /**
     * Apaga uma imagem antiga (ao trocar a foto ou excluir o produto/comércio). Ignora links externos.
     *
     * REQUIRES_NEW: roda sempre em uma transação PRÓPRIA. Isso é necessário porque o
     * ComercioService chama este método de dentro do "afterCommit" (depois que a transação
     * principal já foi confirmada); sem uma transação nova, o DELETE não seria gravado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remover(String caminho) {
        if (caminho == null || !caminho.startsWith(PREFIXO)) return;   // links antigos (http...) não são nossos
        imagens.apagarPorNome(caminho.substring(PREFIXO.length()));
    }
}
