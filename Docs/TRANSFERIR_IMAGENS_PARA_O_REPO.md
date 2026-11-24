# Ver as imagens dos documentos no VS Code do Codespace

As imagens enviadas pelo front ficam no volume Docker `documentos_motorista`, montado em `/var/lib/tiodaperua/documentos`. Essa pasta fica fora do repositório, então não aparece no explorador do VS Code.

Este tutorial copia as imagens para a pasta oculta `.imagens-transferencia/` dentro do repositório, para que possam ser abertas no VS Code. Essa pasta está no `.gitignore`, então não vai para o Git.

## Atenção

- As imagens são dados pessoais de motoristas. Não as mova para pastas que não estejam no `.gitignore`.
- Apague a cópia quando terminar (passo 4).

## Passo 1: abrir o terminal do Codespace

No VS Code do Codespace, abra o terminal integrado (`Ctrl+'`). Ele roda dentro do container da aplicação, que tem acesso ao volume.

## Passo 2: copiar as imagens

Rode o bloco abaixo no terminal:

```bash
SRC=/var/lib/tiodaperua/documentos
DEST=/workspaces/BackEndJava/.imagens-transferencia

mkdir -p "$DEST"
find "$SRC" -type f | while read -r f; do
  if head -c 4 "$f" | od -An -tx1 | grep -q '89 50 4e 47'; then ext=png; else ext=jpg; fi
  rel=${f#"$SRC"/}
  mkdir -p "$DEST/$(dirname "$rel")"
  cp "$f" "$DEST/$rel.$ext"
done
```

O script detecta o formato pela assinatura do arquivo (PNG ou JPEG) e acrescenta a extensão na cópia. Os arquivos originais não são alterados.

A pasta `.imagens-transferencia/` só aparece depois deste passo. Antes de rodar o script, ela não existe no repositório. Se ainda não aparecer no explorador, clique em "Atualizar" (ícone de atualizar na barra do explorador) ou rode `ls -la /workspaces/BackEndJava` para conferir.

## Passo 3: abrir a imagem

No explorador do VS Code, abra a pasta `.imagens-transferencia/`. A estrutura é:

```
.imagens-transferencia/
└── motoristas/
    └── {idMotorista}/
        └── documentos/
            └── {idDocumento}/
                ├── FRENTE.jpg
                └── VERSO.png
```

Clique no arquivo para abrir o visualizador de imagens.

Para listar só o caminho dos arquivos no terminal:

```bash
find /workspaces/BackEndJava/.imagens-transferencia -type f
```

## Passo 4: apagar a cópia

Quando terminar, remova a pasta de transferência:

```bash
rm -rf /workspaces/BackEndJava/.imagens-transferencia
```

## Conferir que a pasta está ignorada pelo Git

```bash
cd /workspaces/BackEndJava && git check-ignore -v .imagens-transferencia/
```

A saída deve mostrar a regra `.gitignore` com `.imagens-transferencia/`. Se não mostrar nada, não copie as imagens e avise o time.
