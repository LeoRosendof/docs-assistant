import os

# Valores padrão servem para desenvolvimento local; em outros ambientes
# (Docker, por exemplo) basta definir as variáveis, sem mexer no código.
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")
OLLAMA_MODELO = os.getenv("OLLAMA_MODELO", "llama3.1:8b")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))
# Limite de tokens por resposta. Sem ele, o modelo pode entrar em loop de repetição
# e só parar ao esgotar o contexto (observado com a pergunta "O que é RAG?").
OLLAMA_NUM_PREDICT = int(os.getenv("OLLAMA_NUM_PREDICT", "512"))
