# Ver as imagens dos documentos no Codespace

As imagens enviadas pelo front ficam cifradas no volume Docker `documentos_motorista`, montado em `/var/lib/tiodaperua/documentos`. Essa pasta fica fora do repositório, e os arquivos não são legíveis diretamente: copiá-los do disco gera apenas dados cifrados. Para ver uma imagem, use a API, que decifra o arquivo e confere a integridade (SHA-256).

## Atenção

- As imagens são dados pessoais de motoristas. Não as salve em pastas versionadas.
- O volume é zerado quando o container é reconstruído. Os registros de documento continuam no banco, mas o arquivo pode desaparecer.

## Ver uma imagem pela API

Como admin, com um access token válido:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/TioDaPerua/api/Admin/Documentos/{idDocumento}/Imagens/FRENTE" -o /tmp/frente.jpg
```

Troque `FRENTE` por `VERSO` ou `UNICA`, conforme o tipo. Cada visualização é registrada na auditoria. Apague o arquivo temporário ao terminar: `rm -f /tmp/frente.jpg`.

## Validar que o arquivo está cifrado

Os 12 primeiros bytes são o IV e não seguem a assinatura de JPEG (`FF D8 FF`) nem de PNG (`89 50 4E 47`). Para conferir, rode no terminal do Codespace (container da aplicação):

```bash
find /var/lib/tiodaperua/documentos -type f | head -n 1 | xargs -I{} sh -c 'od -An -tx1 -N16 "{}"'
```

A saída não deve começar com `ff d8 ff` nem com `89 50 4e 47`. Para comparar com o original, envie uma imagem de teste e confirme que `curl` (acima) devolve a imagem legível, enquanto o arquivo em disco não é.

## Apagar a cópia temporária

```bash
rm -f /tmp/frente.jpg
```
