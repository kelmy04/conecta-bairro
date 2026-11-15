-- ============================================================
-- Conecta Bairro - LIMPEZA DOS DADOS DE TESTE (irreversível!)
-- Apaga TODAS as contas, comércios, produtos, agendamentos e FOTOS (tabela imagem).
-- Mantém as tabelas e as CATEGORIAS (Mercado, Barbearia, etc.).
-- RESTART IDENTITY faz os ids voltarem a começar do 1.
-- Execute no Query Tool do pgAdmin (ou no SQL Editor do Neon), no banco do projeto.
-- A tabela "imagem" é criada na primeira execução do sistema; rode este script depois disso.
-- ============================================================
TRUNCATE TABLE agendamento, produto, comercio, usuario, imagem RESTART IDENTITY;

-- Conferência (devem retornar 0, 0, 0, 0, 0 e o número de categorias):
SELECT (SELECT COUNT(*) FROM usuario)     AS usuarios,
       (SELECT COUNT(*) FROM comercio)    AS comercios,
       (SELECT COUNT(*) FROM produto)     AS produtos,
       (SELECT COUNT(*) FROM agendamento) AS agendamentos,
       (SELECT COUNT(*) FROM imagem)      AS imagens,
       (SELECT COUNT(*) FROM categoria_comercio) AS categorias;
