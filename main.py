# Author: Danyal | GridLink DePIN - Monolith 2026
from fastapi import FastAPI
from pydantic import BaseModel
import uuid
import random

app = FastAPI(title="GridLink Matchmaker Oracle")

class RegisterRequest(BaseModel):
    ipAddress: str
    walletAddress: str

@app.post("/node/register")
async def register_node(request: RegisterRequest):
    print(f"New Node Connected: {request.ipAddress} | Wallet: {request.walletAddress}")
    return {
        "nodeId": f"node_{uuid.uuid4().hex[:8]}", 
        "status": "active"
    }

@app.get("/node/status")
async def get_node_status():
    return {
        "isActive": True,
        "connectedPeers": random.randint(2, 6),
        "latencyMs": random.randint(15, 60)
    }