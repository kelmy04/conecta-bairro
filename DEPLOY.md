# Publicar o Conecta Bairro

## Variáveis de ambiente
| Variável | Para que serve | Padrão |
|---|---|---|
| `DB_URL` | Endereço JDBC do banco, ex.: `jdbc:postgresql://HOST:5432/NOME` | banco local |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco (**segredo**) | vazio |
| `COOKIE_SECURE` | `true` quando o site usa HTTPS | `false` |
| `APP_TIMEZONE` | Fuso da agenda | `America/Cuiaba` |
| `PORT` | Porta (a hospedagem define) | `8080` |
| `JAVA_OPTS` | Memória/opções do Java (padrão pensado para 512 MB) | `-Xmx300m -XX:+UseSerialGC ...` |
| `DB_POOL` | Máximo de conexões com o banco | `5` |
| `DDL_AUTO` | `update` cria/ajusta tabelas; `validate` só confere | `update` |

**As fotos ficam no próprio banco** (tabela `imagem`), reduzidas a no máximo 1280 px e sem dados de GPS.
Por isso o site **não precisa de disco persistente**: pode rodar em hospedagens gratuitas que apagam o disco a cada
publicação, sem perder fotos. Em contrapartida, o tamanho do banco passa a importar (veja o painel do seu banco).

O banco novo **não precisa** do `schema.sql`: as tabelas, as categorias e o índice anti-duplicidade
de horários são criados sozinhos na primeira inicialização.

## Testar tudo com Docker (na sua máquina)
1. Copie `.env.example` para `.env` e defina `DB_PASSWORD`.
2. `docker compose up --build`
3. Abra http://localhost:8080 e crie uma conta em `/registrar`.

## Render (resumo)
1. Envie o projeto ao GitHub (o `.gitignore` já protege senhas, `target/` e `uploads/`).
2. Crie um **PostgreSQL** no Render e anote host, banco, usuário e senha.
3. Crie um **Web Service** a partir do repositório (ele usa o `Dockerfile`).
4. Em *Environment*, defina `DB_URL` (formato `jdbc:postgresql://HOST:5432/BANCO`), `DB_USER`, `DB_PASSWORD` e `COOKIE_SECURE=true`.
5. Não é preciso Disk: as fotos ficam no banco.
6. Abra o endereço `.onrender.com`, crie sua conta e cadastre o comércio.

## VPS (resumo)
Instale Docker, copie o projeto, crie o `.env` e rode `docker compose up -d --build`.
Para HTTPS, coloque na frente um proxy como o Caddy (certificado automático) e use `COOKIE_SECURE=true`.

## Antes de divulgar
- Use uma senha de banco forte e faça backup periódico (`pg_dump`).
- `COOKIE_SECURE=true` com HTTPS.
- O formulário de reserva coleta nome e telefone: avise o uso dos dados (LGPD).
- Ainda não há limite de tentativas de login: considere adicionar antes de abrir para muitas pessoas.
