package br.com.conectabairro.service;

import br.com.conectabairro.dto.ComercioForm;
import br.com.conectabairro.exception.ExpedienteInvalidoException;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.Agendamento;
import br.com.conectabairro.model.CategoriaComercio;
import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.repository.AgendamentoRepository;
import br.com.conectabairro.repository.CategoriaRepository;
import br.com.conectabairro.repository.ComercioRepository;
import br.com.conectabairro.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** Regras de negócio dos comércios (CRUD + agrupamento para a vitrine). */
@Service @RequiredArgsConstructor
public class ComercioService {
    private final ComercioRepository repo;
    private final CategoriaRepository categorias;
    private final AgendamentoRepository agendamentos;
    private final UsuarioRepository usuarios;
    private final ArmazenamentoService armazenamento;

    public List<Comercio> listar() { return repo.findAll(); }

    /** ETAPA 2: comércios do lojista logado (tela "Meus comércios"). */
    public List<Comercio> listarDoDono(String emailDono) { return repo.findByDonoEmail(emailDono); }

    /** Categorias para o campo "Tipo" do formulário (o controller não acessa mais o repositório). */
    public List<CategoriaComercio> listarCategorias() { return categorias.findAll(); }

    public Comercio buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RegraNegocioException("Comércio não encontrado."));
    }

    /**
     * ETAPA 3 (validação de dono): devolve o comércio SOMENTE se o lojista logado for o dono
     * (compara o dono_id do comércio com o usuário logado, pelo e-mail). Caso contrário lança
     * AccessDeniedException, que o Spring Security transforma em HTTP 403 (página error/403.html).
     */
    public Comercio buscarDoDono(Long id, String emailLogado) {
        Comercio c = buscar(id);
        boolean ehDono = c.getDono() != null && c.getDono().getEmail().equalsIgnoreCase(emailLogado);
        if (!ehDono) throw new AccessDeniedException("Você não é o dono deste comércio.");
        return c;
    }

    /**
     * Cadastro vindo do formulário (já validado pelo ComercioForm).
     * O dono do comércio é o lojista logado; a foto de capa (arquivo) é salva em uploads/.
     */
    public Comercio cadastrar(ComercioForm f, String emailDono) {
        CategoriaComercio categoria = categorias.findById(f.getCategoriaId())
                .orElseThrow(() -> new RegraNegocioException("Tipo de comércio inválido."));
        validarExpediente(categoria, f.getAbertura(), f.getFechamento());   // barbearia: confere abertura/fechamento
        String foto = armazenamento.salvar(f.getFotoCapa());   // valida a imagem antes de gravar no banco
        Comercio c = new Comercio();
        c.setNome(f.getNome().trim());
        c.setCategoria(categoria);
        c.setEndereco(f.getEndereco().trim());
        c.setWhatsapp(f.getWhatsapp());
        c.setFotoCapa(foto);
        if (categoria.getTipoPainel() == CategoriaComercio.TipoPainel.AGENDAMENTO) {   // expediente só para agenda
            c.setAbertura(f.getAbertura());
            c.setFechamento(f.getFechamento());
        }                                   // caminho público, ex.: /uploads/uuid.jpg
        c.setDono(usuarios.findByEmail(emailDono).orElse(null));
        try {
            return salvar(c);
        } catch (RuntimeException e) {
            armazenamento.remover(foto);                       // sem arquivo órfão se o banco falhar
            throw e;
        }
    }

    /**
     * ETAPA 3: edição do estabelecimento pelo próprio dono.
     * O tipo (categoria) PODE ser trocado. Nada é apagado na troca: produtos, serviços e agendamentos antigos
     * continuam no banco, só não aparecem no painel do novo tipo. Duas regras protegem os clientes e os dados:
     *  - sair de uma agenda com horários futuros confirmados é recusado (ver validarTrocaDeCategoria);
     *  - se o novo tipo for uma agenda, abertura e fechamento passam a ser obrigatórios.
     * Foto: se veio um arquivo novo, salva e apaga a antiga; se não veio, mantém a atual.
     */
    public Comercio editar(Long id, ComercioForm f, String emailLogado) {
        Comercio c = buscarDoDono(id, emailLogado);            // só o dono pode editar
        CategoriaComercio nova = categorias.findById(f.getCategoriaId())
                .orElseThrow(() -> new RegraNegocioException("Tipo de comércio inválido."));
        validarTrocaDeCategoria(c, nova);                      // tudo conferido ANTES de salvar a foto nova
        validarExpediente(nova, f.getAbertura(), f.getFechamento());   // só vale se o novo tipo for agenda
        String fotoAntiga = c.getFotoCapa();
        String fotoNova = armazenamento.salvar(f.getFotoCapa());   // null = nenhuma foto enviada

        c.setNome(f.getNome().trim());
        c.setCategoria(nova);
        c.setEndereco(f.getEndereco().trim());
        c.setWhatsapp(f.getWhatsapp());
        if (nova.getTipoPainel() == CategoriaComercio.TipoPainel.AGENDAMENTO) {   // expediente só existe para agenda
            c.setAbertura(f.getAbertura());
            c.setFechamento(f.getFechamento());
        } else {
            c.setAbertura(null);
            c.setFechamento(null);
        }
        if (fotoNova != null) c.setFotoCapa(fotoNova);         // senão, mantém a foto antiga

        Comercio salvo;
        try {
            salvo = salvar(c);
        } catch (RuntimeException e) {
            armazenamento.remover(fotoNova);
            throw e;
        }
        if (fotoNova != null) armazenamento.remover(fotoAntiga);   // troca concluída: apaga a antiga
        return salvo;
    }

    /**
     * ETAPA 3: exclusão do comércio pelo dono. Confere o dono (403 se não for), apaga o comércio
     * (o JPA apaga produtos e agendamentos em cascata) e depois apaga as fotos da pasta uploads/.
     */
    @Transactional
    public void excluirDoDono(Long id, String emailLogado) {
        Comercio c = buscarDoDono(id, emailLogado);
        List<String> fotos = coletarFotos(c);
        repo.delete(c);
        removerArquivosAposCommit(fotos);
    }

    /** Caminhos de todas as imagens de um comércio: capa + fotos dos produtos. */
    public List<String> coletarFotos(Comercio c) {
        List<String> fotos = new ArrayList<>();
        if (c.getFotoCapa() != null) fotos.add(c.getFotoCapa());
        c.getProdutos().forEach(p -> { if (p.getFotoUrl() != null) fotos.add(p.getFotoUrl()); });
        return fotos;
    }

    /**
     * Apaga os arquivos SOMENTE depois que o banco confirmar a exclusão (commit). Assim, se a
     * transação falhar e for desfeita, as imagens continuam no disco e nada fica inconsistente.
     */
    public void removerArquivosAposCommit(List<String> caminhos) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            caminhos.forEach(armazenamento::remover);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { caminhos.forEach(armazenamento::remover); }
        });
    }

    public Comercio salvar(Comercio c) {
        c.setWhatsapp(c.getWhatsapp().replaceAll("\\D", ""));   // guarda só dígitos
        return repo.save(c);
    }

    /**
     * API REST - criar. Regras de segurança:
     *  - o id enviado no JSON é IGNORADO (senão daria para sobrescrever o comércio de outra pessoa);
     *  - o dono é SEMPRE o usuário logado (o JSON não consegue definir o dono);
     *  - a categoria é carregada do banco; a foto só pode ser um link externo.
     */
    public Comercio criarPelaApi(Comercio c, String emailDono) {
        c.setId(null);
        c.setCategoria(resolverCategoria(c.getCategoria()));
        normalizarExpediente(c);
        c.setFotoCapa(armazenamento.validarLinkExterno(c.getFotoCapa(), null));
        c.setDono(usuarios.findByEmail(emailDono).orElse(null));
        return salvar(c);
    }

    /** API REST - atualizar (a checagem do dono é feita no controller, com @PreAuthorize). */
    public Comercio atualizar(Long id, Comercio novo) {
        Comercio c = buscar(id);
        String fotoAntiga = c.getFotoCapa();
        CategoriaComercio novaCategoria = resolverCategoria(novo.getCategoria());
        validarTrocaDeCategoria(c, novaCategoria);   // mesma proteção da tela: não esconder clientes agendados
        c.setNome(novo.getNome());
        c.setCategoria(novaCategoria);
        c.setAbertura(novo.getAbertura());
        c.setFechamento(novo.getFechamento());
        normalizarExpediente(c);
        c.setEndereco(novo.getEndereco());
        c.setFotoCapa(armazenamento.validarLinkExterno(novo.getFotoCapa(), fotoAntiga));
        c.setWhatsapp(novo.getWhatsapp());
        Comercio salvo = salvar(c);
        if (!Objects.equals(fotoAntiga, salvo.getFotoCapa())) armazenamento.remover(fotoAntiga);   // não deixa arquivo órfão
        return salvo;
    }

    /**
     * Troca de tipo: se o comércio tem uma AGENDA e vai passar para um tipo sem agenda, os horários futuros
     * já confirmados sumiriam do painel e o cliente ficaria sem atendimento. Por isso a troca só é permitida
     * depois que o lojista cancelar esses agendamentos (na tela de agenda).
     */
    private void validarTrocaDeCategoria(Comercio c, CategoriaComercio nova) {
        if (Objects.equals(c.getCategoria().getId(), nova.getId())) return;     // não mudou
        boolean saindoDaAgenda = c.isAgendavel()
                && nova.getTipoPainel() != CategoriaComercio.TipoPainel.AGENDAMENTO;
        if (saindoDaAgenda && agendamentos.existsByComercioIdAndStatusAndDataHoraAfter(
                c.getId(), Agendamento.Status.CONFIRMADO, LocalDateTime.now()))
            throw new RegraNegocioException("Há clientes com horário marcado. Cancele os agendamentos futuros "
                    + "na agenda antes de trocar o tipo.");
    }

    /**
     * Expediente da agenda (só barbearias): o lojista escolhe abertura e fechamento. O fechamento é quando o
     * atendimento ENCERRA: abertura 07:00 e fechamento 22:00 = primeiro horário 07:00, último 21:50.
     * Os minutos precisam ser múltiplos de 10 (os horários são de 10 em 10 minutos).
     */
    private void validarExpediente(CategoriaComercio categoria, LocalTime abertura, LocalTime fechamento) {
        if (categoria.getTipoPainel() != CategoriaComercio.TipoPainel.AGENDAMENTO) return;
        if (abertura == null || fechamento == null)
            throw new ExpedienteInvalidoException("Informe o horário de abertura e o de fechamento.");
        if (!abertura.isBefore(fechamento))
            throw new ExpedienteInvalidoException("O fechamento deve ser depois da abertura.");
        int m = AgendamentoService.DURACAO_MIN;
        if (abertura.getMinute() % m != 0 || fechamento.getMinute() % m != 0
                || abertura.getSecond() != 0 || fechamento.getSecond() != 0)
            throw new ExpedienteInvalidoException("Use horários de " + m + " em " + m + " minutos (ex.: 07:00, 07:10).");
    }

    /** API REST: barbearia -> valida o expediente; loja de produtos -> descarta qualquer expediente enviado. */
    private void normalizarExpediente(Comercio c) {
        if (c.isAgendavel()) validarExpediente(c.getCategoria(), c.getAbertura(), c.getFechamento());
        else { c.setAbertura(null); c.setFechamento(null); }
    }

    /** Troca a categoria "só com id" vinda do JSON pela categoria real do banco. */
    private CategoriaComercio resolverCategoria(CategoriaComercio informada) {
        if (informada == null || informada.getId() == null)
            throw new RegraNegocioException("Informe a categoria (campo categoria.id).");
        return categorias.findById(informada.getId())
                .orElseThrow(() -> new RegraNegocioException("Tipo de comércio inválido."));
    }

    /** Exclusão usada pela API REST (também limpa as imagens do comércio). */
    @Transactional
    public void excluir(Long id) {
        Comercio c = buscar(id);
        List<String> fotos = coletarFotos(c);
        repo.delete(c);
        removerArquivosAposCommit(fotos);
    }

    /**
     * Agrupa os comércios por categoria: chave = nome da categoria, valor = List de comércios.
     * TreeMap mantém as categorias em ordem alfabética (Collator respeita acentos).
     */
    public Map<String, List<Comercio>> agruparPorCategoria() {
        Map<String, List<Comercio>> mapa = new TreeMap<>(Collator.getInstance(Locale.forLanguageTag("pt-BR")));
        for (Comercio c : repo.findAll()) {
            mapa.computeIfAbsent(c.getCategoria().getNome(), k -> new ArrayList<>()).add(c);
        }
        return mapa;
    }
}
