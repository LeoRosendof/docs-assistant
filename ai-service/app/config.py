import os

# Valores padrão servem para desenvolvimento local; em outros ambientes
# (Docker, por exemplo) basta definir as variáveis, sem mexer no código.
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/chat")
OLLAMA_MODELO = os.getenv("OLLAMA_MODELO", "llama3.1:8b")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))
