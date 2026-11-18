#!/usr/bin/env python3
"""Cria os perfis de teste do Tio da Perua (escolas, motoristas, responsáveis e dependentes) pela API.

Usa somente a biblioteca padrão do Python. Os dados ficam em dados.json, ao lado deste script.
Se uma conta já existir (login válido), ela é reaproveitada e não é recriada.
"""
import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

URL_PADRAO = "http://localhost:8080/TioDaPerua/api"
ARQUIVO_DADOS = Path(__file__).resolve().parent / "dados.json"
ARQUIVO_CREDENCIAIS = Path(__file__).resolve().parent / "credenciais.txt"


class ErroSeed(Exception):
    pass


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def chamar(self, metodo, caminho, corpo=None, token=None):
        cabecalhos = {"Content-Type": "application/json", "Accept": "application/json"}
        if token:
            cabecalhos["Authorization"] = f"Bearer {token}"
        dados = json.dumps(corpo).encode("utf-8") if corpo is not None else None
        req = urllib.request.Request(self.base + caminho, data=dados, method=metodo, headers=cabecalhos)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                bruto = resp.read()
                return resp.status, (json.loads(bruto) if bruto else None)
        except urllib.error.HTTPError as erro:
            return erro.code, erro.read().decode("utf-8", errors="replace")

    def exigir(self, metodo, caminho, corpo=None, token=None, esperados=(200, 201, 204)):
        status, resposta = self.chamar(metodo, caminho, corpo, token)
        if status not in esperados:
            raise ErroSeed(f"{metodo} {caminho} retornou {status}: {resposta}")
        return resposta

    def entrar(self, email, senha):
        status, resposta = self.chamar("POST", "/Autenticacao/Entrar", {"email": email, "senha": senha})
        if status == 200:
            return resposta["accessToken"], resposta["perfil"]
        if status == 401:
            return None, None
        raise ErroSeed(f"Login de {email} retornou {status}: {resposta}")


def enderecoLegado(endereco):
    return (f"{endereco['logradouro']}, {endereco['numero']} - {endereco['bairro']}, "
            f"{endereco['cidade']}/{endereco['uf']} - CEP {endereco['cep']}")


def usuarioDTO(conta, tipoPerfil, endereco, senha):
    return {
        "email": conta["email"],
        "senha": senha,
        "endereco": enderecoLegado(endereco),
        "telefone": conta["telefone"],
        "tipoPerfil": tipoPerfil,
        "aceitouTermos": True,
    }


def garantirEscola(api, dados, escola, senha, log):
    email = escola["email"]
    token, perfil = api.entrar(email, senha)
    if token:
        log(f"  Escola ja existe: {escola['nome']} (id {perfil['id']})")
        return perfil["id"], False
    corpo = {
        "nome": escola["nome"],
        "cnpj": escola["cnpj"],
        "usuarioDTO": usuarioDTO(escola, "ESCOLA", escola["endereco"], senha),
        "enderecoOperacionalDTO": escola["endereco"],
    }
    criada = api.exigir("POST", "/Escola/Cadastrar", corpo)
    log(f"  Escola criada: {escola['nome']} (id {criada['id']})")
    return criada["id"], True


def garantirMotorista(api, dados, motorista, senha, log):
    email = motorista["email"]
    token, perfil = api.entrar(email, senha)
    if token:
        log(f"  Motorista ja existe: {motorista['nome']} (id {perfil['id']})")
        return perfil["id"], None, False
    corpo = {
        "nome": motorista["nome"],
        "dataNascimento": motorista["dataNascimento"],
        "cpf": motorista["cpf"],
        "cnh": motorista["cnh"],
        "usuarioDTO": usuarioDTO(motorista, "MOTORISTA", motorista["enderecoOperacional"], senha),
        "enderecoOperacionalDTO": motorista["enderecoOperacional"],
    }
    criado = api.exigir("POST", "/Motorista/Cadastrar", corpo)
    token, _ = api.entrar(email, senha)
    api.exigir("POST", f"/Motorista/CadastrarVeiculo/{criado['id']}", motorista["veiculo"], token=token)
    log(f"  Motorista criado: {motorista['nome']} (id {criado['id']}) com veiculo {motorista['veiculo']['placa']}")
    return criado["id"], token, True


def garantirResponsavel(api, dados, responsavel, senha, escolas, log):
    email = responsavel["email"]
    token, perfil = api.entrar(email, senha)
    if token:
        log(f"  Responsavel ja existe: {responsavel['nome']} (id {perfil['id']}), pulando dependentes")
        return
    corpo = {
        "nome": responsavel["nome"],
        "cpf": responsavel["cpf"],
        "dataNascimento": responsavel["dataNascimento"],
        "usuario": usuarioDTO(responsavel, "RESPONSAVEL", responsavel["endereco"], senha),
    }
    criado = api.exigir("POST", "/Responsavel/Cadastrar", corpo)
    token, _ = api.entrar(email, senha)
    log(f"  Responsavel criado: {responsavel['nome']} (id {criado['id']})")
    for dependente in responsavel["dependentes"]:
        escolaId = escolas[dependente["escolaRef"]]
        corpoDep = {
            "nome": dependente["nome"],
            "cpf": None,
            "dataNascimento": dependente["dataNascimento"],
            "periodo": dependente["periodo"],
            "endereco": enderecoLegado(responsavel["endereco"]),
            "escola": {"id": escolaId},
        }
        resposta = api.exigir("POST", f"/Responsavel/CadastrarDependente/{criado['id']}", corpoDep, token=token)
        depCriado = next(d for d in resposta["dependentes"]
                         if d["nome"] == dependente["nome"] and d["dataNascimento"] == dependente["dataNascimento"])
        enderecoDep = dict(responsavel["endereco"], finalidade="AMBOS")
        api.exigir("POST", f"/Responsavel/Dependente/{depCriado['id']}/Enderecos", enderecoDep, token=token)
        log(f"    Dependente: {dependente['nome']} (id {depCriado['id']}) na escola {escolaId}")


def vincularMotoristaEscola(api, escolaToken, motoristaToken, motoristaId, log):
    convite = api.exigir("POST", "/Escola/Vinculos", {"motoristaId": motoristaId}, token=escolaToken)
    api.exigir("POST", f"/Motorista/Vinculos/{convite['id']}/Aceitar", {}, token=motoristaToken)
    log(f"    Vinculo escola-motorista aceito (vinculo {convite['id']})")


def cidadeDe(item):
    endereco = item.get("endereco") or item["enderecoOperacional"]
    return f"{endereco['cidade']}/{endereco['uf']}"


def gerarArquivoCredenciais(dados, caminho):
    linhas = [
        "CREDENCIAIS DE TESTE - Tio da Perua",
        "Gerado por seed_perfis.py. Sao contas de teste; nao use em producao.",
        "Este arquivo esta no .gitignore e nao deve ser commitado.",
        "",
        f"Senha de todas as contas: {dados['senhaPadrao']}",
        "",
    ]
    for titulo, chave in (("ESCOLAS", "escolas"), ("MOTORISTAS", "motoristas"), ("RESPONSAVEIS", "responsaveis")):
        linhas.append(f"{titulo}:")
        for item in dados[chave]:
            linhas.append(f"  {item['nome']} ({cidadeDe(item)})")
            linhas.append(f"    e-mail: {item['email']}")
            if chave == "responsaveis":
                nomes = ", ".join(d["nome"] for d in item["dependentes"])
                linhas.append(f"    dependentes: {nomes}")
        linhas.append("")
    caminho.write_text("\n".join(linhas), encoding="utf-8")


def principal():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--url", default=None, help=f"Base da API (padrao: TIO_API_URL ou {URL_PADRAO})")
    parser.add_argument("--dados", default=str(ARQUIVO_DADOS), help="Arquivo JSON com os perfis")
    parser.add_argument("--credenciais", default=str(ARQUIVO_CREDENCIAIS),
                        help="Arquivo de texto onde as credenciais serao gravadas")
    args = parser.parse_args()

    import os
    base = args.url or os.environ.get("TIO_API_URL", URL_PADRAO)
    dados = json.loads(Path(args.dados).read_text(encoding="utf-8"))
    senha = dados["senhaPadrao"]
    api = Api(base)

    def log(msg):
        print(msg, flush=True)

    try:
        api.chamar("POST", "/Autenticacao/Entrar", {"email": "ping@invalido.test", "senha": "x"})
    except OSError as erro:
        print(f"API indisponivel em {base}: {erro}. Suba o backend antes de rodar o seed.", file=sys.stderr)
        return 1

    log(f"Seed em {base}")
    escolasIds = {}
    try:
        log("Escolas:")
        for escola in dados["escolas"]:
            escola["email"] = f"{escola['ref']}@{dados['dominioEmail']}"

            escolaId, _ = garantirEscola(api, dados, escola, senha, log)
            escolasIds[escola["ref"]] = escolaId
            escola["id"] = escolaId

        log("Motoristas:")
        motoristasIds = {}
        for motorista in dados["motoristas"]:
            motoristaId, motoristaToken, criado = garantirMotorista(api, dados, motorista, senha, log)
            motoristasIds[motorista["ref"]] = motoristaId
            if criado:
                escola = next(e for e in dados["escolas"] if e["ref"] == motorista["escolaRef"])
                escolaToken, _ = api.entrar(f"{escola['ref']}@{dados['dominioEmail']}", senha)
                vincularMotoristaEscola(api, escolaToken, motoristaToken, motoristaId, log)

        log("Responsaveis e dependentes:")
        for responsavel in dados["responsaveis"]:
            garantirResponsavel(api, dados, responsavel, senha, escolasIds, log)
    except ErroSeed as erro:
        print(f"\nFalha: {erro}", file=sys.stderr)
        print("O seed nao e transacional. Se parou no meio, corrija a causa e rode de novo "
              "(contas ja criadas sao reaproveitadas) ou recrie o banco.", file=sys.stderr)
        return 2

    credenciais = Path(args.credenciais)
    gerarArquivoCredenciais(dados, credenciais)
    log(f"\nPerfis prontos. Credenciais gravadas em {credenciais}")
    return 0


if __name__ == "__main__":
    sys.exit(principal())
