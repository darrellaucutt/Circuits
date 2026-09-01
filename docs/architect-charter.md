# Mobile Architect Charter

**Purpose:** Define how we scale teams and markets without compounding technical debt.  
**Scope:** Architecture, delivery, quality, and operations.  
**Owner:** Mobile Architect (with Platform Guild + Eng Leads)  
**Review cadence:** Quarterly, or when team count / market count changes materially.

---

## 1. Principles

| # | Principle | In practice |
|---|-----------|-------------|
| 1 | **Boundaries beat headcount** | If two teams need daily coordination to ship, fix the architecture. |
| 2 | **Default path = right path** | Paved paths and automation over tribal knowledge and heroics. |
| 3 | **Opinionated at seams, flexible inside** | Standardize integration; allow variation within feature modules. |
| 4 | **Debt is a portfolio** | Every major deviation is accepted, deferred, or remediated — never invisible. |
| 5 | **Measure outcomes** | Build time, crash-free rate, lead time — not module count. |

---

## 2. Guardrails (Nonnegotiables)

Violations block merge or release unless an explicit **exception ADR** is approved.

### Architectural

| Rule | Enforcement |
|------|-------------|
| Dependencies flow inward: `feature → domain → data → platform` | Gradle dependency lint / CI fitness function |
| Feature modules cannot depend on other feature modules | CI module graph check |
| Cross-feature communication via contracts only (events, deep links, shared domain APIs) | Code review + lint |
| Platform APIs (network, auth, analytics) accessed through platform abstractions only | Static analysis |
| Sync / wear / extension contracts are versioned and backward-compatible for N releases | Schema review in RFC |

### Delivery

| Rule | Enforcement |
|------|-------------|
| Trunk-based development; branches live ≤ 3 days | Process + CI |
| Incomplete work behind feature flags | PR checklist |
| Release train: `[weekly / biweekly]`; hotfixes follow same pipeline | Release tooling |
| Definition of Done includes observability (logging, analytics, crash attribution) | PR template |

### Quality

| Rule | Enforcement |
|------|-------------|
| Domain logic has unit tests | CI coverage gate on `core` / `domain` |
| Critical user journeys have integration or UI tests | CI + release gate |
| Lint, static analysis, and dependency audit pass | CI |
| Performance budgets: cold start `[Xs]`, ANR rate `[<Y%]`, main-thread blocking | Macrobenchmark / Play Vitals |

### Operational

| Rule | Enforcement |
|------|-------------|
| Crash-free sessions ≥ `[99.x%]` before full rollout | Staged rollout gate |
| Feature teams own dashboards for their area (crashes, latency, errors) | On-call + SLO mapping |
| Kill switch or rollback path for every user-facing release | Feature flags / remote config |
| Security: no secrets in repo; dependency CVE SLA `[N days]` | Secret scan + Dependabot |

---

## 3. Paved Paths (Platform-Provided)

The platform team maintains these as **documented, templated, and supported** defaults.

| Capability | Paved path | Owner |
|------------|------------|-------|
| New feature | `./scripts/new-feature-module` → nav hook, DI, analytics scaffold | Platform |
| Networking | Single HTTP client, auth refresh, retries, error mapping | Platform |
| Navigation & deep links | Central route registry; typed args | Platform |
| UI | Design system components; theming; a11y baseline | Platform / Design |
| State | ViewModel + domain use-case pattern; no business logic in Composables/Views | Architecture guild |
| Persistence | Repository pattern; Room/GRDB behind interfaces | Platform |
| Analytics & experiments | Wrapper with standard event schema | Platform |
| CI/CD | PR → build → test → artifact → staged rollout | Platform |
| Wear / extensions | Shared domain + versioned sync codec; thin platform UI | Platform + feature |
| Observability | Structured logging, crash breadcrumbs, performance traces | Platform |

**Rule:** Teams may opt out only via **exception ADR** with named owner and sunset date.

---

## 4. Flexibility Zones (Team Retains Choice)

Within guardrails, teams decide without architect approval:

- Feature-internal module structure (screens, use-cases, local helpers)
- UI layout, motion, and feature-specific UX (using design system tokens)
- Local refactors that do not change public module APIs
- Experimentation inside a feature flag
- Test strategy details beyond minimum gates
- Non-critical performance tuning within budgets

---

## 5. Ownership Model

| Layer | Owns | Examples |
|-------|------|----------|
| **Platform team** | Paved paths, CI, design system, shared SDKs | Auth client, release pipeline |
| **Stream-aligned teams** | Vertical user journeys end-to-end | Workout timer, circuit builder |
| **Architecture guild** | RFCs, ADRs, dependency rules, reference implementations | Module map, migration playbooks |
| **Enabling team** (as needed) | Time-boxed spikes, migrations, coaching | Navigation rewrite, KMP extraction |

**You build it, you run it:** Each stream team owns crash rate, lead time, and tech-debt OKRs for their domain.

---

## 6. Technical Debt Triage

Every significant deviation from paved paths or guardrails gets a **Debt Card** (RFC appendix or ticket label).

### Classification

| Class | When to use | Required fields |
|-------|-------------|-----------------|
| **Accept** | Cost of fix > value; risk is low and bounded | Rationale, owner, review date |
| **Defer** | Valid fix, wrong timing | Trigger condition + target quarter |
| **Remediate** | Safety, velocity, or compliance risk | Epic link, priority, deadline |

### Decision rubric

| Signal | Default action |
|--------|----------------|
| Blocks release / security / compliance | **Remediate** immediately |
| Increases coupling at module boundaries | **Remediate** within 1–2 sprints |
| Duplicates paved path inside one feature | **Defer** with trigger (e.g. "when touching area again") |
| Legacy code, stable, low churn | **Accept** with annual review |
| Slows build or bloats binary beyond budget | **Remediate** when over threshold |

### Portfolio budget

- **~15–20%** of each team's capacity reserved for platform health and debt paydown
- Major migrations (e.g. navigation rewrite, monolith split) get explicit roadmap slots — not "when we have time"

---

## 7. Decision & Exception Process

```
Idea → RFC (if cross-cutting or new pattern)
     → ADR (decision recorded, alternatives rejected)
     → Implement on paved path (default)
     → Exception ADR (if opting out) → Debt Card (accept / defer / remediate)
     → Sunset review on schedule
```

| Change type | Required |
|-------------|----------|
| New module or public API | RFC + architecture review |
| New dependency (major) | RFC or platform approval |
| Guardrail change | ADR + guild sign-off |
| Paved-path opt-out | Exception ADR + Debt Card |

---

## 8. Success Metrics

Review quarterly. Targets are illustrative — set baselines first, then improve.

| Metric | Baseline | Target | Owner |
|--------|----------|--------|-------|
| Clean build time | — | ↓ 20% YoY | Platform |
| Incremental build (feature change) | — | < `[N]` min | Platform |
| Forbidden dependency violations | — | Trending to 0 | Architecture |
| Crash-free sessions | — | ≥ `[99.x%]` | Stream teams |
| Lead time (idea → prod) | — | ↓ per quarter | Stream teams |
| % changes touching `core` / platform | — | Stable or ↓ | Architecture |
| Open **Remediate** debt cards past SLA | — | 0 | Eng leads |

---

## 9. Module Map (Target State)

```
app (thin shell: DI, nav wiring, release config)
├── feature-*          ← stream-aligned; public API only
├── domain / core      ← business rules, timer engine, models
├── data               ← repositories, local + remote
├── platform           ← auth, analytics, network, design tokens
└── sync / wear        ← versioned contracts, shared codec
```

**Dependency rule:** `feature → domain → data → platform`. Never reversed. Features do not depend on features.

---

## 10. Quick Reference (Interview / Onboarding)

**Guardrails** = enforced nonnegotiables (CI, release gates, dependency rules).  
**Paved paths** = platform defaults that make the right thing easy.  
**Flex zones** = team choice inside module boundaries.  
**Debt triage** = accept / defer / remediate — always explicit, always owned.

> *As teams and markets multiply, the architect's job is to make integration safe and repetitive work automatic — not to review every PR.*

---

*Template v1.0 — customize bracketed `[values]` for your org. Example domains (timer, wear sync) reflect a fitness/circuits-style mobile app.*
