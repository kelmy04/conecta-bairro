-- ============================================================
-- Conecta Bairro - LIMPEZA DOS DADOS DE TESTE (irreversível!)
-- Apaga TODAS as contas, comércios, produtos e agendamentos.
-- Mantém as tabelas e as CATEGORIAS (Mercado, Barbearia, etc.).
-- RESTART IDENTITY faz os ids voltarem a começar do 1.
-- Execute no Query Tool do pgAdmin, no banco do projeto.
-- ============================================================
TRUNCATE TABLE agendamento, produto, comercio, usuario RESTART IDENTITY;

-- Conferência (devem retornar 0, 0, 0, 0 e 5):
SELECT (SELECT COUNT(*) FROM usuario)     AS usuarios,
       (SELECT COUNT(*) FROM comercio)    AS comercios,
       (SELECT COUNT(*) FROM produto)     AS produtos,
       (SELECT COUNT(*) FROM agendamento) AS agendamentos,
       (SELECT COUNT(*) FROM categoria_comercio) AS categorias;
