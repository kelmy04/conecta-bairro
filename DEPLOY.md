# Publicar o Conecta Bairro

## Variáveis de ambiente
| Variável | Para que serve | Padrão |
|---|---|---|
| `DB_URL` | Endereço JDBC do banco, ex.: `jdbc:postgresql://HOST:5432/NOME` | banco local |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco (**segredo**) | vazio |
| `COOKIE_SECURE` | `true` quando o site usa HTTPS | `false` |
| `UPLOAD_DIR` | Pasta das fotos (precisa ser um disco **persistente**) | `uploads` (Docker: `/data/uploads`) |
| `APP_TIMEZONE` | Fuso da agenda | `America/Cuiaba` |
| `PORT` | Porta (a hospedagem define) | `8080` |
| `DDL_AUTO` | `update` cria/ajusta tabelas; `validate` só confere | `update` |

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
5. Para guardar as fotos, adicione um **Disk** montado em `/data/uploads` (recurso de plano pago).
6. Abra o endereço `.onrender.com`, crie sua conta e cadastre o comércio.

## VPS (resumo)
Instale Docker, copie o projeto, crie o `.env` e rode `docker compose up -d --build`.
Para HTTPS, coloque na frente um proxy como o Caddy (certificado automático) e use `COOKIE_SECURE=true`.

## Antes de divulgar
- Use uma senha de banco forte e faça backup periódico (`pg_dump`).
- `COOKIE_SECURE=true` com HTTPS.
- O formulário de reserva coleta nome e telefone: avise o uso dos dados (LGPD).
- Ainda não há limite de tentativas de login: considere adicionar antes de abrir para muitas pessoas.
