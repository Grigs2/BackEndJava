#!/usr/bin/env python3
"""Monta um cenário de uso sobre os perfis criados por seed_perfis.py, para testar o app sem cadastrar nada à mão.

O que faz (pode rodar várias vezes; o que já existe é reaproveitado):
  1. Documentos dos motoristas (CNH, vistoria, licenciamento) enviados e aprovados pelo ADMIN
     (precisa de ADMIN_EMAIL e ADMIN_PASSWORD no ambiente; sem eles, só envia).
  2. Viagens de ida e volta de cada motorista, nos turnos dos alunos da escola dele.
  3. Convites do motorista aceitos pelos responsáveis. Exceção: BH fica com o convite PENDENTE.
  4. Consentimento de GPS do motorista e dos responsáveis (precisa de GPS_CONSENT_VERSION no backend).
  5. Viagens de hoje:
     - SP manhã-ida EM ANDAMENTO, com uma posição de GPS (mapa ao vivo);
     - RJ manhã-ida EM ANDAMENTO, alunos já embarcados e SEM posição (mapa alternativo em linha de metrô);
     - RJ tarde-ida só PREPARADA ("Aguardando início").

Com --simular-gps, depois do cenário fica enviando a posição da perua de SP a cada 15 s (Ctrl+C para parar).
"""
import argparse
import json
import math
import os
import sys
import time
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from seed_perfis import ARQUIVO_DADOS, URL_PADRAO, Api, ErroSeed  # noqa: E402

HOJE = date.today()

# RENAVAM fictício (11 dígitos) por motorista, só para o licenciamento.
RENAVAM = {"motorista-sp": "01234567890", "motorista-rj": "09876543210", "motorista-bh": "05555555555"}

# Coordenadas aproximadas do bairro do motorista de SP (Pinheiros), usadas na posição de teste.
POSICAO_SP = (-23.56160, -46.68790)

# Cenário de hoje por motorista: quais viagens preparar/iniciar e o que fazer com os alunos.
CENARIO_HOJE = {
    "motorista-sp": {"MANHA_IDA": {"iniciar": True, "gps": True, "embarcar": False}},
    "motorista-rj": {
        "MANHA_IDA": {"iniciar": True, "gps": False, "embarcar": True},
        "TARDE_IDA": {"iniciar": False},
    },
}
# Motoristas cujos convites ficam pendentes (para testar convite e popout de notificações).
CONVITE_PENDENTE = {"motorista-bh"}


def log(msg):
    print(msg, flush=True)


def aviso(msg):
    print(f"  ! {msg}", flush=True)


def turnos(periodoAluno):
    """Turno do aluno (MANHA_IDA -> MANHA) vira as viagens de ida e de volta desse turno."""
    turno = periodoAluno.split("_")[0]
    return [f"{turno}_IDA", f"{turno}_VOLTA"]


class Cenario:
    def __init__(self, api, dados):
        self.api = api
        self.dados = dados
        self.senha = dados["senhaPadrao"]
        self.dominio = dados["dominioEmail"]
        self.tokens = {}
        self.perfis = {}

    # ------------------------------------------------------------------ sessões
    def entrar(self, email, senha=None):
        if email not in self.tokens:
            token, perfil = self.api.entrar(email, senha or self.senha)
            if not token:
                raise ErroSeed(f"Não foi possível entrar com {email}. Rode antes o seed_perfis.py.")
            self.tokens[email], self.perfis[email] = token, perfil
        return self.tokens[email], self.perfis[email]

    def emailEscola(self, ref):
        return f"{ref}@{self.dominio}"

    # ------------------------------------------------------------------ 1. documentos
    def documentos(self, motorista, tokenAdmin):
        token, perfil = self.entrar(motorista["email"])
        mid = perfil["id"]
        existentes = self.api.exigir("GET", f"/Motorista/{mid}/Documentos", token=token) or []
        efetivos = {d["tipo"]: d for d in existentes if not d.get("substituidoEm")}
        pedidos = {
            "CNH": {"tipo": "CNH", "numero": motorista["cnh"], "categoria": "D",
                    "dataValidade": str(HOJE + timedelta(days=3 * 365))},
            "VISTORIA": {"tipo": "VISTORIA", "numero": f"VIST-{motorista['veiculo']['placa']}",
                         "dataRealizacao": str(HOJE - timedelta(days=30)),
                         "dataValidade": str(HOJE + timedelta(days=335))},
            "LICENCIAMENTO": {"tipo": "LICENCIAMENTO", "numero": RENAVAM.get(motorista["ref"], "01234567890"),
                              "dataValidade": str(HOJE + timedelta(days=365))},
        }
        for tipo, corpo in pedidos.items():
            atual = efetivos.get(tipo)
            if atual and atual["status"] in ("APROVADO", "PENDENTE"):
                continue
            self.api.exigir("POST", f"/Motorista/{mid}/Documentos", corpo, token=token)
            log(f"    Documento {tipo} enviado")
        if not tokenAdmin:
            return
        pendentes = self.api.exigir("GET", "/Admin/Documentos/Pendentes", token=tokenAdmin) or []
        for doc in pendentes:
            status, resp = self.api.chamar("PUT", f"/Admin/Documentos/{doc['id']}/Revisar",
                                           {"status": "APROVADO"}, token=tokenAdmin)
            if status in (200, 204):
                log(f"    Documento {doc.get('tipo')} #{doc['id']} aprovado pelo ADMIN")
            elif status != 409:
                aviso(f"ADMIN não aprovou o documento #{doc['id']}: {status} {resp}")

    # ------------------------------------------------------------------ 2. viagens
    def viagens(self, motorista):
        token, perfil = self.entrar(motorista["email"])
        mid = perfil["id"]
        _, perfilEscola = self.entrar(self.emailEscola(motorista["escolaRef"]))
        alunos = [d for r in self.dados["responsaveis"] for d in r["dependentes"]
                  if d["escolaRef"] == motorista["escolaRef"]]
        periodos = sorted({p for a in alunos for p in turnos(a["periodo"])})
        existentes = {v["periodo"]: v for v in (self.api.exigir("GET", f"/Viagem/ListarViagens/{mid}", token=token) or [])
                      if v.get("ativo") is not False}
        resultado = {}
        for periodo in periodos:
            if periodo in existentes:
                resultado[periodo] = existentes[periodo]["id"]
                continue
            criada = self.api.exigir("POST", f"/Viagem/CriarViagem/{mid}",
                                     {"periodo": periodo, "escolas": [{"id": perfilEscola["id"], "nome": None}]},
                                     token=token)
            resultado[periodo] = criada["id"]
            log(f"    Viagem {periodo} criada (id {criada['id']})")
        return resultado

    # ------------------------------------------------------------------ 3. convites
    def convites(self, motorista, viagensIds):
        tokenMot, _ = self.entrar(motorista["email"])
        pendente = motorista["ref"] in CONVITE_PENDENTE
        for responsavel in self.dados["responsaveis"]:
            meus = [d for d in responsavel["dependentes"] if d["escolaRef"] == motorista["escolaRef"]]
            if not meus:
                continue
            tokenResp, perfilResp = self.entrar(responsavel["email"])
            perfilCompleto = self.api.exigir("GET", "/Responsavel/Perfil", token=tokenResp)
            idsPorNome = {d["nome"]: d["id"] for d in perfilCompleto.get("dependentes") or []}
            for dep in meus:
                depId = idsPorNome.get(dep["nome"])
                if not depId:
                    aviso(f"Aluno {dep['nome']} não encontrado no perfil de {responsavel['email']}")
                    continue
                for periodo in turnos(dep["periodo"]):
                    if pendente and periodo.endswith("VOLTA"):
                        continue  # um convite pendente basta para o teste
                    self.convite(tokenMot, tokenResp, perfilResp["id"], viagensIds[periodo], depId, dep["nome"],
                                 periodo, aceitar=not pendente)

    def convite(self, tokenMot, tokenResp, respId, viagemId, depId, nome, periodo, aceitar):
        solicitacoes = self.api.exigir("GET", f"/Responsavel/ListarSolicitacoes/{respId}", token=tokenResp) or []
        daViagem = [s for s in solicitacoes if s.get("viagemId") == viagemId and s.get("dependenteId") == depId]
        if any(s.get("aceito") and not s.get("dataFim") for s in daViagem):
            return
        aberta = next((s for s in daViagem if not s.get("respondido") and s.get("status") in (None, "PENDENTE")), None)
        if not aberta:
            status, resp = self.api.chamar("POST", f"/Viagem/SolicitarDependente/{viagemId}/{depId}", token=tokenMot)
            if status not in (200, 201):
                aviso(f"Convite de {nome} para {periodo} não enviado: {status} {resp}")
                return
            aberta = resp
            log(f"    Convite enviado: {nome} ({periodo})")
        if not aceitar:
            log(f"    Convite de {nome} ({periodo}) deixado PENDENTE para teste")
            return
        status, resp = self.api.chamar("POST", f"/Responsavel/ResponderSolicitacao/{aberta['id']}",
                                       {"aceito": True, "compartilhamentoAutorizado": True}, token=tokenResp)
        if status in (200, 201):
            log(f"    Convite aceito: {nome} ({periodo})")
        else:
            aviso(f"Convite de {nome} ({periodo}) não aceito: {status} {resp}")

    # ------------------------------------------------------------------ 4. consentimento GPS
    def consentimentos(self):
        for motorista in self.dados["motoristas"]:
            token, _ = self.entrar(motorista["email"])
            if not self.consentir("/Viagem/Localizacao/Consentimento/Motorista", token, motorista["email"]):
                return False
        for responsavel in self.dados["responsaveis"]:
            token, _ = self.entrar(responsavel["email"])
            perfil = self.api.exigir("GET", "/Responsavel/Perfil", token=token)
            for dep in perfil.get("dependentes") or []:
                self.consentir(f"/Viagem/Localizacao/Consentimento/Dependente/{dep['id']}", token, dep["nome"])
        log("  Consentimentos de GPS registrados")
        return True

    def consentir(self, caminho, token, quem):
        status, resp = self.api.chamar("PUT", caminho, {"aceito": True}, token=token)
        if status == 503:
            aviso("GPS desativado no backend (GPS_CONSENT_VERSION não definida). "
                  "Defina a variável e reinicie o backend para testar o mapa ao vivo.")
            return False
        if status not in (200, 201):
            aviso(f"Consentimento de {quem} não registrado: {status} {resp}")
        return True

    # ------------------------------------------------------------------ 5. viagens de hoje
    def hoje(self, motorista, viagensIds, gpsDisponivel):
        plano = CENARIO_HOJE.get(motorista["ref"])
        if not plano:
            return None
        token, _ = self.entrar(motorista["email"])
        execucaoGps = None
        for periodo, opcoes in plano.items():
            viagemId = viagensIds.get(periodo)
            if not viagemId:
                continue
            dia = self.api.exigir("POST", f"/Viagem/PrepararViagemDia/{viagemId}", {"data": str(HOJE)}, token=token)
            vdId = dia["id"]
            log(f"    {periodo} de hoje preparada (execução {vdId}, {dia['status']})")
            if not opcoes.get("iniciar"):
                continue
            if dia["status"] == "PLANEJADA":
                status, resp = self.api.chamar("PUT", "/Viagem/AlterarStatusViagemDia",
                                               {"idViagemDia": vdId, "novoStatus": "EM_ANDAMENTO"}, token=token)
                if status != 200:
                    pront = self.api.chamar("GET", f"/Viagem/Prontidao/{viagemId}", token=token)[1]
                    aviso(f"Não foi possível iniciar {periodo}: {status} {resp}. Prontidão: {pront}")
                    continue
                log(f"    {periodo} INICIADA")
            elif dia["status"] != "EM_ANDAMENTO":
                log(f"    {periodo} de hoje já está {dia['status']}; nada a fazer")
                continue
            if opcoes.get("embarcar"):
                paradas = self.api.exigir("GET", f"/Viagem/BuscarParadasViagemDia/{vdId}", token=token) or []
                vistos = set()
                for parada in paradas:
                    for aluno in parada.get("listaDependentes") or []:
                        # O aluno aparece na parada de embarque e na de desembarque; registra uma vez só.
                        if aluno["id"] in vistos:
                            continue
                        vistos.add(aluno["id"])
                        if aluno.get("statusEmbarque") == "ESPERANDO":
                            self.api.exigir("PUT", "/Viagem/AlterarStatusPresenca",
                                            {"idViagemDia": vdId, "idDependente": aluno["id"], "novoStatus": "EMBARCADO"},
                                            token=token)
                            log(f"      {aluno['nomeDependente']} EMBARCOU")
            if opcoes.get("gps") and gpsDisponivel:
                self.posicao(motorista["email"], vdId, 0)
                execucaoGps = (motorista["email"], vdId)
        return execucaoGps

    def posicao(self, email, vdId, passo):
        token, _ = self.entrar(email)
        # Pequeno deslocamento circular para a perua "andar" no mapa.
        lat = POSICAO_SP[0] + 0.0015 * math.sin(passo / 6)
        lon = POSICAO_SP[1] + 0.0015 * math.cos(passo / 6)
        corpo = {"idViagemDia": vdId, "latitude": round(lat, 5), "longitude": round(lon, 5),
                 "instanteDispositivo": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")}
        status, resp = self.api.chamar("PUT", "/Viagem/Localizacao", corpo, token=token)
        if status == 401:
            # O token de acesso dura 15 minutos: entra de novo e tenta mais uma vez.
            self.tokens.pop(email, None)
            token, _ = self.entrar(email)
            status, resp = self.api.chamar("PUT", "/Viagem/Localizacao", corpo, token=token)
        if status == 200:
            log(f"    Posição enviada ({corpo['latitude']}, {corpo['longitude']})")
        elif status == 409 and "15 segundos" in str(resp):
            pass
        else:
            aviso(f"Posição não enviada: {status} {resp}")


def principal():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", default=None, help=f"Base da API (padrão: TIO_API_URL ou {URL_PADRAO})")
    parser.add_argument("--dados", default=str(ARQUIVO_DADOS), help="Arquivo JSON com os perfis")
    parser.add_argument("--simular-gps", action="store_true",
                        help="Depois do cenário, envia a posição da perua de SP a cada 15 s até Ctrl+C")
    args = parser.parse_args()

    base = args.url or os.environ.get("TIO_API_URL", URL_PADRAO)
    dados = json.loads(Path(args.dados).read_text(encoding="utf-8"))
    for escola in dados["escolas"]:
        escola["email"] = f"{escola['ref']}@{dados['dominioEmail']}"
    api = Api(base)
    cenario = Cenario(api, dados)

    try:
        api.chamar("POST", "/Autenticacao/Entrar", {"email": "ping@invalido.test", "senha": "x"})
    except OSError as erro:
        print(f"API indisponível em {base}: {erro}. Suba o backend antes.", file=sys.stderr)
        return 1

    log(f"Cenário em {base} (data de hoje: {HOJE})")
    try:
        tokenAdmin = None
        adminEmail, adminSenha = os.environ.get("ADMIN_EMAIL"), os.environ.get("ADMIN_PASSWORD")
        if adminEmail and adminSenha:
            tokenAdmin, _ = api.entrar(adminEmail, adminSenha)
            if not tokenAdmin:
                aviso("ADMIN_EMAIL/ADMIN_PASSWORD não entram; documentos ficarão em análise.")
        else:
            aviso("ADMIN_EMAIL/ADMIN_PASSWORD não definidos; documentos ficarão em análise.")

        viagensPorMotorista = {}
        for motorista in dados["motoristas"]:
            log(f"Motorista {motorista['nome']}:")
            cenario.documentos(motorista, tokenAdmin)
            viagensPorMotorista[motorista["ref"]] = cenario.viagens(motorista)
            cenario.convites(motorista, viagensPorMotorista[motorista["ref"]])

        log("GPS:")
        gpsDisponivel = cenario.consentimentos()

        log("Viagens de hoje:")
        execucaoGps = None
        for motorista in dados["motoristas"]:
            resultado = cenario.hoje(motorista, viagensPorMotorista[motorista["ref"]], gpsDisponivel)
            execucaoGps = execucaoGps or resultado
    except ErroSeed as erro:
        print(f"\nFalha: {erro}", file=sys.stderr)
        print("Corrija a causa e rode de novo: o que já foi criado é reaproveitado.", file=sys.stderr)
        return 2

    log("\nCenário pronto. Contas e senha em scripts/seed-perfis-teste/credenciais.txt.")
    if args.simular_gps:
        if not execucaoGps:
            aviso("Sem viagem de SP em andamento com GPS; nada para simular.")
            return 0
        email, vdId = execucaoGps
        log("Simulando GPS da perua de SP a cada 15 s (Ctrl+C para parar)...")
        passo = 1
        try:
            while True:
                time.sleep(16)
                cenario.posicao(email, vdId, passo)
                passo += 1
        except KeyboardInterrupt:
            log("Simulação encerrada.")
    return 0


if __name__ == "__main__":
    sys.exit(principal())
