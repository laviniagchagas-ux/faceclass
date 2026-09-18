# Código explicado — FaceClass

Este guia explica a função de cada bloco de código. A ideia é entender o caminho que uma ação percorre, e não decorar cada linha.

## Visão do caminho dos dados

```text
Tela Android → Retrofit → rota FastAPI → regras do serviço → MySQL
     ↑                                                    ↓
     └────────────── resposta JSON / erro ────────────────┘
```

Exemplo: ao confirmar presença, o celular lê a face, envia as medidas para `POST /attendance/face-check-in`, a API compara a assinatura facial cadastrada, verifica a aula e salva a presença no MySQL. Depois a resposta volta para a tela e atualiza o histórico.

## Back-end Python — pasta `backend/app`

### `main.py`

É a porta de entrada da API. Cria o aplicativo FastAPI, permite que o Android chame a API durante o desenvolvimento e registra as rotas de autenticação, cadastros, biometria, chamada e painel. A rota `GET /health` responde se a API está viva.

### `core/config.py`

Lê as configurações do arquivo `.env`: endereço do MySQL, chave JWT, duração do login, fuso horário e modo de demonstração. Assim, senha e configurações não ficam escritas no código.

### `core/security.py`

Cuida da segurança do login:

- transforma a senha em hash Argon2 antes de salvar;
- compara a senha digitada com o hash existente;
- cria o token JWT após o login;
- verifica se o token ainda é válido em cada rota protegida.

### `db/database.py`

Cria a ligação com MySQL por meio do SQLAlchemy. A função `get_db()` abre uma sessão para uma requisição e fecha ao final, evitando conexões presas.

### `db/models.py`

Traduz as tabelas SQL em classes Python. Por exemplo, a classe `Aluno` representa a tabela `aluno`; `Aula` representa `aula`; `Presenca` representa `presenca`.

As entidades principais são:

| Classe/tabela | Para que serve |
| --- | --- |
| `Usuario` | login, senha protegida e perfil (ADMIN, PROFESSOR ou ALUNO) |
| `Escola` | dados da escola, Wi-Fi e local permitido |
| `Aluno`, `Professor`, `Pais` | pessoas cadastradas |
| `Turma`, `Matricula` | vínculo entre alunos e turmas |
| `Aula` | chamada aberta por um professor |
| `Presenca` | registro de presença, atraso ou falta |
| `SaldoAtraso` | percentual acumulado de atrasos no mês |

### `schemas.py`

Define o formato dos dados que chegam e saem da API. É a camada que evita, por exemplo, criar uma aula sem turma, cadastrar e-mail inválido ou receber uma assinatura facial com tamanho incorreto.

### `api/deps.py`

Contém as verificações reutilizadas pelas rotas. Lê o token Bearer, descobre o usuário logado e bloqueia perfis sem permissão. Por exemplo, `require_roles("PROFESSOR")` impede aluno de criar aula.

## Back-end Python — rotas

| Arquivo | O que recebe e devolve |
| --- | --- |
| `api/routes/auth.py` | cria o primeiro administrador, faz login e mostra o usuário logado |
| `api/routes/management.py` | cria/lista escolas, alunos, responsáveis, professores, turmas e matrículas |
| `api/routes/attendance.py` | abre/fecha aula, lista aulas disponíveis e registra/corrige chamadas |
| `api/routes/biometrics.py` | informa se o aluno cadastrou a face, registra ou remove a assinatura facial |
| `api/routes/dashboard.py` | monta o histórico do aluno e o resumo da turma para o professor |

Uma rota é uma URL da API. Por exemplo, `POST /auth/login` recebe e-mail e senha; `GET /dashboard/me/attendance` devolve o histórico do aluno.

## Back-end Python — regras importantes

### `services/attendance.py`

É onde ficam as regras de negócio da chamada. O código:

1. confere se a aula existe e está aberta;
2. verifica se o aluno pertence à turma;
3. impede a mesma presença duas vezes na mesma aula;
4. valida horário, Wi-Fi, localização e confirmação facial quando esses dados são exigidos;
5. calcula atraso: há 10 minutos de tolerância; depois, cada bloco iniciado de 5 minutos soma 10%;
6. transforma 100% de atraso acumulado em uma falta;
7. cria o registro na tabela `presenca`.

### `services/biometrics.py`

Recebe uma lista de 12 medidas faciais normalizadas, padroniza os números, cria um hash SHA-256 e compara a leitura nova com a leitura cadastrada pela distância RMS. Ele não salva foto, vídeo nem Base64.

Essa comparação é adequada para a demonstração controlada do TCC. Ela não substitui uma solução biométrica profissional com proteção contra fraude, consentimento e regras de proteção de dados.

## Banco de dados — pasta `backend/database`

| Arquivo | Quando usar |
| --- | --- |
| `01_schema_faceclass.sql` | em banco novo; cria todas as tabelas já com os campos de assinatura facial |
| `02_upgrade_from_original.sql` | somente se existir o banco antigo da equipe |
| `03_add_hash_biometria_facial.sql` | migração antiga do hash facial; não executar no banco novo |
| `04_add_vetor_biometria_facial.sql` | somente se o banco já tinha o hash, mas ainda não tinha `vetor_face` |

## Android — pasta `android/app/src/main`

### `AndroidManifest.xml`

Declara que o aplicativo precisa de internet e câmera. `usesCleartextTraffic="true"` permite comunicação local por `http://` durante a apresentação; em produção o correto seria HTTPS.

### `data/Models.kt`

Define as estruturas Kotlin que representam os JSONs da API: usuário, aula, presença, resposta de login e assinatura facial.

### `data/FaceClassApi.kt`

Tem duas funções principais:

- `FaceClassApi`: lista os endpoints que o Retrofit pode chamar;
- `FaceClassRepository`: executa as chamadas, adiciona o token no cabeçalho e guarda a sessão no celular.

`BuildConfig.BASE_URL` define para onde o aplicativo envia os pedidos. No emulador, `10.0.2.2` significa o computador onde o Python está rodando.

### `data/FriendlyApiError.kt`

Lê os erros da API e os transforma em mensagens compreensíveis. Em vez de mostrar uma exceção técnica, a tela pode mostrar, por exemplo, que a sessão expirou, a senha está incorreta ou o servidor está desligado.

### `FaceClassViewModel.kt`

É a ponte entre tela e dados. Guarda o estado atual — usuário logado, aulas, histórico, painel, mensagem e carregamento — e chama o repositório quando o usuário faz login, atualiza dados, cadastra face, registra presença ou cria aula.

### `MainActivity.kt`

Monta as telas do Jetpack Compose: login, visão do aluno, visão do professor, frequência e histórico. Também abre a câmera com CameraX, usa ML Kit para encontrar pontos do rosto e produz a assinatura de 12 medidas enviada ao back-end.

### `ui/theme/Theme.kt`

Centraliza as cores e o tema usado nas telas. É o arquivo a alterar se o grupo quiser mudar o estilo visual do aplicativo sem procurar cor por cor.

## Arquivos de configuração

| Arquivo | Função |
| --- | --- |
| `backend/requirements.txt` | bibliotecas Python necessárias |
| `backend/.env.example` | modelo seguro das configurações locais |
| `android/app/build.gradle.kts` | SDK, dependências Android e endereço da API |
| `android/gradle/wrapper/gradle-wrapper.properties` | versão do Gradle usada pelo projeto |
| `.gitignore` | evita enviar senhas e arquivos gerados para o GitHub |
