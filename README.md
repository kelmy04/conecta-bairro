# Conecta Bairro

## Como rodar
1. Instale JDK 17+ (ideal: 21 LTS), Maven e PostgreSQL.
2. Coloque a pasta em um caminho SEM acentos nem espaços (ex.: `C:\dev\conecta-bairro`).
3. Crie o banco `conecta_bairro` e execute `src/main/resources/db/schema.sql` (uma única vez).
4. Ajuste a senha do banco em `src/main/resources/application-local.properties` (arquivo só da sua máquina; fica fora do Git).
5. No terminal, dentro da pasta: `mvn clean spring-boot:run`
6. Abra http://localhost:8080

## Contas
- Qualquer pessoa cria conta em `/registrar`; depois cadastra comércios em `/meu-painel` > "+ Cadastrar Novo Comércio".
- Cada lojista só acessa o painel e a edição dos PRÓPRIOS comércios (outro usuário recebe 403).
- Excluir comércio ou conta apaga também produtos, agendamentos e fotos.

## Primeiro acesso
- Não há usuário de exemplo: crie sua conta em `/registrar`, entre em `/login` e cadastre o comércio em `/meu-painel`.
- Para zerar os dados de teste de um banco já existente, execute `src/main/resources/db/limpar-dados-de-teste.sql`
  (as fotos ficam no banco, na tabela `imagem`, e são apagadas junto).

## Agendamento (barbearia)
- O lojista escolhe a abertura e o fechamento da agenda (ex.: 07:00 e 22:00 = reservas das 07:00 às 21:50), em horários de 10 em 10 minutos; os já reservados aparecem marcados e geram aviso, até 60 dias à frente; nome e telefone são validados.
- Depois de reservar, o cliente vê a tela de sucesso com o botão "Confirmar pelo WhatsApp".
- No painel, o lojista pode cancelar agendamentos (o horário volta a ficar livre) e editar produtos.

## Imagens
- As fotos são enviadas pelo formulário (JPG/PNG, até 3 MB) e ficam **no banco de dados** (tabela `imagem`).
  Antes de gravar, são reduzidas (máx. 1280 px), giradas conforme o celular e têm o GPS (EXIF) removido.

## Endereços
- Vitrine: `/`  | Login: `/login` | Meus comércios: `/meu-painel` | Cadastro: `/cadastrar`
- Editar comércio (somente o dono): `/comercio/{id}/editar`
- Painel do lojista: `/painel/{id}` (exige login e ser o dono)
- API (consultas): `/api/comercios`, `/api/comercios/{id}/produtos`

## Publicar
Veja `DEPLOY.md` (Docker, Render e VPS).
