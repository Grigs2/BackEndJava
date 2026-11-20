#!/usr/bin/env bash
# Prepara o ambiente de teste no Codespace do backend: sobe a API (se ainda não estiver no ar),
# cria os perfis de teste e monta o cenário de uso (viagens, vínculos, documentos, GPS e viagens de hoje).
#
# Uso (na raiz do repositório do backend):
#   bash scripts/preparar-ambiente-teste.sh                # sobe a API se precisar e cria tudo
#   bash scripts/preparar-ambiente-teste.sh --simular-gps  # idem e, no fim, fica movendo a perua de SP no mapa
#   bash scripts/preparar-ambiente-teste.sh --so-perfis    # só as contas (sem viagens nem cenário)
#
# Pode rodar quantas vezes quiser: o que já existe é reaproveitado.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEED="$RAIZ/scripts/seed-perfis-teste"
API_URL="${TIO_API_URL:-http://localhost:8080/TioDaPerua/api}"
LOG_BACKEND="${LOG_BACKEND:-/tmp/tiodaperua-backend.log}"
SO_PERFIS=0
ARGS_CENARIO=()
for arg in "$@"; do
  case "$arg" in
    --so-perfis) SO_PERFIS=1 ;;
    --simular-gps) ARGS_CENARIO+=("--simular-gps") ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *) echo "Opção desconhecida: $arg" >&2; exit 1 ;;
  esac
done

passo() { printf '\n==> %s\n' "$1"; }

# Valores padrão só para teste. Em produção, defina versões reais.
export GPS_CONSENT_VERSION="${GPS_CONSENT_VERSION:-teste-codespace}"
export TERMS_VERSION="${TERMS_VERSION:-teste-codespace}"
export TIO_API_URL="$API_URL"

passo "Conferindo o Python 3"
# A imagem do Codespace pode trazer só o python3-minimal, sem módulos como json e urllib.
# Testa os módulos que as seeds usam e instala o pacote completo se faltar algum.
python_completo() {
  command -v python3 >/dev/null 2>&1 && python3 -c "import json, urllib.request, argparse, pathlib, datetime" >/dev/null 2>&1
}
if ! python_completo; then
  echo "Python 3 ausente ou incompleto; instalando o pacote completo..."
  # O update pode falhar por repositórios de terceiros da imagem (ex.: Yarn com chave vencida);
  # as listas do Debian costumam baixar mesmo assim, então segue para a instalação.
  sudo apt-get update -qq || echo "Aviso: 'apt-get update' teve erros; tentando instalar mesmo assim."
  sudo apt-get install -y -qq python3 "libpython3-stdlib" >/dev/null || true
fi
if ! python_completo; then
  echo "Não foi possível preparar o Python 3. Rode: sudo apt-get install -y python3" >&2
  exit 1
fi
python3 --version

api_no_ar() {
  # 400/401 também contam: a API respondeu.
  local codigo
  codigo=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"email":"ping@invalido.test","senha":"x"}' "$API_URL/Autenticacao/Entrar" || true)
  [[ "$codigo" =~ ^(200|400|401)$ ]]
}

passo "Conferindo a API em $API_URL"
if api_no_ar; then
  echo "API já está no ar."
else
  for var in JWT_SECRET_BASE64 ADMIN_EMAIL ADMIN_PASSWORD; do
    if [[ -z "${!var:-}" ]]; then
      echo "A variável $var não está definida. Cadastre-a nos secrets do Codespaces (veja Docs/AMBIENTE_CODESPACE.md)." >&2
      exit 1
    fi
  done
  echo "Subindo o backend em segundo plano (log em $LOG_BACKEND)..."
  chmod +x "$RAIZ/mvnw"
  (cd "$RAIZ" && nohup ./mvnw -q spring-boot:run -Dmaven.test.skip=true >"$LOG_BACKEND" 2>&1 &)
  printf 'Aguardando a API'
  for _ in $(seq 1 90); do
    if api_no_ar; then printf ' pronta.\n'; break; fi
    printf '.'; sleep 2
  done
  if ! api_no_ar; then
    printf '\n' >&2
    echo "A API não respondeu em 3 minutos. Últimas linhas do log:" >&2
    tail -n 40 "$LOG_BACKEND" >&2
    exit 1
  fi
fi

passo "Criando os perfis de teste (escolas, motoristas, responsáveis e alunos)"
python3 "$SEED/seed_perfis.py" --url "$API_URL"

if [[ "$SO_PERFIS" -eq 1 ]]; then
  echo; echo "Pronto (só perfis). Credenciais em $SEED/credenciais.txt"
  exit 0
fi

passo "Montando o cenário de uso (documentos, viagens, vínculos, GPS e viagens de hoje)"
python3 "$SEED/seed_cenario.py" --url "$API_URL" "${ARGS_CENARIO[@]}"

passo "Pronto"
echo "Credenciais: $SEED/credenciais.txt"
echo "Senha de todas as contas de teste: veja 'senhaPadrao' em $SEED/dados.json"
echo "O que testar com cada conta: scripts/seed-perfis-teste/README.md, seção 'Cenário de uso'."
