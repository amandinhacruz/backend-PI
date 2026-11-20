"""Popula um banco de simulação do Vértice pela API do backend.

Uso (com o backend da simulação no ar):
    python simulacao/popular.py --api http://localhost:8080

Tudo passa pelos mesmos endpoints que o front usa, então as validações, a
criptografia e o consentimento valem para os dados da simulação. O script só
aceita rodar num banco sem usuários além do admin (use um banco novo a cada rodada).

Passos:
 1. cadastra 65 startups e 20 investidores/mentores;
 2. aprova as startups e os investidores, deixa 3 startups pendentes e reprova 2;
 3. mostra quantas startups cada investidor vê no matching;
 4. cria histórico para metade dos investidores: pedidos de conexão, startups
    passadas, conexões firmadas e negadas, e avaliações (RF10).
"""
import argparse
import json
import random
import re
import sys
import unicodedata
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dados  # noqa: E402

ADMIN_EMAIL = "admin@projetopi.com"
ADMIN_SENHA = "admin123"


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def chamar(self, metodo, caminho, corpo=None, token=None, esperado=(200, 201)):
        dados_req = None if corpo is None else json.dumps(corpo, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(self.base + caminho, data=dados_req, method=metodo)
        req.add_header("Content-Type", "application/json; charset=utf-8")
        if token:
            req.add_header("Authorization", "Bearer " + token)
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                texto = resp.read().decode("utf-8")
                status = resp.status
        except urllib.error.HTTPError as erro:
            texto = erro.read().decode("utf-8", "replace")
            status = erro.code
        if status not in esperado:
            raise RuntimeError(f"{metodo} {caminho} -> HTTP {status}: {texto[:300]}")
        return json.loads(texto) if texto else None

    def login(self, email, senha):
        return self.chamar("POST", "/auth/login", {"email": email, "password": senha})["token"]


def slug(texto):
    sem_acento = unicodedata.normalize("NFD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", ".", sem_acento.lower()).strip(".")


def cpf(rnd):
    n = [rnd.randint(0, 9) for _ in range(9)]
    for tamanho in (9, 10):
        soma = sum(d * (tamanho + 1 - i) for i, d in enumerate(n[:tamanho]))
        resto = (soma * 10) % 11
        n.append(0 if resto == 10 else resto)
    return "".join(map(str, n))


def cnpj(rnd):
    n = [rnd.randint(0, 9) for _ in range(8)] + [0, 0, 0, 1]
    for pesos in ([5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2], [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]):
        resto = sum(d * p for d, p in zip(n, pesos)) % 11
        n.append(0 if resto < 2 else 11 - resto)
    return "".join(map(str, n))


PRIMEIROS = ["Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabriela", "Henrique", "Isadora",
             "Lucas", "Mariana", "Nelson", "Olívia", "Paulo", "Renata", "Sérgio", "Tânia", "Vitor"]
SOBRENOMES = ["Albuquerque", "Barros", "Cordeiro", "Dantas", "Farias", "Guedes", "Holanda", "Lima",
              "Macedo", "Nóbrega", "Oliveira", "Pessoa", "Queiroz", "Rêgo", "Souto", "Tavares"]


def cadastrar_startup(api, rnd, registro):
    nome, segmento, estagio, cidade, uf, tmin, tmax, resumo, problema, diferenciais = registro
    email = f"{slug(nome)}@{dados.DOMINIO}"
    corpo = {
        "email": email, "password": dados.SENHA, "name": nome,
        "cnpj": cnpj(rnd), "website": f"https://{slug(nome).replace('.', '')}.example",
        "city": cidade, "state": uf, "foundedYear": str(rnd.randint(2019, 2026)),
        "profileOwner": f"{rnd.choice(PRIMEIROS)} {rnd.choice(SOBRENOMES)}",
        "segment": segmento, "stage": estagio,
        "summary": resumo, "problem": problema, "competitiveAdvantages": diferenciais,
        "terms": True, "privacy": True, "dataProcessing": True,
    }
    if tmin is not None:
        corpo["minimumTicket"] = tmin
    if tmax is not None:
        corpo["maximumTicket"] = tmax
    criado = api.chamar("POST", "/auth/cadastro/startup", corpo)
    return {"id": criado["id"], "email": email, "nome": nome}


def cadastrar_investidor(api, rnd, registro):
    nome, tipo, cidade, uf, segmentos, estagios, tmin, tmax, sobre, carreira, busca = registro
    email = f"{slug(nome)}@{dados.DOMINIO}"
    corpo = {
        "email": email, "password": dados.SENHA, "name": nome, "tipo": tipo, "cpf": cpf(rnd),
        "linkedin": f"https://linkedin.example/in/{slug(nome).replace('.', '-')}",
        "city": cidade, "state": uf, "segments": segmentos,
        "aboutInvestor": sobre, "career": carreira, "lookingFor": busca,
        "terms": True, "privacy": True, "dataProcessing": True,
    }
    if estagios:
        corpo["stages"] = estagios
    if tmin is not None:
        corpo["minimumTicket"] = tmin
    if tmax is not None:
        corpo["maximumTicket"] = tmax
    criado = api.chamar("POST", "/auth/cadastro/investidor", corpo)
    return {"id": criado["id"], "email": email, "nome": nome}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api", default="http://localhost:8080", help="URL do backend da simulação")
    parser.add_argument("--semente", type=int, default=2026, help="semente dos dados aleatórios (CPF, CNPJ)")
    args = parser.parse_args()
    rnd = random.Random(args.semente)
    api = Api(args.api)

    admin = api.login(ADMIN_EMAIL, ADMIN_SENHA)
    existentes = api.chamar("GET", "/admin/usuarios", token=admin)
    if existentes:
        sys.exit(f"O banco já tem {len(existentes)} usuário(s) além do admin. "
                 "Use um banco novo (veja docs/SIMULACAO.md) para não misturar dados.")

    # 1-2. Cadastros e moderação --------------------------------------------
    print("Cadastrando startups...")
    startups = [cadastrar_startup(api, rnd, r) for r in dados.STARTUPS]
    pendentes = [cadastrar_startup(api, rnd, r) for r in dados.PENDENTES]
    reprovadas = [cadastrar_startup(api, rnd, r) for r in dados.REPROVADAS]
    print("Cadastrando investidores e mentores...")
    investidores = [cadastrar_investidor(api, rnd, r) for r in dados.INVESTIDORES]

    for usuario in startups + investidores:
        api.chamar("PUT", f"/admin/usuarios/{usuario['id']}/aprovar", token=admin)
    for usuario in reprovadas:
        api.chamar("PUT", f"/admin/usuarios/{usuario['id']}/rejeitar", token=admin)
    print(f"  {len(startups)} startups aprovadas, {len(pendentes)} pendentes, {len(reprovadas)} reprovadas")
    print(f"  {len(investidores)} investidores/mentores aprovados")

    # 3. Cobertura do matching ----------------------------------------------
    print("\nStartups compatíveis por investidor (antes do histórico):")
    tokens = {}
    filas = {}
    proibidas = {s["nome"] for s in pendentes + reprovadas}
    for inv in investidores:
        tokens[inv["email"]] = api.login(inv["email"], dados.SENHA)
        fila = api.chamar("GET", "/matchmaking/investidor", token=tokens[inv["email"]])
        filas[inv["email"]] = fila["items"]
        nomes = [item["name"] for item in fila["items"]]
        vazou = proibidas.intersection(nomes)
        if vazou:
            sys.exit(f"ERRO: startup pendente/reprovada apareceu para {inv['nome']}: {vazou}")
        print(f"  {inv['nome']:<22} {len(nomes):>2}  {', '.join(nomes[:4])}{' ...' if len(nomes) > 4 else ''}")

    # 4. Histórico para metade dos investidores ------------------------------
    print("\nCriando histórico de decisões...")
    solicitadas = passadas = 0
    for inv in investidores[:10]:
        fila = filas[inv["email"]]
        if not fila:
            continue
        for item in fila[:2]:
            api.chamar("POST", f"/matchmaking/investidor/startups/{item['id']}/decisao",
                       {"decision": "accepted"}, token=tokens[inv["email"]])
            solicitadas += 1
        if len(fila) >= 4:
            api.chamar("POST", f"/matchmaking/investidor/startups/{fila[-1]['id']}/decisao",
                       {"decision": "rejected"}, token=tokens[inv["email"]])
            passadas += 1

    firmadas, negadas, contador = [], 0, 0
    for st in startups:
        token_st = api.login(st["email"], dados.SENHA)
        for pedido in api.chamar("GET", "/matchmaking/startup/pedidos", token=token_st):
            contador += 1
            decisao = "rejected" if contador % 4 == 0 else "accepted"
            api.chamar("POST", f"/matchmaking/startup/pedidos/{pedido['id']}/decisao",
                       {"decision": decisao}, token=token_st)
            if decisao == "accepted":
                firmadas.append((st, token_st, pedido))
            else:
                negadas += 1

    avaliacoes = 0
    for indice, (st, token_st, pedido) in enumerate(firmadas):
        investidor = next(i for i in investidores if i["id"] == pedido["userId"])
        # Uma conexão recebe nota baixa para exercitar a reputação negativa.
        nota_investidor = 2 if indice == 0 else (5 if pedido["affinity"] >= 80 else 4)
        comentario = ("Pitch confuso e pouca clareza sobre o uso do dinheiro." if nota_investidor == 2
                      else "Conversa objetiva, time preparado.")
        api.chamar("PUT", f"/conexoes/{pedido['id']}/avaliacao",
                   {"score": nota_investidor, "comment": comentario}, token=tokens[investidor["email"]])
        api.chamar("PUT", f"/conexoes/{pedido['id']}/avaliacao",
                   {"score": 4 + indice % 2, "comment": "Investidor atencioso e com boas perguntas."},
                   token=token_st)
        avaliacoes += 2

    print(f"  {solicitadas} pedidos de conexão, {passadas} startups passadas")
    print(f"  {len(firmadas)} conexões firmadas, {negadas} negadas, {avaliacoes} avaliações")
    print(f"\nPronto. Senha de todas as contas: {dados.SENHA}")
    print(f"Exemplos: {investidores[0]['email']} (investidor), {startups[0]['email']} (startup)")


if __name__ == "__main__":
    main()
