# Conecta Bairro

<p align="center"><img src="src/main/resources/static/img/logo.svg" alt="Conecta Bairro" width="360"></p>

![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-banco%20de%20dados-336791)
![Docker](https://img.shields.io/badge/Docker-pronto-2496ED)
![Status](https://img.shields.io/badge/status-em%20produ%C3%A7%C3%A3o-brightgreen)

Plataforma web que **digitaliza e conecta os pequenos comércios de um bairro**: mercado, açougue, pet shop, loja de roupas, indústria, lanchonete, bar, restaurante, barbearia, cabeleireiro(a) e prestadores de serviços terceirizados (eletricista, encanador, diarista...).
Os moradores encontram os comércios, veem produtos e marcam horários pelo celular; os donos gerenciam tudo em um painel simples, com fontes grandes e cores amigáveis, pensado para quem tem pouca familiaridade com tecnologia.

Projeto de extensão universitária de Engenharia de Computação.

**Site no ar:** https://conecta-bairro-lvcm.onrender.com

## Sumário

* [Status](#status)
* [Funcionalidades](#funcionalidades)
* [Tecnologias](#tecnologias)
* [Como rodar no seu computador](#como-rodar-no-seu-computador)
* [Como usar](#como-usar)
* [Rotas principais](#rotas-principais)
* [Segurança](#segurança)
* [Estrutura do projeto](#estrutura-do-projeto)
* [Publicação](#publicação)
* [Trabalhos futuros](#trabalhos-futuros)
* [Como contribuir](#como-contribuir)
* [Licença e autoria](#licença-e-autoria)

## Status

Em produção para demonstração. A hospedagem gratuita "dorme" depois de alguns minutos sem acessos, então o **primeiro acesso pode levar cerca de 1 minuto**.

## Funcionalidades

**Vitrine pública (moradores)**

* Lista de todos os comércios do bairro, agrupados por categoria em ordem alfabética.
* Página de cada comércio com foto de capa, endereço e botão direto para o **WhatsApp** do lojista.
* Lojas de produtos: catálogo com foto, preço e estoque.
* Barbearia e cabeleireiro(a): reserva de horário sem precisar criar conta.
* Prestadores de serviços (eletricista, encanador, diarista, pedreiro, pintor...): lista de serviços com preço "a partir de" e botão **Pedir orçamento**, que abre o WhatsApp do prestador com a mensagem pronta.

**Painel do lojista**

* Conta própria (criar conta, entrar, sair, excluir conta) e tela "Meus comércios".
* Cadastro, edição e exclusão de comércios, com upload da foto de capa.
* Produtos: cadastrar, editar e excluir, com foto.
* **Post de divulgação:** gera o texto pronto para colar no WhatsApp ou no Instagram.
* Cada lojista só enxerga e altera os **próprios** comércios.

**Agenda (barbearia e cabeleireiro(a))**

* O lojista escolhe o horário de **abertura e fechamento** da agenda (ex.: abre 07:00, fecha 22:00 = reservas das 07:00 às 21:50).
* Horários de **10 em 10 minutos**, até 60 dias à frente.
* Horário já ocupado aparece marcado, e o cliente recebe o aviso; duas pessoas não conseguem reservar o mesmo horário.
* Cada categoria tem seus serviços (ex.: Corte, Barba e Corte + Barba na barbearia; Corte, Escova, Coloração, Hidratação e Penteado no cabeleireiro(a)).
* Depois de reservar, o cliente vê uma tela de sucesso com o botão **"Confirmar pelo WhatsApp"** e a mensagem já pronta.
* No painel, o lojista vê a agenda do dia e pode cancelar agendamentos (o horário volta a ficar livre).

**Fotos**

* Envio de arquivo JPG ou PNG (até 3 MB). As fotos são reduzidas a no máximo 1280 px, giradas conforme o celular e têm o GPS (EXIF) removido.
* Ficam guardadas **no banco de dados** (tabela `imagem`), o que permite usar hospedagens gratuitas que apagam o disco a cada publicação.

## Tecnologias

|Camada|Tecnologia|
|-|-|
|Linguagem e framework|Java 17+ (testado também com JDK 26), Spring Boot 3.3 (MVC e API REST)|
|Banco de dados|PostgreSQL, com Spring Data JPA / Hibernate|
|Tela|Thymeleaf, HTML5, CSS3, JavaScript e Bootstrap 5 (responsivo)|
|Segurança|Spring Security 6 (login com BCrypt, CSRF, controle de acesso por dono)|
|Outras|Lombok, Bean Validation, Maven|
|Entrega|Docker e Docker Compose; publicado no Render com banco no Neon|

Estruturas de dados usadas: `TreeMap` para agrupar os comércios por categoria (ordem alfabética) e `List` para produtos e agendamentos.

## Como rodar no seu computador

**Pré-requisitos:** JDK 17 ou mais novo, Maven e PostgreSQL.

1. Clone o repositório e entre na pasta.

> No Windows, deixe a pasta em um caminho **sem acentos nem espaços** (ex.: `C:\dev\conecta-bairro`).

2. Crie um banco vazio no PostgreSQL, por exemplo `conecta_bairro`.
3. Crie o arquivo `src/main/resources/application-local.properties` (ele fica fora do Git) com os dados do seu banco:

```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/conecta_bairro
   spring.datasource.username=postgres
   spring.datasource.password=SUA_SENHA
   ```

4. Execute:

```
   mvn clean spring-boot:run
   ```

5. Abra http://localhost:8080

As tabelas, as categorias e o índice que impede reservas duplicadas são criados **automaticamente** na primeira execução. O arquivo `src/main/resources/db/schema.sql` fica como documentação do modelo de dados.

**Com Docker** (site e banco juntos): copie `.env.example` para `.env`, defina `DB_PASSWORD` e rode `docker compose up --build`.

## Como usar

**Sou morador**

1. Abra a vitrine e escolha um comércio.
2. Em lojas de produtos, veja o catálogo e fale com o lojista pelo botão do WhatsApp.
3. Em barbearia ou cabeleireiro(a), escolha o dia, o horário, o serviço, informe nome e telefone e clique em **Reservar horário**. Depois use **Confirmar pelo WhatsApp**.

**Sou lojista**

1. Clique em **Criar Conta** e depois em **Entrar**.
2. Em **Meus Comércios**, clique em **+ Cadastrar Novo Comércio**. Se for barbearia ou cabeleireiro(a), defina abertura e fechamento.
3. Use **Gerenciar Produtos** (ou **Gerenciar Agenda**) e **Editar Estabelecimento** em cada card.
4. Nos produtos, o botão **Gerar post** cria o texto de divulgação para copiar.

## Rotas principais

|Rota|Quem acessa|
|-|-|
|`/` e `/comercio/{id}`|Qualquer pessoa|
|`/registrar` e `/login`|Qualquer pessoa|
|`/meu-painel`, `/cadastrar`|Lojista logado|
|`/painel/{id}`|Somente o dono do comércio|
|`/comercio/{id}/editar` e `/comercio/{id}/excluir`|Somente o dono|
|`/minha-conta/excluir`|Lojista logado (apaga a própria conta)|
|`GET /api/comercios`, `/api/comercios/{id}`, `/api/comercios/{id}/produtos`|Público (consulta)|
|`POST`, `PUT` e `DELETE` em `/api/...`|Login **e** ser o dono do registro|

## Segurança

* Senhas guardadas com **BCrypt**; proteção **CSRF** ativa em todos os formulários.
* **Trava de dono** (`@PreAuthorize` + `DonoGuard`): outro usuário recebe erro 403 no painel, na edição e na API.
* Formulários validados por **DTOs** (impede campos extras que sobrescreveriam registros de outras pessoas).
* Upload verificado pelo **conteúdo** do arquivo (não só pela extensão), com limite de tamanho e de dimensões.
* Índice único no banco impede dois agendamentos confirmados no mesmo horário, mesmo em acessos simultâneos.
* Textos exibidos com escape automático do Thymeleaf (proteção contra XSS) e consultas parametrizadas do JPA (proteção contra SQL Injection).
* Segredos (senha do banco) ficam em variáveis de ambiente, nunca no código.

## Estrutura do projeto

```
conecta-bairro/
├── Dockerfile · docker-compose.yml · .env.example · DEPLOY.md
├── pom.xml
└── src/main/
    ├── java/br/com/conectabairro/
    │   ├── config/       SecurityConfig, DonoGuard, BancoConfig, DadosIniciaisConfig
    │   ├── controller/   Vitrine, Painel, Comercio, Conta, Dashboard, Imagem + API REST
    │   ├── dto/          formulários validados (Comercio, Produto, Agendamento, Registro)
    │   ├── exception/    exceções de negócio e tratamento de erros
    │   ├── model/        entidades JPA (Usuario, Comercio, Produto, Agendamento, ...)
    │   ├── repository/   interfaces Spring Data
    │   └── service/      regras de negócio (agenda, fotos, comércios, produtos, contas)
    └── resources/
        ├── templates/    páginas Thymeleaf
        ├── static/css/   estilo (paleta azul, verde e laranja)
        └── db/           schema.sql e script de limpeza dos dados de teste
```

## Publicação

O passo a passo (variáveis de ambiente, Docker, Render e VPS) está em [`DEPLOY.md`](DEPLOY.md).
Hoje o site roda no **Render** (plano gratuito) e o banco no **Neon**. Antes de divulgar para os comerciantes, leia a seção "Antes de divulgar" do `DEPLOY.md`.

## Trabalhos futuros

* Limite de tentativas de login e recuperação de senha.
* Aviso de privacidade (LGPD) no formulário de reserva.
* Duração por serviço: hoje cada reserva ocupa um único horário de 10 minutos.
* Promoções e busca/filtro na vitrine, com paginação.
* Testes automatizados (JUnit) e migrações de banco com Flyway.
* Backup automático do banco.

## Como contribuir

1. Abra uma *Issue* descrevendo a sugestão ou o problema.
2. Crie um *branch* a partir de `main` (ex.: `feature/busca-vitrine`) e faça commits pequenos e claros.
3. Rode o projeto localmente e confira o fluxo alterado antes de abrir o *Pull Request*.
4. Nunca envie senhas ou arquivos `.env` e `application-local.properties` (o `.gitignore` já os protege).

## Licença e autoria

Projeto acadêmico de extensão universitária. Licença: a definir pela autoria.

Desenvolvido por **Kelmy A. J. Costa** — curso de Engenharia de Computação.

