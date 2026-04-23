# Infrastructure & Deployment Guide
This document covers the technical setup for the Beto-Bot project.
It provides two paths for deployment: 

- Docker Compose for simple/local environments
- Kubernetes (K3s) for professional-grade orchestration.

For detailed examples regarding development//secret setup you can check the [dev.md](./dev.md)

--- 
## 1. Option A: Docker Compose Setup
This is the fastest way to get the system running on a single machine.

### The docker-compose.yml
This setup runs the GitHub MCP server in http mode and connects beto-bot via the internal Docker network.

```YAML
services:
  github-mcp:
    image: ghcr.io/github/github-mcp-server:latest
    container_name: github-mcp
    ports:
      - "9090:8082"
    environment:
      - GITHUB_PERSONAL_ACCESS_TOKEN=${GITHUB_PERSONAL_ACCESS_TOKEN}
      - GITHUB_TOOLSETS=repos,issues,pull_requests,contents
    command: http

  beto-bot:
    image: ghcr.io/silversurferstate/beto-bot:latest
    depends_on:
      - github-mcp
    environment:
      - GOOGLE_API_KEY=${GOOGLE_API_KEY}
      - GITHUB_PROJECT_ID=${GITHUB_PROJECT_ID}
      - GITHUB_PERSONAL_ACCESS_TOKEN=${GITHUB_PERSONAL_ACCESS_TOKEN}
      - GITHUB_MCP_SERVER_ADDRESS=${GITHUB_MCP_SERVER_ADDRESS}
      - AGENT_MODEL_ANALYST=${AGENT_MODEL_ANALYST}
      - AGENT_MODEL_CODER=${AGENT_MODEL_CODER}
```
### Using the docker-compose file
Fill in your .env file.

Set GITHUB_MCP_SERVER_ADDRESS to http://github-mcp:8082.

Run docker-compose up -d. Since the image is public, no login is required.

--- 
## 2. Option B: Kubernetes Setup (K3s)

### Cluster Installation & Access
```bash
Install K3s: curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644.
```

### **🪟 Windows Users**
**Copy Config:** Copy /etc/rancher/k3s/k3s.yaml from the Linux server to your Windows machine at $HOME\.kube\config.

**Update IP:** Open the file and change server: https://127.0.0.1:6443 to server: https://<YOUR_SERVER_IP>:6443.

---

### **🍎 macOS Users**
**Create Directory:** Run mkdir -p ~/.kube in your terminal.

**Download Config:** Use SCP to pull the config directly:

```bash
scp user@your-server-ip:/etc/rancher/k3s/k3s.yaml ~/.kube/config
```
**Update IP:** Use sed to quickly swap the loopback address for your server IP:

```bash
sed -i '' 's/127.0.0.1/<YOUR_SERVER_IP>/g' ~/.kube/config
```
---
**Verify:** Run kubectl get nodes.


### **🔒 Secret Management (Doppler)**
We use the Doppler Kubernetes Operator to sync secrets directly into the cluster.

You can create a free account on [doppler](https://www.doppler.com/)
You can find more info on how to create a service-token there as well.

### Authentication
Store your Doppler Service Token in a secret named doppler-token-auth under the key serviceToken.

```bash
kubectl create secret generic doppler-token-auth \
--from-literal=serviceToken=dp.st.your_token_here
```

### Sync Resource (doppler-sync.yaml)
```YAML
apiVersion: secrets.doppler.com/v1alpha1
kind: DopplerSecret
metadata:
  name: beto-bot-secrets
spec:
  tokenSecret: { name: doppler-token-auth }
  managedSecret: { name: app-secrets }
  project: name_of_your_project_in_doppler
  config: name_of_your_config_in_doppler
```

## 3. Container Registry (GHCR)
The beto-bot image is hosted on GitHub Container Registry.

### Public Access
Because the image is Public, the cluster does not require an imagePullSecret to download it.

Deployment Configuration
The deployment manifest stays clean:

```YAML
spec:
  template:
    spec:
      containers:
        - name: beto-bot
          image: ghcr.io/your-username/beto-bot:latest
          envFrom:
            - secretRef:
                name: app-secrets
```

## 4. Internal Networking
Docker Compose Address: http://github-mcp:8082.

Kubernetes Address: http://github-mcp-service:9090.

> **Note: Ensure GITHUB_MCP_SERVER_ADDRESS in your environment does not contain quotation marks, or the Java URI.create() will fail.**