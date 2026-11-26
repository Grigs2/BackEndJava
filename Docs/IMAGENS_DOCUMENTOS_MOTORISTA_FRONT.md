# Imagens dos documentos do motorista (guia para o front)

Este guia descreve o que o backend implementou para o upload e a leitura das imagens dos documentos do motorista (CNH, vistoria e licenciamento). Ele segue o contrato em `docs/CONTRATO_UPLOAD_IMAGENS_DOCUMENTOS_BACK.md` do front-end.

Prefixo de todas as rotas: `/TioDaPerua/api`.

## 1. Regras gerais

| Item | Regra |
|---|---|
| Formatos | JPEG e PNG. O backend valida o conteúdo (assinatura do arquivo), não só a extensão ou o `Content-Type`. |
| Tamanho | 1 MB por arquivo. 2 MB por requisição multipart. |
| Lados por tipo | CNH e LICENCIAMENTO: `FRENTE` e `VERSO`. VISTORIA: `UNICA`. |
| Quando enviar | Só enquanto o documento está `PENDENTE` ou `RECUSADO`. |
| Reenvio | Enviar o mesmo lado de novo substitui a imagem anterior. |
| Submissão substituída | Não aceita imagens. Retorna 409. |
| Aprovação | Exige todos os lados do tipo. Sem eles, o admin recebe 409. |
| Recusa | Não exige imagens. |
| Nova submissão | Ao criar uma nova submissão do mesmo tipo, as imagens da anterior são apagadas. |

Lados são valores fixos: `FRENTE`, `VERSO`, `UNICA`. Aceitam maiúsculas ou minúsculas.

## 2. Permissões

- Rotas do motorista: só o próprio motorista (`id_motorista` do token). Outro motorista recebe 403.
- Rotas do admin: perfil `ADMIN`.

Todas as rotas exigem `Authorization: Bearer <accessToken>`.

## 3. Endpoints do motorista

### 3.1 Enviar imagem

`POST /Motorista/{id_motorista}/Documentos/{idDocumento}/Imagens/{lado}`

- Content-Type: `multipart/form-data`
- Campo do arquivo: `arquivo` (obrigatório)
- Resposta: `201 Created`

```json
{
  "documentoId": 7,
  "lado": "FRENTE",
  "mimeType": "image/jpeg",
  "tamanhoBytes": 183442,
  "enviadoEm": "2026-11-24T07:50:00Z"
}
```

Exemplo com `fetch`:

```js
const form = new FormData();
form.append("arquivo", arquivoSelecionado); // File ou Blob
const resp = await fetch(`${BASE}/Motorista/${idMotorista}/Documentos/${idDocumento}/Imagens/FRENTE`, {
  method: "POST",
  headers: { Authorization: `Bearer ${accessToken}` }, // NAO definir Content-Type manualmente
  body: form,
});
```

Não defina o `Content-Type` manualmente: o navegador precisa colocar o `boundary` do multipart.

### 3.2 Listar imagens de um documento

`GET /Motorista/{id_motorista}/Documentos/{idDocumento}/Imagens`

Resposta `200 OK`: lista de objetos com o mesmo formato do item de 3.1. Lista vazia se não houver imagens.

### 3.3 Baixar imagem

`GET /Motorista/{id_motorista}/Documentos/{idDocumento}/Imagens/{lado}`

Resposta `200 OK` com os bytes da imagem. O `Content-Type` é `image/jpeg` ou `image/png`. Cabeçalho `Cache-Control: private, no-store`.

Para exibir, use um `blob` com o token no header (a imagem não pode ser carregada por uma tag `<img src>` sem o header de autorização):

```js
const resp = await fetch(url, { headers: { Authorization: `Bearer ${accessToken}` } });
const blob = await resp.blob();
imgEl.src = URL.createObjectURL(blob);
```

## 4. Endpoints do admin

### 4.1 Fila de revisão com imagens

`GET /Admin/Documentos/Revisao`

Resposta `200 OK`: lista de `DocumentoRevisaoDTO`. Cada item inclui o novo campo `imagens`:

```json
{
  "documento": { "...": "DocumentoMotoristaDTO" },
  "motoristaId": 20,
  "motoristaNome": "Nome",
  "motoristaIdade": 35,
  "veiculoPlaca": "ABC1D23",
  "veiculoModelo": "Modelo",
  "conferencias": [],
  "podeAprovar": true,
  "imagens": [
    { "documentoId": 7, "lado": "FRENTE", "mimeType": "image/jpeg", "tamanhoBytes": 183442, "enviadoEm": "2026-11-24T07:50:00Z" },
    { "documentoId": 7, "lado": "VERSO", "mimeType": "image/jpeg", "tamanhoBytes": 190110, "enviadoEm": "2026-11-24T07:51:00Z" }
  ]
}
```

`imagens` pode vir vazio. A fila simples `GET /Admin/Documentos/Pendentes` não mudou e não traz imagens.

### 4.2 Visualizar imagem

`GET /Admin/Documentos/{idDocumento}/Imagens/{lado}`

Retorna os bytes da imagem, como em 3.3. Cada visualização é registrada na auditoria (`IMAGEM_DOCUMENTO_VISUALIZADA`).

### 4.3 Revisar documento

`PUT /Admin/Documentos/{idDocumento}/Revisar` (sem alteração no corpo)

- `{"status":"APROVADO"}`: falha com 409 se faltar algum lado obrigatório.
- `{"status":"RECUSADO","motivoRecusa":"..."}`: sem exigência de imagens.

## 5. Erros

Formato padrão de erro do backend. Os códigos relevantes para as imagens:

| Código | Quando |
|---|---|
| 400 | Lado inválido, ou não aplicável ao tipo do documento (ex.: `FRENTE` em VISTORIA). Arquivo vazio ou campo `arquivo` ausente. |
| 401 | Token ausente ou expirado. |
| 403 | Documento de outro motorista, ou usuário sem perfil para a rota. |
| 404 | Documento ou imagem não encontrada. |
| 409 | Documento está `APROVADO` ou `VENCIDO`, ou é uma submissão substituída. Aprovação sem as imagens obrigatórias. |
| 413 | Arquivo acima de 1 MB, ou requisição acima de 2 MB. |
| 415 | Formato diferente de JPEG ou PNG. |

Sugestão para o front: validar tamanho (1 MB) e tipo (`image/jpeg` ou `image/png`) antes de enviar. O backend valida de novo, então o 415 e o 413 ainda podem ocorrer.

## 6. Fluxo sugerido no front

1. Motorista envia o documento (`POST /Motorista/{id}/Documentos`) e recebe o `id` do documento.
2. Para cada lado exigido pelo tipo, envia a imagem (3.1).
3. Confere com `GET .../Imagens` quais lados já foram enviados.
4. Exibe o status do documento. Se `RECUSADO`, permite reenviar as imagens e uma nova submissão.
5. Admin abre `GET /Admin/Documentos/Revisao`, visualiza as imagens (4.2) e decide (4.3).

## 7. Pendências conhecidas

- Retenção: 30 dias após a suspensão da conta, as imagens são apagadas pelo job diário e os documentos afetados voltam para `RECUSADO` com o motivo de reenvio. Reativar antes disso mantém as imagens. O motivo gravado na recusa é "Imagens removidas após 30 dias de suspensão da conta. Reenvie o documento.".
- Início de viagem: documentos pendentes, recusados ou vencidos não bloqueiam o início. Eles aparecem como aviso em `GET /Viagem/Prontidao/{idViagem}` (campo `avisos`, com `pronta: true` quando não há bloqueios). O front deve exibir esse aviso para o motorista regularizar o documento.
- Como a viagem não é bloqueada, o aviso em `avisos` é o único sinal do problema na tela de início. O status detalhado (`RECUSADO`, `motivoRecusa`) continua disponível nos endpoints de documentos do motorista.
- Limite no proxy: o nginx de produção precisa de `client_max_body_size 2m` (fora deste repositório).
