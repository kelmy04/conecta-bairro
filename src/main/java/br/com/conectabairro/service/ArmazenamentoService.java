package br.com.conectabairro.service;

import br.com.conectabairro.exception.ArquivoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * Guarda as imagens enviadas pelos lojistas na pasta "uploads/" (na raiz do projeto, fora do src).
 * No banco gravamos apenas o CAMINHO público (ex.: /uploads/3f2a...jpg), nunca o arquivo.
 *
 * Por que fora do "src"? Tudo que fica em src/main/resources é empacotado na hora do build;
 * arquivos enviados com o sistema já rodando não entrariam e se perderiam a cada novo build.
 */
@Service
public class ArmazenamentoService {
    private static final long TAMANHO_MAXIMO = 3L * 1024 * 1024;   // 3 MB por imagem
    private static final int DIMENSAO_MAXIMA = 6000;               // px; evita "imagem bomba" que consome memória
    private static final String PREFIXO = "/uploads/";             // mesma rota exposta no WebConfig
    private static final String MSG_FORMATO = "Envie uma imagem nos formatos JPG ou PNG.";

    private final Path raiz;

    public ArmazenamentoService(@Value("${app.upload-dir:uploads}") String pasta) {
        this.raiz = Path.of(pasta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);   // cria a pasta na primeira execução
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de uploads: " + raiz, e);
        }
    }

    /**
     * Valida e salva a imagem. Devolve o caminho público (ex.: /uploads/uuid.jpg)
     * ou null se nenhum arquivo foi enviado (campo da foto é opcional).
     */
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) return null;

        if (arquivo.getSize() > TAMANHO_MAXIMO)
            throw new ArquivoInvalidoException("A imagem deve ter no máximo 3 MB.");

        String extensao = detectarExtensao(arquivo);
        String nome = UUID.randomUUID() + extensao;   // nome aleatório: nunca usamos o nome enviado pelo usuário

        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, raiz.resolve(nome));
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível salvar a imagem. Tente novamente.");
        }
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

    /** Apaga uma imagem antiga (ao trocar a foto ou excluir o produto). Ignora links externos. */
    public void remover(String caminho) {
        if (caminho == null || !caminho.startsWith(PREFIXO)) return;   // links antigos (http...) não são nossos
        Path alvo = raiz.resolve(caminho.substring(PREFIXO.length())).normalize();
        if (!alvo.startsWith(raiz)) return;                            // segurança: nunca sair da pasta uploads
        try {
            Files.deleteIfExists(alvo);
        } catch (IOException ignorado) {
            // falha ao apagar não deve impedir a operação principal
        }
    }

    /**
     * Confere o CONTEÚDO do arquivo (e não só o que o navegador informou): um .exe renomeado
     * para .jpg é recusado. Lê apenas o cabeçalho, sem carregar a imagem inteira na memória.
     */
    private String detectarExtensao(MultipartFile arquivo) {
        try (ImageInputStream in = ImageIO.createImageInputStream(arquivo.getInputStream())) {
            Iterator<ImageReader> leitores = (in == null)
                    ? Collections.<ImageReader>emptyIterator() : ImageIO.getImageReaders(in);
            if (!leitores.hasNext()) throw new ArquivoInvalidoException(MSG_FORMATO);

            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(in);
                if (leitor.getWidth(0) > DIMENSAO_MAXIMA || leitor.getHeight(0) > DIMENSAO_MAXIMA)
                    throw new ArquivoInvalidoException("A imagem é grande demais (máximo 6000 x 6000 pixels).");
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                return switch (formato) {
                    case "jpeg", "jpg" -> ".jpg";
                    case "png" -> ".png";
                    default -> throw new ArquivoInvalidoException(MSG_FORMATO);
                };
            } finally {
                leitor.dispose();
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a imagem. Verifique o arquivo.");
        }
    }
}
