# Back-end FaceClass

Esta pasta contém a API Python e os scripts do MySQL.

- O ponto de entrada é `app/main.py`.
- O banco novo é criado por `database/01_schema_faceclass.sql`.
- As variáveis do computador ficam em `.env`, criado a partir de `.env.example`.
- A API é iniciada com `python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`.

Leia os guias da raiz do projeto para os detalhes:

- [O que cada parte do código faz](../docs/01-codigo-explicado.md)
- [Como rodar no computador da escola](../docs/02-rodar-na-escola.md)
