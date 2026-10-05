# Decisões de projeto

Registro curto de cada decisão e do motivo. Atualizado ao longo dos commits.

## 001 - Monorepo
Os três serviços ficam no mesmo repositório. O objetivo é mostrar a integração entre eles,
e um único `docker-compose` sobe tudo.

## 002 - LLM local com Ollama
Roda na GPU local (RTX 3060, 12 GB), sem custo e sem chave de API. O provedor será
configurável por variável de ambiente para permitir trocar por APIs externas.

## 003 - RAG primeiro
A parte de IA é a mais difícil de explicar e a mais valiosa, então o `ai-service` vem
antes do backend Java.

## 004 - Modelo de embedding: bge-m3 em vez de nomic-embed-text
A primeira escolha foi o `nomic-embed-text` (leve, usado nos exemplos do Ollama). Ao testar
com um documento real em português (um modelo de revisão sistemática da literatura, 12
páginas), a recuperação foi fraca. Medi com 9 perguntas cuja resposta eu sabia onde estava,
contando em que posição aparecia o trecho que contém a resposta:

| Modelo | Trechos | Trecho certo em 1º | No top 3 |
|---|---|---|---|
| nomic-embed-text | 250 / 500 / 1000 / 1500 caracteres | 2/9 em todos | 3/9, 4/9, 5/9, 5/9 |
| bge-m3 | 500 | 4/9 | 6/9 |
| bge-m3 | 1000 | 5/9 | 6/9 |
| bge-m3 | 1500 | 7/9 | 8/9 |

Além disso, com o nomic uma pergunta sem relação ("Qual a capital da França?") pontuava 0,63
de similaridade, quase igual às perguntas relevantes (0,64 a 0,78), o que impediria usar um
limite de score para responder "não sei". Com o bge-m3 as perguntas sem relação ficaram em
0,28 a 0,37 e as relevantes em 0,45 a 0,72.

Mudar só o tamanho do trecho não resolveu no nomic; trocar o modelo resolveu a maior parte.
Hipótese (não verificada): o nomic-embed-text é treinado sobretudo em inglês.

**Decisão:** usar o `bge-m3` (multilíngue). Os prefixos de embedding e o tamanho dos trechos
passaram a ser configuráveis, porque o nomic exige prefixos e o bge-m3 não. O padrão de
trecho subiu de 500 para 1000 caracteres.

**Limitações da medição:** são só 9 perguntas e um único documento, então os números
indicam uma tendência, não uma garantia. Com trechos maiores há menos candidatos, o que
facilita acertar o top 3 por acaso; por isso escolhi 1000 e não 1500, que parecia melhor mas
com mais risco de ajuste excessivo a este documento. Duas perguntas (strings de busca e
PI/ECO) continuaram ruins mesmo com o bge-m3, porque a resposta fica no meio de trechos com
conteúdo misturado. Texto riscado no PDF original também é extraído como se fosse válido.
