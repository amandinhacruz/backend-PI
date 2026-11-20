"""Cria o histórico de conversas e reuniões da simulação do Vértice.

Uso (com o backend da simulação no ar e o banco já populado por popular.py):
    python simulacao/conversas.py --api http://localhost:8080

Para cada conexão firmada, troca mensagens pela API e cria um cenário de reunião:
  0. reunião que já aconteceu (aparece no Histórico) e mensagem de agradecimento;
  1. reunião confirmada nos próximos dias;
  2. convite aguardando a resposta da startup;
  3. convite recusado e um segundo convite confirmado;
  4. reunião confirmada e depois cancelada pelo investidor.

A API só aceita mensagens com a data de agora e reuniões no futuro. Por isso, no fim, o script
ajusta pelo psql as datas das mensagens (espalhadas pelas últimas semanas) e da reunião do
cenário 0 (passada). Só datas e o campo "lida" são alterados; os textos continuam
criptografados como a API gravou. Use --sem-retroagir para pular esse ajuste.
"""
import argparse
import datetime as dt
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import dados  # noqa: E402
from popular import Api, slug  # noqa: E402

DIALOGOS = [
    [("inv", "Olá, equipe da {st}! Vi o perfil de vocês no Vértice e gostei bastante da proposta."),
     ("st", "Olá, {inv}! Obrigado pelo interesse. Ficamos felizes com a conexão."),
     ("inv", "Vocês podem me contar um pouco sobre os clientes atuais e o modelo de receita?"),
     ("st", "Claro. Hoje trabalhamos com contratos mensais e crescemos em média 8% ao mês. "
            "Posso enviar os números detalhados."),
     ("inv", "Ótimo, envie sim. Também quero entender como pretendem usar o investimento."),
     ("st", "Principalmente em time comercial e na expansão para outras capitais do Nordeste.")],
    [("st", "Boa tarde, {inv}! Obrigado por solicitar a conexão. Como podemos ajudar?"),
     ("inv", "Boa tarde! Tenho experiência no setor e acho que posso contribuir além do capital."),
     ("st", "Que ótimo. Nosso maior desafio hoje é estruturar o processo de vendas."),
     ("inv", "Posso ajudar nesse ponto. Qual é o ciclo médio de venda de vocês?"),
     ("st", "Cerca de 45 dias para clientes maiores e uma semana para os pequenos.")],
    [("inv", "Oi, pessoal da {st}! Li o pitch de vocês. O problema que resolvem é bem real na região."),
     ("st", "Oi, {inv}! Exatamente, sentimos isso na pele antes de fundar a empresa."),
     ("inv", "Quantas pessoas estão no time hoje e qual a dedicação de cada uma?"),
     ("st", "Somos quatro sócios em tempo integral e dois desenvolvedores."),
     ("inv", "Perfeito. Vou preparar algumas perguntas sobre o mercado para conversarmos melhor.")],
]

ASSUNTOS = ["Apresentação do pitch e próximos passos", "Conversa sobre métricas e uso do investimento",
            "Alinhamento sobre mentoria e estratégia comercial", "Due diligence inicial",
            "Conhecer o time e o produto"]
LOCAIS = ["https://meet.example/vertice-{n}", "Porto Digital, Rua do Apolo 181, Recife",
          "https://meet.example/conversa-{n}", None, "Videochamada (o link será enviado no dia)"]


def sql_ts(valor):
    return valor.strftime("%Y-%m-%d %H:%M:%S")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api", default="http://localhost:8080")
    parser.add_argument("--psql", default=r"C:\Program Files\PostgreSQL\18\bin\psql.exe")
    parser.add_argument("--banco", default="vertice_simulacao")
    parser.add_argument("--usuario-banco", default="postgres")
    parser.add_argument("--sem-retroagir", action="store_true", help="não ajusta datas pelo psql")
    args = parser.parse_args()
    api = Api(args.api)

    investidores = [(r[0], f"{slug(r[0])}@{dados.DOMINIO}") for r in dados.INVESTIDORES]
    startups = {r[0]: f"{slug(r[0])}@{dados.DOMINIO}" for r in dados.STARTUPS}
    tokens = {}

    def token(email):
        if email not in tokens:
            tokens[email] = api.login(email, dados.SENHA)
        return tokens[email]

    conversas = []
    for nome, email in investidores:
        for conversa in api.chamar("GET", "/conversas", token=token(email)):
            conversas.append((nome, email, conversa))
    if not conversas:
        sys.exit("Nenhuma conexão firmada. Rode popular.py antes.")
    if any(c["lastMessage"] for _, _, c in conversas):
        sys.exit("As conversas já têm mensagens. Recrie o banco da simulação para gerar o histórico de novo.")

    hoje = dt.date.today()
    agora = dt.datetime.now().replace(microsecond=0)
    atualizacoes = []
    resumo = {"mensagens": 0, "reunioes": 0}

    for i, (nome_inv, email_inv, conversa) in enumerate(conversas):
        email_st = startups[conversa["name"]]
        tk = {"inv": token(email_inv), "st": token(email_st)}
        primeiro_nome = nome_inv.split()[0]
        cid = conversa["id"]

        for autor, texto in DIALOGOS[i % len(DIALOGOS)]:
            api.chamar("POST", f"/conversas/{cid}/mensagens",
                       {"text": texto.format(st=conversa["name"], inv=primeiro_nome)}, token=tk[autor])
            resumo["mensagens"] += 1

        def convidar(dias, hora, duracao=60):
            local = LOCAIS[i % len(LOCAIS)]
            corpo = {"date": (hoje + dt.timedelta(days=dias)).isoformat(), "time": hora,
                     "durationMinutes": duracao, "subject": ASSUNTOS[i % len(ASSUNTOS)]}
            if local:
                corpo["location"] = local.format(n=cid)
            resumo["reunioes"] += 1
            return api.chamar("POST", f"/conversas/{cid}/convites", corpo, token=tk["inv"])["meeting"]["id"]

        def responder(reuniao, decisao):
            api.chamar("POST", f"/reunioes/{reuniao}/resposta", {"decision": decisao}, token=tk["st"])

        cenario = i % 5
        passada = None
        if cenario == 0:
            passada = convidar(2 + i, "10:00")
            responder(passada, "accepted")
            api.chamar("POST", f"/conversas/{cid}/mensagens",
                       {"text": "Obrigado pela reunião! Vou analisar os números e retorno até sexta."},
                       token=tk["inv"])
        elif cenario == 1:
            responder(convidar(2 + i, "14:00", 45), "accepted")
        elif cenario == 2:
            convidar(3 + i, "09:30")
        elif cenario == 3:
            responder(convidar(2 + i, "11:00"), "rejected")
            api.chamar("POST", f"/conversas/{cid}/mensagens",
                       {"text": "Nesse dia não conseguimos. Pode ser na semana seguinte?"}, token=tk["st"])
            responder(convidar(9 + i, "15:30", 30), "accepted")
        else:
            reuniao = convidar(4 + i, "16:00")
            responder(reuniao, "accepted")
            api.chamar("POST", f"/reunioes/{reuniao}/cancelar", token=tk["inv"])
            api.chamar("POST", f"/conversas/{cid}/mensagens",
                       {"text": "Precisei cancelar por um imprevisto. Envio um novo convite em breve."},
                       token=tk["inv"])

        # Plano de datas: a conversa começa entre 9 e 23 dias atrás, mensagens a cada ~5 horas.
        historico = api.chamar("GET", f"/conversas/{cid}/mensagens", token=tk["inv"])
        inicio = dt.datetime.combine(hoje - dt.timedelta(days=9 + i), dt.time(9, 0))
        momento = inicio
        dia_reuniao = None
        if passada:
            dia_reuniao = dt.datetime.combine(inicio.date() + dt.timedelta(days=3), dt.time(10, 0))
            atualizacoes.append(f"update agendamentos set data = '{dia_reuniao.date()}' where id = {passada};")
        for posicao, mensagem in enumerate(historico):
            if passada and posicao == len(historico) - 1:
                momento = dia_reuniao + dt.timedelta(hours=6)  # agradecimento depois da reunião
            else:
                momento += dt.timedelta(hours=4, minutes=17 * (posicao % 4))
            momento = min(momento, agora - dt.timedelta(minutes=5 * (len(historico) - posicao)))
            # Metade das conversas fica com a última mensagem não lida pelo destinatário.
            nao_lida = posicao == len(historico) - 1 and i % 2 == 1
            lida = "null" if nao_lida else f"'{sql_ts(momento + dt.timedelta(hours=1))}'"
            atualizacoes.append(f"update mensagens set data_envio = '{sql_ts(momento)}', lida_em = {lida} "
                                f"where id = {mensagem['id']};")

    print(f"{len(conversas)} conversas, {resumo['mensagens']} mensagens de texto, {resumo['reunioes']} convites")

    if args.sem_retroagir:
        print("Datas não ajustadas (--sem-retroagir): todo o histórico ficou com a data de hoje.")
        return
    ambiente = dict(os.environ)
    ambiente.setdefault("PGPASSWORD", "root")
    resultado = subprocess.run(
        [args.psql, "-h", "localhost", "-U", args.usuario_banco, "-d", args.banco, "-v", "ON_ERROR_STOP=1",
         "-q", "-c", "begin; " + " ".join(atualizacoes) + " commit;"],
        env=ambiente, capture_output=True, text=True)
    if resultado.returncode != 0:
        sys.exit("Falha ao ajustar as datas: " + resultado.stderr.strip())
    print(f"Datas ajustadas: {len(atualizacoes)} registros espalhados pelas últimas semanas.")


if __name__ == "__main__":
    main()
