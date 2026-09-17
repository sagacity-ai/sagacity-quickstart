# Sagacity Quickstart

A runnable demo of [Sagacity](https://github.com/sagacity-ai/sagacity) — the SAGA pattern for Spring AI agents.

Two real-world scenarios. Both fail mid-saga. Watch automatic compensation. Check the dashboard.

---

## What it shows

### Scenario 1 — Fintech: Payment Order Processing
**Saga ID:** `payment-order-acme-2026-001`

Agent places an order: reserve inventory → charge card → send receipt → update loyalty points.

**Failure:** Card declined at step 2.

**What Sagacity does:** Releases the inventory reservation automatically. Card was never charged — nothing to reverse there. System returns to clean state.

**Without Sagacity:** Inventory stays reserved indefinitely. Customer gets no receipt and no refund conversation makes sense because they were never charged. Operations team has to manually hunt down the orphaned reservation.

---

### Scenario 2 — HR: Employee Onboarding
**Saga ID:** `employee-onboarding-james-wilson-2026-001`

Agent onboards a new hire: create AD account → provision Slack → setup payroll → send welcome email.

**Failure:** Payroll system timeout at step 3.

**What Sagacity does:** Removes Slack access, then deletes the Active Directory account — in reverse order. No orphaned credentials.

**Without Sagacity:** James Wilson has an Active Directory account and a Slack login, but is not in payroll and never got a welcome email. IT has to manually clean up. If nobody notices, he can log into systems he was never formally onboarded to.

---

## Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key (or swap for Anthropic/Ollama — see `application.properties`)

## Run in 3 steps

```bash
# 1. Clone
git clone https://github.com/sagacity-ai/sagacity-quickstart.git
cd sagacity-quickstart

# 2. Set your API key
export SPRING_AI_OPENAI_API_KEY=sk-...

# 3. Run
mvn spring-boot:run
```

## Optional: Connect to Sagacity Cloud

To see the hash-chain-verified audit trail in the dashboard, add your API key:

```bash
export SAGACITY_CLOUD_API_KEY=your-api-key
```

Then open [https://sagacity-dashboard.vercel.app](https://sagacity-dashboard.vercel.app) after running.

## Expected output

```
╔══════════════════════════════════════════════════════════════════╗
║       Sagacity Quickstart — SAGA Pattern for Spring AI Agents   ║
║       github.com/sagacity-ai/sagacity                           ║
╚══════════════════════════════════════════════════════════════════╝

━━━ Scenario 1: Fintech — Payment Order Processing ━━━━━━━━━━━━━━

  ✅ [reserveInventory]  sku=SKU-LAPTOP-PRO  qty=5  reservation=RES-SKU9-001
  💳 [chargeCard]        amount=£1299.99  card=****4242  ...
  ❌  card declined — insufficient funds
  ↩️  [voidCharge]       charge was never processed — nothing to void
  ↩️  [releaseInventory] reservation RES-SKU9-001 released — stock available again

  Saga status  : COMPENSATED
  Inventory reservations after compensation: EMPTY ✅ — no orphaned reservations

━━━ Scenario 2: HR — Employee Onboarding ━━━━━━━━━━━━━━━━━━━━━━━━

  ✅ [createADAccount]   user=james.wilson@acme.com  id=ad-jwil-001
  ✅ [provisionSlack]    workspace=acme  user=james.wilson@acme.com
  💼 [setupPayroll]      account=ad-jwil-001  salary=£85000  start=2026-10-01
  ❌  payroll system timeout — service unavailable
  ↩️  [removeFromPayroll] enrollment never completed — nothing to reverse
  ↩️  [deprovisionSlack] james.wilson@acme.com removed from Slack workspace
  ↩️  [deleteADAccount]  account ad-jwil-001 deleted — no orphaned credentials

  Saga status  : COMPENSATED
  Active Directory accounts after compensation: EMPTY ✅ — no orphaned accounts
  Slack users after compensation: EMPTY ✅ — no orphaned workspace access
```

## Add Sagacity to your project

```xml
<dependency>
    <groupId>io.github.sumitvairagar</groupId>
    <artifactId>sagacity-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Links

- 📖 Docs: https://sagacity-ai.github.io/sagacity/
- ⭐ GitHub: https://github.com/sagacity-ai/sagacity
- 🖥️ Dashboard: https://sagacity-dashboard.vercel.app
- 📦 Maven Central: https://central.sonatype.com/artifact/io.github.sumitvairagar/sagacity-spring-boot-starter
