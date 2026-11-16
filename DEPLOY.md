# Publicar o Conecta Bairro

Guia para colocar o site no ar. O caminho principal usa serviços gratuitos e **não pede cartão**:

```
GitHub (código)  →  Render (site, via Dockerfile)  →  Neon (banco PostgreSQL, com as fotos)
```

> Os nomes de botões e menus dos serviços podem mudar com o tempo. Se algo não bater com a tela, siga a ideia do passo.

## Índice

1. [Antes de começar](#1-antes-de-começar)
2. [Enviar o projeto ao GitHub](#2-enviar-o-projeto-ao-github)
3. [Criar o banco no Neon](#3-criar-o-banco-no-neon)
4. [Testar o banco na sua máquina (opcional)](#4-testar-o-banco-na-sua-máquina-opcional)
5. [Criar o site no Render](#5-criar-o-site-no-render)
6. [Primeiro acesso](#6-primeiro-acesso)
7. [Atualizar o site depois](#7-atualizar-o-site-depois)
8. [Variáveis de ambiente](#8-variáveis-de-ambiente)
9. [Limites do plano gratuito](#9-limites-do-plano-gratuito)
10. [Antes de divulgar para os comerciantes](#10-antes-de-divulgar-para-os-comerciantes)
11. [Problemas comuns](#11-problemas-comuns)
12. [Outras formas: Docker e VPS](#12-outras-formas-docker-e-vps)

## 1\. Antes de começar

Você vai precisar de contas gratuitas no **GitHub**, no **Render** e no **Neon**.
No computador: Git instalado e, para testar localmente, JDK 17+ e Maven.

## 2\. Enviar o projeto ao GitHub

1. Crie um repositório **privado** e vazio em github.com.
2. **Confira se nenhum segredo vai junto.** Na pasta do projeto:

```
   git init
   git add .
   git ls-files | findstr "local .env anota"
   ```

   O último comando **não deve listar** `application-local.properties`, `.env` nem anotações pessoais. Se listar, não continue: tire o arquivo do Git com `git rm --cached ARQUIVO` e coloque-o no `.gitignore`.

3. Envie:

```
   git commit -m "Conecta Bairro"
   git branch -M main
   git remote add origin https://github.com/SEU\_USUARIO/conecta-bairro.git
   git push -u origin main
   ```

**Regra de ouro:** senhas, chaves e strings de conexão nunca ficam em arquivos dentro da pasta do projeto (nem em "anotações"). Quem lê o repositório lê o banco inteiro.

## 3\. Criar o banco no Neon

1. Em neon.com, crie um projeto: **PostgreSQL**, região mais próxima (ex.: São Paulo).
2. Em *Connection details* (ou *Connect*), **desligue o "Connection pooling"**. Assim o endereço (host) não tem `-pooler`.
3. Copie a string de conexão. Ela é parecida com:

```
   postgresql://USUARIO:SENHA@ep-xxxx.sa-east-1.aws.neon.tech/BANCO?sslmode=require\&channel\_binding=require
   ```

4. Transforme em **três valores** para o Render:

|Variável|Como montar|Exemplo|
|-|-|-|
|`DB\_URL`|`jdbc:postgresql://` + host + `/` + banco + `?sslmode=require` (**sem** `channel\_binding`)|`jdbc:postgresql://ep-xxxx.sa-east-1.aws.neon.tech/BANCO?sslmode=require`|
|`DB\_USER`|o `USUARIO` da string|`meu\_usuario\_owner`|
|`DB\_PASSWORD`|a `SENHA` da string|(a senha)|

Não é preciso rodar nenhum SQL: as tabelas, as categorias e o índice que impede reservas duplicadas são criados sozinhos na primeira inicialização.

## 4\. Testar o banco na sua máquina (opcional)

Vale a pena conferir antes de publicar. No PowerShell (use **aspas simples**, por causa dos caracteres `?` e `\&`):

```
$env:DB\_URL = 'jdbc:postgresql://HOST/BANCO?sslmode=require'
$env:DB\_USER = 'USUARIO'
$env:DB\_PASSWORD = 'SENHA'
mvn clean spring-boot:run
```

Abra http://localhost:8080, crie uma conta e cadastre um comércio com foto. Ao terminar, limpe as variáveis:

```
Remove-Item Env:DB\_URL, Env:DB\_USER, Env:DB\_PASSWORD
```

Se o arquivo `application-local.properties` existir, ele tem prioridade sobre essas variáveis: renomeie-o temporariamente se quiser testar só com elas.

## 5\. Criar o site no Render

1. Em render.com, entre com a conta do GitHub.
2. **New > Web Service** e escolha o repositório. O Render detecta o `Dockerfile` (runtime **Docker**).
3. Escolha a região mais próxima do banco e o plano **Free**.
4. Em *Environment*, crie as variáveis:

|Variável|Valor|
|-|-|
|`DB\_URL`|o valor montado no passo 3|
|`DB\_USER`|o usuário do banco|
|`DB\_PASSWORD`|a senha do banco|
|`COOKIE\_SECURE`|`true`|

5. Se houver o campo *Health Check Path*, use `/`.
6. Clique em **Create Web Service**. A primeira publicação leva alguns minutos. Acompanhe em *Logs* até aparecer `Started ConectaBairroApplication`.

## 6\. Primeiro acesso

1. Abra o endereço `https://SEU-SITE.onrender.com`.
2. Clique em **Criar Conta**, depois em **Entrar**.
3. Em **Meus Comércios**, clique em **+ Cadastrar Novo Comércio**.
4. Se criou contas e comércios só para testar, apague-os antes de divulgar (veja o passo 10).

## 7\. Atualizar o site depois

```
git add .
git commit -m "descreva a mudança"
git push
```

O Render publica a nova versão sozinho. Para mudar uma variável, edite em *Environment* e salve (ele reinicia o site).

## 8\. Variáveis de ambiente

|Variável|Para que serve|Padrão|
|-|-|-|
|`DB\_URL`|Endereço JDBC do banco|banco local|
|`DB\_USER`|Usuário do banco|`postgres`|
|`DB\_PASSWORD`|Senha do banco (**segredo**)|vazio|
|`COOKIE\_SECURE`|`true` quando o site usa HTTPS (cookie de login só criptografado)|`false`|
|`APP\_TIMEZONE`|Fuso da agenda|`America/Cuiaba`|
|`PORT`|Porta (a hospedagem define)|`8080`|
|`JAVA\_OPTS`|Memória e opções do Java (padrão pensado para instância de 512 MB)|`-Xmx300m -XX:+UseSerialGC ...`|
|`DB\_POOL`|Máximo de conexões com o banco|`5`|
|`DDL\_AUTO`|`update` cria e ajusta tabelas; `validate` só confere|`update`|

**As fotos ficam no próprio banco** (tabela `imagem`), reduzidas a no máximo 1280 px e sem dados de GPS. Por isso o site **não precisa de disco persistente** e pode rodar em hospedagens que apagam o disco a cada publicação.

## 9\. Limites do plano gratuito

* **Site dorme (Render):** depois de uns 15 minutos sem acessos, e o próximo visitante espera cerca de 1 minuto.
* **Banco dorme (Neon):** a computação do banco é suspensa após 5 minutos sem uso, e o primeiro acesso depois disso é um pouco mais lento.
* **Sessões:** quando o site dorme ou é republicado, quem estava logado precisa entrar de novo.
* **Espaço:** as fotos ocupam o armazenamento do banco, que no plano gratuito é pequeno (confira o limite atual no painel do Neon). Como cada foto fica em torno de 100 a 250 KB, cabem alguns milhares.
* **Recuperação:** o plano gratuito tem uma janela curta de restauração. Faça cópias de segurança (passo 10).

**Para uma apresentação:** abra o site uns 2 minutos antes e navegue um pouco, para acordar o site e o banco.
**Para uso real pelos comerciantes:** considere um plano pago ou um VPS, para o site não dormir.

## 10\. Antes de divulgar para os comerciantes

* **Senha do banco:** troque-a se ela já apareceu em chat, print, anotação ou no GitHub. No Neon: *Roles* > *Reset password*. Depois atualize `DB\_PASSWORD` no Render.
* **Repositório privado** e sem segredos (passo 2).
* **Dados de teste:** para apagar todas as contas, comércios, produtos, agendamentos e fotos, rode o conteúdo de `src/main/resources/db/limpar-dados-de-teste.sql` no *SQL Editor* do Neon. As categorias são mantidas. A ação não tem volta.
* **Cópia de segurança:** com o `pg\_dump` instalado, rode de vez em quando:

```
  pg\_dump "postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require" -F c -f backup.dump
  ```

  Guarde o `backup.dump` fora do repositório.

* **HTTPS:** `COOKIE\_SECURE=true`. O Render já entrega o site em HTTPS.
* **LGPD:** o formulário de reserva coleta nome e telefone; avise para que serão usados.
* **Login:** ainda não há limite de tentativas. Considere implementar antes de abrir para muita gente.

## 11\. Problemas comuns

|Sintoma|Causa provável e solução|
|-|-|
|Deploy falha com erro de conexão|`DB\_URL` deve começar com `jdbc:postgresql://`, usar o host **sem** `-pooler` e terminar com `?sslmode=require`.|
|`password authentication failed`|Senha errada ou desatualizada (por exemplo, depois de redefinir no Neon). Atualize `DB\_PASSWORD`.|
|Erro mencionando `channel\_binding`|Remova `\&channel\_binding=require` do `DB\_URL`.|
|Site abre, mas o login "não pega"|Use o endereço `https://`. Com `COOKIE\_SECURE=true`, o cookie não é gravado em `http://`.|
|Primeiro acesso muito lento|Site e banco estavam dormindo (passo 9).|
|Fotos não aparecem ou não salvam|Verifique o espaço do banco no painel do Neon e se a foto tem até 3 MB, em JPG ou PNG.|
|Reiniciando sem parar / falta de memória|O Java precisa caber na instância. Ajuste `JAVA\_OPTS` (ex.: `-Xmx300m`) ou use uma instância maior.|
|Horários da agenda 4 horas errados|Confira `APP\_TIMEZONE` (padrão `America/Cuiaba`).|
|Quer ver o que aconteceu|Painel do Render > *Logs*.|

## 12\. Outras formas: Docker e VPS

**Testar tudo com Docker na sua máquina** (site e banco juntos):

1. Copie `.env.example` para `.env` e defina `DB\_PASSWORD`.
2. `docker compose up --build`
3. Abra http://localhost:8080 e crie uma conta em `/registrar`.

**VPS (servidor próprio):** instale Docker, copie o projeto, crie o `.env` e rode `docker compose up -d --build`.
Para HTTPS, coloque na frente um proxy como o Caddy (certificado automático) e use `COOKIE\_SECURE=true`.
No `docker-compose.yml`, o banco guarda os dados em um volume (`dados-banco`), então eles sobrevivem a reinicializações.

