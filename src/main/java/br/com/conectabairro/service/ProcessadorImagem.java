package br.com.conectabairro.service;

import br.com.conectabairro.exception.ArquivoInvalidoException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Prepara a foto enviada pelo lojista ANTES de ela ir para o banco de dados.
 *
 * Por que processar (e não guardar o arquivo como veio)?
 *  1) ESPAÇO: as fotos agora ficam no banco (que no plano grátis tem ~1 GB). Uma foto de celular
 *     tem 2-3 MB; redimensionada para no máximo 1280 px ela cai para ~100-250 KB.
 *  2) PRIVACIDADE: fotos de celular trazem o EXIF com a LOCALIZAÇÃO (GPS) de onde foram tiradas
 *     (ex.: a casa do comerciante). Ao regravar a imagem, todo esse metadado é descartado.
 *  3) SEGURANÇA: o arquivo é decodificado e regravado do zero, então qualquer conteúdo "escondido"
 *     dentro dele (arquivo disfarçado de imagem) não sobrevive.
 *  4) ROTAÇÃO: o celular guarda a foto "deitada" + uma marca (EXIF Orientation) dizendo como girar.
 *     Como o EXIF é descartado, nós aplicamos o giro antes, para a foto não ficar de lado.
 *
 * Usa só classes do próprio Java (ImageIO), sem bibliotecas extras. Sem dependências do Spring,
 * o que permite testar isoladamente.
 */
public final class ProcessadorImagem {

    /** Maior lado da imagem final, em pixels. */
    static final int LADO_MAXIMO = 1280;
    /** Recusa imagens gigantes (evita "imagem bomba"). Só o cabeçalho é lido para conferir isso. */
    static final int DIMENSAO_MAXIMA = 6000;
    /** Qualidade do JPEG (0 a 1). 0,82 é visualmente igual ao original e bem menor. */
    private static final float QUALIDADE_JPEG = 0.82f;
    private static final String MSG_FORMATO = "Envie uma imagem nos formatos JPG ou PNG.";

    private ProcessadorImagem() { }

    /** Resultado: os bytes prontos, a extensão (.jpg/.png) e o tipo para o navegador. */
    public record Resultado(byte[] bytes, String extensao, String contentType) { }

    /**
     * Valida e converte a imagem enviada. Lança ArquivoInvalidoException (mensagem amigável)
     * se não for um JPG/PNG legível ou se for grande demais.
     *
     * Saída: JPEG quando a imagem não tem transparência (bem menor); PNG quando tem
     * transparência (ex.: logotipo com fundo vazado), para não ganhar um fundo preto.
     */
    public static Resultado processar(byte[] original) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> leitores = (in == null)
                    ? java.util.Collections.<ImageReader>emptyIterator() : ImageIO.getImageReaders(in);
            if (!leitores.hasNext()) throw new ArquivoInvalidoException(MSG_FORMATO);

            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(in);
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                boolean jpeg = formato.equals("jpeg") || formato.equals("jpg");
                if (!jpeg && !formato.equals("png")) throw new ArquivoInvalidoException(MSG_FORMATO);

                int largura = leitor.getWidth(0), altura = leitor.getHeight(0);   // só lê o cabeçalho
                if (largura > DIMENSAO_MAXIMA || altura > DIMENSAO_MAXIMA)
                    throw new ArquivoInvalidoException("A imagem é grande demais (máximo 6000 x 6000 pixels).");

                // Decodifica já "encolhida" (pula pixels na leitura): uma foto de 12 megapixels
                // não precisa ser carregada inteira na memória para virar uma de 1280 px.
                ImageReadParam param = leitor.getDefaultReadParam();
                int passo = Math.max(1, Math.max(largura, altura) / LADO_MAXIMO);
                param.setSourceSubsampling(passo, passo, 0, 0);
                BufferedImage lida = leitor.read(0, param);

                int orientacao = jpeg ? lerOrientacaoExif(original) : 1;
                return gerarSaida(lida, orientacao);
            } finally {
                leitor.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ArquivoInvalidoException a) throw a;
            // arquivo corrompido, formato exótico (ex.: JPEG CMYK) ou qualquer falha de leitura
            throw new ArquivoInvalidoException("Não foi possível ler a imagem. Tente outra foto (JPG ou PNG).");
        }
    }

    /** Gira conforme o EXIF, reduz para o tamanho máximo e grava em JPEG ou PNG. */
    private static Resultado gerarSaida(BufferedImage origem, int orientacao) throws IOException {
        int w = origem.getWidth(), h = origem.getHeight();
        boolean transparente = origem.getColorModel().hasAlpha();

        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(w, h));   // nunca amplia
        boolean trocaLados = orientacao >= 5;                                   // 5..8 giram 90 graus
        int destW = Math.max(1, (int) Math.round((trocaLados ? h : w) * escala));
        int destH = Math.max(1, (int) Math.round((trocaLados ? w : h) * escala));

        BufferedImage destino = new BufferedImage(destW, destH,
                transparente ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            if (!transparente) {                    // JPEG não tem transparência: fundo branco por segurança
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, destW, destH);
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            AffineTransform t = AffineTransform.getScaleInstance(escala, escala);
            t.concatenate(transformacaoExif(orientacao, w, h));   // primeiro gira/espelha, depois reduz
            g.drawImage(origem, t, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        if (transparente) {
            if (!ImageIO.write(destino, "png", saida)) throw new IOException("Sem gravador PNG");
            return new Resultado(saida.toByteArray(), ".png", "image/png");
        }
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(saida)) {
            ImageWriteParam p = escritor.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(QUALIDADE_JPEG);
            escritor.setOutput(ios);
            escritor.write(null, new IIOImage(destino, null, null), p);   // metadata = null: sem EXIF/GPS
        } finally {
            escritor.dispose();
        }
        return new Resultado(saida.toByteArray(), ".jpg", "image/jpeg");
    }

    /**
     * Transformação que corrige cada valor de EXIF Orientation (1 a 8) para uma imagem w x h.
     * 1 = normal | 2 = espelhada | 3 = de cabeça para baixo | 4 = espelhada na vertical
     * 5 e 7 = espelhadas + 90 graus | 6 = girar 90 graus horário | 8 = girar 90 graus anti-horário.
     * (A ordem dos argumentos de AffineTransform é: m00, m10, m01, m11, m02, m12.)
     */
    static AffineTransform transformacaoExif(int orientacao, int w, int h) {
        return switch (orientacao) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();   // 1: nada a fazer
        };
    }

    /**
     * Lê a marca "Orientation" (EXIF, tag 0x0112) de um JPEG percorrendo os blocos do arquivo.
     * Devolve 1 (normal) se não existir ou se o arquivo estiver fora do padrão.
     */
    static int lerOrientacaoExif(byte[] b) {
        try {
            if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return 1;   // não é JPEG
            int pos = 2;
            while (pos + 4 <= b.length && (b[pos] & 0xFF) == 0xFF) {
                int marcador = b[pos + 1] & 0xFF;
                if (marcador == 0xDA || marcador == 0xD9) break;                   // começou a imagem: acabou o cabeçalho
                int tamanho = ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
                if (marcador == 0xE1 && tamanho >= 16
                        && b[pos + 4] == 'E' && b[pos + 5] == 'x' && b[pos + 6] == 'i' && b[pos + 7] == 'f') {
                    int tiff = pos + 10;                                           // início do bloco TIFF do EXIF
                    boolean pequenoFinal = b[tiff] == 'I';                         // "II" = little-endian, "MM" = big-endian
                    int ifd = tiff + (int) ler(b, tiff + 4, 4, pequenoFinal);
                    int entradas = (int) ler(b, ifd, 2, pequenoFinal);
                    for (int i = 0; i < entradas; i++) {
                        int entrada = ifd + 2 + i * 12;
                        if (ler(b, entrada, 2, pequenoFinal) == 0x0112) {
                            int valor = (int) ler(b, entrada + 8, 2, pequenoFinal);
                            return (valor >= 1 && valor <= 8) ? valor : 1;
                        }
                    }
                    return 1;
                }
                pos += 2 + tamanho;
            }
        } catch (RuntimeException e) {
            // arquivo malformado (índice fora do vetor etc.): trata como "sem rotação"
        }
        return 1;
    }

    /** Lê um número de 2 ou 4 bytes respeitando a ordem dos bytes (endianness) do EXIF. */
    private static long ler(byte[] b, int pos, int bytes, boolean pequenoFinal) {
        long v = 0;
        for (int i = 0; i < bytes; i++) {
            int byteAtual = b[pos + (pequenoFinal ? bytes - 1 - i : i)] & 0xFF;
            v = (v << 8) | byteAtual;
        }
        return v;
    }
}
