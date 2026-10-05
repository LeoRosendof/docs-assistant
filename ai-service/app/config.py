import os

# Valores padrão servem para desenvolvimento local; em outros ambientes
# (Docker, por exemplo) basta definir as variáveis, sem mexer no código.
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")
OLLAMA_MODELO = os.getenv("OLLAMA_MODELO", "llama3.1:8b")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))
OLLAMA_EMBED_URL = os.getenv("OLLAMA_EMBED_URL", "http://localhost:11434/api/embed")
# bge-m3 em vez de nomic-embed-text: recuperou bem melhor em português (docs/DECISOES.md, 004).
OLLAMA_EMBED_MODELO = os.getenv("OLLAMA_EMBED_MODELO", "bge-m3")
# Alguns modelos de embedding exigem prefixos que distinguem trecho de pergunta
# (o nomic usa "search_document: " e "search_query: "); o bge-m3 não usa nenhum.
EMBED_PREFIXO_DOCUMENTO = os.getenv("EMBED_PREFIXO_DOCUMENTO", "")
EMBED_PREFIXO_PERGUNTA = os.getenv("EMBED_PREFIXO_PERGUNTA", "")
CHUNK_TAMANHO = int(os.getenv("CHUNK_TAMANHO", "1000"))
CHUNK_SOBREPOSICAO = int(os.getenv("CHUNK_SOBREPOSICAO", "200"))
# Limite de tokens por resposta. Sem ele, o modelo pode entrar em loop de repetição
# e só parar ao esgotar o contexto (observado com a pergunta "O que é RAG?").
OLLAMA_NUM_PREDICT = int(os.getenv("OLLAMA_NUM_PREDICT", "512"))
