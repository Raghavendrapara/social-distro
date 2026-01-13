# CI/CD Best Practices (2026)

Modern pipeline options for efficiency and productivity.

---

## Recommended Stack

| Category | 2026 Best Choice | Why |
|---|---|---|
| **CI/CD Platform** | GitHub Actions + Dagger | Native GitHub integration, portable pipelines |
| **Container Registry** | GitHub Container Registry (ghcr.io) | Free for public, integrated with Actions |
| **IaC** | Terraform + Pulumi | Declarative infra, language-native |
| **GitOps** | ArgoCD | Automated K8s sync from Git |
| **Secret Management** | Google Secret Manager | Integrated with GKE |

---

## GitHub Actions Pipeline

### Full CI/CD Workflow
```yaml
# .github/workflows/ci-cd.yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # ============ BUILD & TEST ============
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven
      
      - name: Run Tests
        run: mvn verify -B
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/

  # ============ SECURITY SCAN ============
  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Dependency Scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          severity: 'CRITICAL,HIGH'
      
      - name: SAST Scan
        uses: github/codeql-action/analyze@v3

  # ============ BUILD IMAGE ============
  build:
    needs: [test, security]
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.tags }}
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=
            type=ref,event=branch
            type=semver,pattern={{version}}
      
      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # ============ DEPLOY ============
  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging
    steps:
      - name: Deploy to Staging
        uses: google-github-actions/deploy-cloudrun@v2
        with:
          service: social-distro-staging
          image: ${{ needs.build.outputs.image-tag }}

  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - name: Authenticate to GKE
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}
      
      - name: Deploy to GKE
        uses: google-github-actions/get-gke-credentials@v2
        with:
          cluster_name: social-distro-cluster
          location: us-central1-a
      
      - name: Update Deployment
        run: |
          kubectl set image deployment/social-distro \
            app=${{ needs.build.outputs.image-tag }}
          kubectl rollout status deployment/social-distro
```

---

## Alternative: Dagger (Portable Pipelines)

Write CI in Go/Python/TypeScript, run anywhere:

```go
// ci/main.go
package main

import (
    "context"
    "dagger.io/dagger"
)

func main() {
    ctx := context.Background()
    client, _ := dagger.Connect(ctx)
    defer client.Close()

    // Build
    src := client.Host().Directory(".")
    app := client.Container().
        From("maven:3.9-eclipse-temurin-25").
        WithDirectory("/app", src).
        WithWorkdir("/app").
        WithExec([]string{"mvn", "package", "-DskipTests"})

    // Test
    app.WithExec([]string{"mvn", "verify"})

    // Push
    app.Publish(ctx, "ghcr.io/user/social-distro:latest")
}
```

**Benefits:**
- Same pipeline runs locally and in CI
- No YAML debugging
- Cacheable functions

---

## GitOps with ArgoCD

```yaml
# argocd/application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: social-distro
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/user/social-distro
    targetRevision: main
    path: k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

**Flow:**
1. Push to `main`
2. GitHub Actions builds image
3. Updates `k8s/deployment.yaml` with new tag
4. ArgoCD detects change, syncs to cluster

---

## Pipeline Stages Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                        CI/CD Pipeline                           │
├─────────┬──────────┬───────────┬────────────┬──────────────────┤
│  Lint   │   Test   │  Security │   Build    │     Deploy       │
├─────────┼──────────┼───────────┼────────────┼──────────────────┤
│ Checkstyle   │ Unit      │ Trivy     │ Docker     │ Staging → Prod │
│ SpotBugs     │ Integration│ CodeQL    │ Layer Cache│ K8s Rolling    │
│              │            │ SBOM      │            │ Canary/Blue-Green│
└─────────┴──────────┴───────────┴────────────┴──────────────────┘
```

---

## 2026 Trends

| Trend | Implementation |
|---|---|
| **AI-Assisted Reviews** | GitHub Copilot in PR reviews |
| **SBOM Generation** | Trivy generates CycloneDX/SPDX |
| **Supply Chain Security** | Sigstore/Cosign signing |
| **Ephemeral Environments** | Preview deployments per PR |
| **Trunk-Based Development** | Feature flags over branches |

---

## Quick Setup Checklist

- [ ] Enable GitHub Actions in repository
- [ ] Add `GCP_SA_KEY` to GitHub Secrets
- [ ] Create GKE cluster with Workload Identity
- [ ] Install ArgoCD in cluster
- [ ] Configure branch protection rules
- [ ] Set up Dependabot for dependency updates
- [ ] Enable CodeQL scanning
