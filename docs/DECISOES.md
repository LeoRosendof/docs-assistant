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

## 005 - Backend: Spring Boot 4.1 com Gradle, escrito à mão
O backend usa Java 21, Spring Boot 4.1.1 e Gradle (`gradlew`), como combinado. O gerador
oficial (start.spring.io) devolveu erro 500 para qualquer projeto Gradle no dia, e só o Maven
funcionava. Em vez de trocar de ferramenta, usei o projeto Maven gerado apenas para copiar a
estrutura e os nomes dos starters do Boot 4 (que mudaram, por exemplo
`spring-boot-starter-webmvc`), escrevi o `build.gradle` à mão e gerei o wrapper com o Gradle
9.8.0, baixado com o checksum conferido. Banco: H2 por enquanto, para o backend rodar sem
instalar nada; a troca por um banco de verdade fica para o `docker-compose`.

## 006 - Validação em um segundo documento
As decisões anteriores (modelo de embedding, trechos de 1000 caracteres, score mínimo de 0,40)
foram calibradas em um único PDF. Para saber se elas se sustentam, testei em outro documento,
de assunto e formato bem diferentes: 59 páginas de artigos de matemática, com fórmulas e
figuras (~73 mil caracteres, 91 trechos). Escrevi 12 perguntas com resposta conhecida e, como
esse documento passou a ser o teste independente, **não ajustei nenhum parâmetro depois de ver
os resultados**.

| Medida | Resultado |
|---|---|
| Trecho certo em 1º lugar | 8 de 12 |
| Trecho certo entre os 4 usados pelo `/perguntar` | 11 de 12 |
| Respostas corretas | 10 de 12 |
| Recusas ("Não encontrei") | 2 de 12 |
| Respostas erradas | 0 de 12 |
| Perguntas fora do documento recusadas | 2 de 2 |

**As duas recusas:** uma pergunta sobre a Torre de Hanói (o trecho certo ficou em 6º lugar,
fora dos 4 que entram no prompt) e uma sobre o autor de um artigo (o trecho estava em 2º, mas o
modelo não achou o nome, que fica no cabeçalho).

**O que isto corrige:** o score mínimo de 0,40 **não separou** relevante de irrelevante neste
documento. Uma pergunta sem relação ("Como fazer um bolo de chocolate?") pontuou 0,455 e passou
do limite, enquanto as relevantes ficaram entre 0,50 e 0,73. No primeiro documento a separação
parecia clara (0,28 a 0,37 contra 0,45 ou mais), mas era um efeito daquela amostra. Quem impediu
a invenção aqui foi a regra de recusa do prompt, e o modelo recusou. Conclusão: o limite serve
para economizar uma chamada ao modelo quando a pergunta é obviamente de outro assunto, mas **não
deve ser tratado como garantia**; a proteção real é o prompt, que depende do comportamento do
modelo.

**Limitações:** são 12 perguntas que eu mesmo escrevi, em um documento só, e as respostas
corretas foram conferidas por mim. Para o texto extraído do PDF, conferi que todas as respostas
esperadas existem nele, então as falhas são da busca ou do modelo, não da extração. O PDF não
está no repositório (é material de terceiros, com direitos autorais).
