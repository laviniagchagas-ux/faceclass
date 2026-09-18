# Como rodar o FaceClass no computador da escola

Este guia serve para uma apresentação em outro computador. Faça a preparação com antecedência, porque instalar Android Studio e dependências pode demorar.

## O que precisa existir no computador

- MySQL 8 e MySQL Workbench;
- Python 3.11 ou 3.12;
- Android Studio, Android SDK 35 e Java 21;
- a pasta `FaceClass-GitHub` copiada por pendrive, nuvem ou GitHub;
- internet na primeira abertura do Android Studio, para ele baixar dependências do Gradle.

## Preparação única do banco

1. Abra o **MySQL Workbench** e conecte como `root`.
2. Abra uma aba SQL nova.
3. Use **File > Open SQL Script** e selecione `backend/database/01_schema_faceclass.sql`.
4. Clique no raio para executar o arquivo inteiro.
5. Atualize a área **Schemas**; o schema `face_class` deve aparecer.

> Use `01_schema_faceclass.sql` somente em banco novo. Se o computador já tiver um banco FaceClass antigo com dados, não rode esse arquivo de novo. Consulte a tabela de migrações no documento [Código explicado](01-codigo-explicado.md).

## Preparação única do back-end

Abra o PowerShell dentro da pasta `backend`. Troque o caminho abaixo se você tiver copiado a pasta para outro lugar:

```powershell
cd "C:\caminho\para\FaceClass-GitHub\backend"
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
notepad .env
```

No arquivo `.env`, ajuste principalmente a linha `DATABASE_URL` para a senha do MySQL daquele computador. Exemplo, caso o usuário seja `root`:

```text
DATABASE_URL=mysql+pymysql://root:SUA_SENHA_DO_MYSQL@localhost:3306/face_class
```

Salve e feche o Bloco de Notas. O `.env` fica somente nesse computador e nunca deve ser enviado ao GitHub.

Se o comando `py -3.12` não existir, tente:

```powershell
python -m venv .venv
```

## Toda vez que for apresentar

### 1. Ligar o MySQL

1. Pesquise **Serviços** no Windows.
2. Abra o aplicativo **Serviços**.
3. Encontre **MySQL80**.
4. Clique com o botão direito e escolha **Iniciar**.

No Workbench, tente conectar. Se conectar, o banco está ligado.

### 2. Ligar a API Python

Abra PowerShell e execute:

```powershell
cd "C:\caminho\para\FaceClass-GitHub\backend"
.\.venv\Scripts\Activate.ps1
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

O resultado esperado inclui:

```text
Uvicorn running on http://0.0.0.0:8000
Application startup complete.
```

Deixe essa janela aberta. Para parar a API depois da apresentação, pressione `Ctrl + C`.

### 3. Conferir rapidamente

Abra no navegador:

```text
http://127.0.0.1:8000/docs
```

Se a documentação Swagger abrir, API e navegador estão se comunicando. A página `http://127.0.0.1:8000/health` deve responder `status: ok`.

### 4. Abrir o Android

1. Abra Android Studio.
2. Escolha **Open** e selecione a pasta `FaceClass-GitHub/android`.
3. Aguarde o Gradle terminar a sincronização.
4. Escolha o emulador e clique no botão ▶.

No emulador, o endereço `10.0.2.2:8000` já encontra o back-end no mesmo computador.

## Se for usar celular físico

Computador e celular precisam estar na mesma rede Wi-Fi.

1. No PowerShell, execute `ipconfig` e anote o **Endereço IPv4** do Wi-Fi, por exemplo `192.168.0.25`.
2. Em `android/app/build.gradle.kts`, substitua `10.0.2.2` pelo IPv4 do computador:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://192.168.0.25:8000/\"")
```

3. Faça Sync no Android Studio e rode de novo.
4. Se o Windows perguntar sobre Firewall, permita acesso na rede privada para a porta 8000.

Nunca use `localhost` no celular: para ele, `localhost` é o próprio celular, não o computador.

## Problemas comuns

| Mensagem | Causa | Como resolver |
| --- | --- | --- |
| `No module named 'app'` | Uvicorn foi iniciado na pasta errada | use `cd` para entrar em `FaceClass-GitHub/backend` antes do comando |
| `Can't connect to MySQL server on 'localhost'` | serviço MySQL desligado | inicie o serviço **MySQL80** no Windows |
| `401 Unauthorized` | token antigo, sem login ou perfil sem permissão | saia do app, entre novamente e confira o perfil |
| app não conecta ao servidor | Python desligado ou endereço da API errado | deixe Uvicorn aberto; no emulador use `10.0.2.2` |
| Gradle reclama de Java 25 | Gradle 8.13 não aceita Java 25 | instale/seleciona Java 21 no Android Studio |
| câmera preta | emulador sem webcam configurada | configure Webcam0 para câmera frontal no Device Manager |
