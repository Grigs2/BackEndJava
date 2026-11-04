# Configurações necessárias para executar o Tio da Perua

**Atualizado em:** 02/11/2026  
Este guia descreve as configurações encontradas no código atual. Os valores secretos devem ser definidos fora do repositório.

## 1. Pré-requisitos locais

- Java 21 para o backend.
- PostgreSQL acessível em `localhost:5432`, com o banco `TioDaPerua` criado. A configuração atual usa esse endereço por padrão e permite substituição da URL por `SPRING_DATASOURCE_URL`.
- Node.js e npm para o frontend Expo.
- O backend usa o contexto `/TioDaPerua/api/` e a porta 8080 por padrão.

Em `application.properties`, usuário e senha do PostgreSQL estão fixos no arquivo; não há variáveis `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` configuradas atualmente. Para usar outras credenciais, é necessário adequar essa configuração sem versionar senhas. O log analisado já mostrou conexão bem-sucedida com o banco local configurado.

## 2. Obrigatória para iniciar o backend: chave JWT

Defina `JWT_SECRET_BASE64` antes de iniciar o backend. O valor deve ser Base64 e, depois de decodificado, conter pelo menos 32 bytes aleatórios. Não use senha humana, não reutilize uma chave publicada e não gere uma chave nova a cada inicialização: a chave precisa permanecer estável para validar tokens emitidos anteriormente.

Para gerar e definir uma chave aleatória apenas na sessão atual do PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Mantenha esse mesmo terminal aberto e inicie o backend nele. A variável `$env:` vale somente para processos iniciados a partir daquela sessão. Para iniciar pelo botão de execução do VS Code, configure a variável no ambiente de usuário do Windows e reabra o VS Code para que ele a herde. Não coloque a chave em código, neste documento, em `.env` sem suporte de carregamento, nem em arquivos versionados.

## 3. Primeiro administrador

O bootstrap ADMIN está habilitado por padrão (`ADMIN_BOOTSTRAP_ENABLED=true`). Se o banco ainda não tiver uma conta ADMIN, configure:

| Variável | Quando necessária | Regra |
|---|---|---|
| `ADMIN_EMAIL` | Primeiro bootstrap ADMIN | Deve ser um e-mail que ainda não pertença a outro perfil. |
| `ADMIN_PASSWORD` | Primeiro bootstrap ADMIN | Pelo menos 12 caracteres e no máximo 72 bytes UTF-8. Use uma senha forte e única. |
| `ADMIN_BOOTSTRAP_ENABLED` | Opcional | `false` desativa o bootstrap automático; use somente se o ADMIN já tiver sido criado por outro procedimento. |

Se já houver um ADMIN consistente no banco, o bootstrap não exige essas credenciais para criar outro. Se não houver ADMIN e o bootstrap estiver habilitado sem as duas variáveis, a inicialização falhará na etapa de bootstrap, mesmo depois de corrigir `JWT_SECRET_BASE64`. Defina os valores no ambiente do processo ou no mecanismo de segredos do ambiente de implantação; não os grave no código nem em scripts compartilhados.

## 4. E-mail de recuperação de senha (opcional para iniciar)

O envio de recuperação usa SendGrid. Essas configurações não são necessárias para subir a aplicação, mas são necessárias para enviar e-mails de redefinição:

| Variável | Finalidade |
|---|---|
| `SENDGRID_API_KEY` | Chave de API do SendGrid com permissão de envio de e-mail. |
| `SENDGRID_FROM_EMAIL` | Remetente verificado/autorizado na conta SendGrid. |
| `PASSWORD_RESET_URL` | URL do frontend que receberá o parâmetro `token` no link. |

O frontend atual ainda não apresenta um fluxo de redefinição que consuma esse token. Antes de oferecer recuperação de senha aos usuários, implemente e teste a tela/rota correspondente e confirme que ela chama o endpoint de redefinição da API. Não use um endereço fictício em produção.

## 5. Versões de termos e consentimento

Estas variáveis não bloqueiam a inicialização:

- `TERMS_VERSION`: versão dos termos efetivamente aprovados para os cadastros; configure-a para manter a versão aceita registrada. Sem valor, o cadastro ainda pode prosseguir, mas a versão fica nula.
- `GPS_CONSENT_VERSION`: versão da política de consentimento de localização. Defina uma versão ao publicar/atualizar a política e mantenha-a estável enquanto aquela versão estiver vigente.

## 6. Variáveis com padrão existente

Não são obrigatórias para iniciar; os valores abaixo são os padrões do backend:

| Variável | Padrão | Uso |
|---|---:|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/TioDaPerua` | URL JDBC do PostgreSQL. |
| `JWT_ACCESS_TOKEN_MINUTES` | `15` | Duração do access token. |
| `JWT_REFRESH_TOKEN_DAYS` | `30` | Duração do refresh token. |
| `ADMIN_BOOTSTRAP_ENABLED` | `true` | Habilita o bootstrap do primeiro ADMIN. |

## 7. Executar

1. Inicie o PostgreSQL e confirme que o banco `TioDaPerua` está acessível com as credenciais que o backend usa.
2. Configure `JWT_SECRET_BASE64` e, se ainda não houver ADMIN no banco, `ADMIN_EMAIL` e `ADMIN_PASSWORD` no ambiente do processo.
3. No PowerShell, na pasta `BackEnd\BackEndJava`, execute:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Considere o backend iniciado somente quando o log confirmar que a aplicação Spring Boot terminou de iniciar sem `Application run failed`. No erro revisado, o Tomcat começou a subir, mas foi encerrado porque a chave JWT não estava configurada.
5. Para iniciar o frontend, em outro terminal, na pasta `FrontEnd\frontendreact`, execute:

   ```powershell
   npm install
   npm start
   ```

   Abra o QR code no Expo Go. O frontend usa atualmente `http://localhost:8080/TioDaPerua/api` em `src/services/api.ts`. No navegador do mesmo computador, `localhost` é adequado se o backend estiver rodando localmente. Em um celular físico, altere a URL para o IP local do computador que executa o backend; deixe ambos na mesma rede e permita a conexão na porta 8080 no firewall. Essa URL está fixa no código e não é carregada de uma variável de ambiente.

## 8. Serviços externos e produção

- Não foi identificada uma chave de mapas/roteirização exigida pelo backend atual. A documentação da implementação mantém a escolha do provedor pendente; não crie nem pague uma chave de mapas para corrigir a falha de inicialização descrita.
- Uma chave SendGrid só é necessária para o envio de redefinição de senha, conforme a seção 4.
- A aplicação não carrega automaticamente um arquivo `.env`; defina variáveis no processo, na configuração de execução do VS Code ou no gerenciador de segredos do ambiente.
- Em produção, use segredos gerenciados, HTTPS, credenciais de banco externas ao código, origens CORS apropriadas e `spring.jpa.show-sql=false`. Nunca reutilize a chave JWT de desenvolvimento em produção.
