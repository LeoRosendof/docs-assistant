from fastapi import FastAPI

app = FastAPI(title="docs-assistant ai-service")


@app.get("/health")
def health():
    return {"status": "ok"}
