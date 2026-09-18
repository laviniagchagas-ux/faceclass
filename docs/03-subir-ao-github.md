# Como subir ao GitHub

## Jeito mais fácil: GitHub Desktop

1. Instale e abra o **GitHub Desktop**.
2. Clique em **File > Add local repository**.
3. Escolha a pasta `FaceClass-GitHub`.
4. Se ele disser que a pasta ainda não é um repositório, clique em **Create a repository**.
5. Escreva uma mensagem de commit, por exemplo: `Primeira versão organizada do FaceClass`.
6. Clique em **Commit to main**.
7. Clique em **Publish repository**.
8. Escolha o nome `faceclass` e confirme se o grupo quer o repositório público ou privado antes de publicar.

## Jeito pelo terminal

No PowerShell, dentro da pasta `FaceClass-GitHub`:

```powershell
git init
git add .
git status
git commit -m "Primeira versão organizada do FaceClass"
```

Depois, no GitHub, crie um repositório vazio chamado `faceclass`. Copie a URL que o site mostrar e execute:

```powershell
git branch -M main
git remote add origin COLE_A_URL_DO_REPOSITORIO_AQUI
git push -u origin main
```

## Antes de clicar em publicar

Confira com `git status` que não aparecem:

- `backend/.env`;
- senhas, tokens ou arquivos de banco com dados reais;
- `.venv`, `build`, `.gradle` ou `.idea`.

O `.gitignore` foi criado justamente para esconder esses arquivos. Se um arquivo confidencial já foi enviado ao GitHub, não basta apagá-lo depois: avise o grupo e troque as senhas/chaves envolvidas.
