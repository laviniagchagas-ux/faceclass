# FaceClass

Projeto de TCC para controle de chamada escolar com aplicativo Android, API em Python e banco de dados MySQL.

Esta pasta foi organizada para ser enviada ao GitHub. Ela contém somente código-fonte, scripts do banco e documentos. Arquivos gerados, senhas, ambiente virtual e configurações pessoais foram deixados de fora.

## Estrutura

```text
FaceClass-GitHub/
├── android/                 aplicativo Kotlin para Android Studio
├── backend/                 API Python/FastAPI e scripts MySQL
│   ├── app/                 código da API
│   ├── database/            criação e migrações do banco
│   └── tests/               teste básico do fluxo
├── docs/                    documentação para estudo e apresentação
├── .gitignore               arquivos que não devem ir para o GitHub
└── README.md                este arquivo
```

## Por onde começar

1. Para entender a função de cada arquivo, leia [Código explicado](docs/01-codigo-explicado.md).
2. Para montar e apresentar o projeto em outro computador, leia [Como rodar na escola](docs/02-rodar-na-escola.md).
3. Para publicar a pasta, leia [Como subir ao GitHub](docs/03-subir-ao-github.md).

## Tecnologias usadas

- **Android:** Kotlin, Jetpack Compose, CameraX, ML Kit e Retrofit.
- **Back-end:** Python, FastAPI, SQLAlchemy e JWT.
- **Banco:** MySQL 8.

## Atenção antes de publicar

- Nunca envie o arquivo `backend/.env`: ele contém a senha do MySQL e a chave de login.
- O arquivo `backend/.env.example` é seguro e serve de modelo para cada integrante criar o próprio `.env`.
- Não envie pastas `build`, `.gradle`, `.idea` ou `.venv`; o `.gitignore` já faz isso automaticamente.
- A assinatura facial deste protótipo guarda somente medidas numéricas normalizadas, não fotos nem vídeos. Para uso real em escola são necessários consentimento, proteção de dados e uma solução biométrica homologada.

## Estado atual do protótipo

- Login para administrador, professor e aluno;
- Cadastro de escola, turmas, alunos, professores e matrículas pela API;
- Professor abre uma aula válida por uma hora;
- Aluno consulta aulas abertas e registra presença;
- Câmera frontal no aplicativo, piscada para demonstração de prova de vida e comparação geométrica de assinatura facial;
- Histórico de presenças, atrasos e faltas;
- Painel de turma para o professor;
- Banco MySQL com campo para a assinatura facial e seu hash de integridade.
