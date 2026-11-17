-- ============================================================
-- Conecta Bairro - Modelagem de dados (PostgreSQL)
-- Uso: CREATE DATABASE conecta_bairro; e então rode este script.
-- ============================================================

-- Define o "tipo de painel" de cada categoria (modularidade):
-- PRODUTOS -> estoque e posts | AGENDAMENTO -> agenda de horários
CREATE TABLE categoria_comercio (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(50) NOT NULL UNIQUE,
    tipo_painel VARCHAR(20) NOT NULL CHECK (tipo_painel IN ('PRODUTOS','AGENDAMENTO','SERVICOS')),
    servicos    VARCHAR(300)             -- serviços da agenda, separados por ';' (só categorias de AGENDAMENTO)
);

CREATE TABLE usuario (            -- dono do comércio (lojista)
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL
);

CREATE TABLE comercio (           -- N comércios : 1 categoria / 1 dono
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL,
    categoria_id BIGINT NOT NULL REFERENCES categoria_comercio(id),
    dono_id      BIGINT REFERENCES usuario(id),
    endereco     VARCHAR(200) NOT NULL,
    whatsapp     VARCHAR(20)  NOT NULL,   -- só dígitos, com DDI: 5511999990000
    foto_capa    VARCHAR(300),
    abertura     TIME,                    -- só barbearia: início da agenda (escolhido pelo lojista)
    fechamento   TIME                     -- só barbearia: o atendimento encerra aqui
);

CREATE TABLE produto (            -- exclui junto com o comércio
    id          BIGSERIAL PRIMARY KEY,
    comercio_id BIGINT NOT NULL REFERENCES comercio(id) ON DELETE CASCADE,
    nome        VARCHAR(100) NOT NULL,
    preco       NUMERIC(10,2) CHECK (preco > 0),   -- opcional: item "a combinar"
    estoque     INTEGER NOT NULL DEFAULT 0 CHECK (estoque >= 0),
    foto_url    VARCHAR(300),
    descricao   VARCHAR(300)             -- texto curto opcional (ingredientes, o que o serviço inclui...)
);

CREATE TABLE agendamento (
    id               BIGSERIAL PRIMARY KEY,
    comercio_id      BIGINT NOT NULL REFERENCES comercio(id) ON DELETE CASCADE,
    cliente_nome     VARCHAR(100) NOT NULL,
    cliente_telefone VARCHAR(20)  NOT NULL,
    servico          VARCHAR(60)  NOT NULL,
    data_hora        TIMESTAMP    NOT NULL,
    status           VARCHAR(15)  NOT NULL DEFAULT 'CONFIRMADO'
);
-- Última barreira contra "dois clientes no mesmo horário" (concorrência):
CREATE UNIQUE INDEX uq_agendamento_horario
    ON agendamento (comercio_id, data_hora) WHERE status = 'CONFIRMADO';

-- Dados iniciais: apenas as categorias (usuários e comércios são criados pelo próprio sistema)
INSERT INTO categoria_comercio (nome, tipo_painel, servicos) VALUES
 ('Mercado','PRODUTOS',NULL),('Açougue','PRODUTOS',NULL),('Pet Shop','PRODUTOS',NULL),
 ('Loja de Roupas','PRODUTOS',NULL),
 ('Indústria','PRODUTOS',NULL),('Lanchonete','PRODUTOS',NULL),
 ('Bar','PRODUTOS',NULL),('Restaurante','PRODUTOS',NULL),
 ('Barbearia','AGENDAMENTO','Corte;Barba;Corte + Barba'),
 ('Cabeleireiro(a)','AGENDAMENTO','Corte;Escova;Coloração;Hidratação;Penteado'),
 ('Eletricista','SERVICOS',NULL),('Encanador','SERVICOS',NULL),('Diarista e Limpeza','SERVICOS',NULL),
 ('Pedreiro e Reformas','SERVICOS',NULL),('Pintor','SERVICOS',NULL),('Jardinagem','SERVICOS',NULL),
 ('Técnico de Informática e Celular','SERVICOS',NULL),('Frete e Mudanças','SERVICOS',NULL),
 ('Outros Serviços','SERVICOS',NULL);Barba;Corte + Barba'),
 ('Cabeleireiro(a)','AGENDAMENTO','Corte;Escova;Coloração;Hidratação;Penteado');
