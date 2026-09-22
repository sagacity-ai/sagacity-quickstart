# Sagacity Quickstart

A runnable demo of [Sagacity](https://github.com/sagacity-ai/sagacity) — the reliability layer for Spring AI agents.

Three real-world scenarios. All involve real side effects. Watch compensation run automatically and a workflow pause for human approval.

---

## What it shows

### Scenario 1 — Fintech: Payment Order Processing
**Demonstrates:** `@Compensable` on individual Spring AI tool calls.

Agent processes an order: reserve inventory → charge card → send receipt → award loyalty points.

**Failure:** Card declined at step 2.

**What Sagacity does:** Releases the inventory reservation automatically. Card was never charged — nothing to reverse there.

**Without Sagacity:** Inventory stays reserved indefinitely. Customer gets no receipt and no order. Operations has to manually hunt down the orphaned reservation.

---

### Scenario 2 — HR: Employee Onboarding
**Demonstrates:** Multi-step saga with reverse-order compensation.

Agent onboards a new hire: create AD account → provision Slack → setup payroll → send welcome email.

**Failure:** Payroll system timeout at step 3.

**What Sagacity does:** Removes Slack access, then deletes the Active Directory account — in reverse order. No orphaned credentials.

**Without Sagacity:** James Wilson has an Active Directory account and a Slack login, but is not in payroll and never got a welcome email. IT has to manually clean up.

---

### Scenario 3 — Fintech: Refund Approval Workflow
**Demonstrates:** `@Workflow`, `@Stage`, `@Gate`, stage output chaining, async execution.

A `sagacity-workflows` workflow: validate refund → issue refund → compliance gate → send confirmation.

**Gate:** The workflow pauses at stage 3 (`notifyCompliance`) and waits for a compliance officer to approve before the confirmation email goes out.

**What Sagacity does:** Executes stages in order, chains each stage's output as the next stage's input automatically, pauses at the gate, resumes on approval. If stage 2 fails instead, stage 1 is compensated automatically.

**The key difference from Scenarios 1 & 2:** The workflow is declared entirely in annotations. No orchestration code. The runtime handles stage order, chaining, gating, and compensation.

```java
@Workflow("refund-approval")
@Component
public class RefundWorkflow {

    @Stage(order = 1)
    @Compensable(by = "cancelValidation")
    public String validateRefund(String orderId) { ... }

    @Stage(order = 2)
    @Compensable(by = "reverseRefund")
    public String issueRefund(String validationId) {
        // 'validationId' injected from stage 1's return value
    }

    @Stage(order = 3)
    @Gate(approvalRequired = true, reason = "Compliance must approve before notifying customer")
    public String notifyCompliance(String refundId) { ... }

    @Stage(order = 4)
    public void sendConfirmation(String refundId) { ... }
}
```

---

## Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key

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

To see the hash-chain-verified audit trail in the dashboard:

```bash
export SAGACITY_CLOUD_API_KEY=your-api-key
```

Then open [https://sagacity-dashboard.vercel.app](https://sagacity-dashboard.vercel.app) after running.

---

## Expected output

```
╔══════════════════════════════════════════════════════════════════════╗
║   Sagacity Quickstart — The Reliability Layer for Spring AI Agents  ║
║   github.com/sagacity-ai/sagacity  ·  v0.3.0                        ║
╚══════════════════════════════════════════════════════════════════════╝

Three scenarios. All involve real-world side effects.
Watch Sagacity compensate failures and gate irreversible actions.

━━━ Scenario 1: Fintech — Payment Order (Saga + Compensation) ━━━━

  ✅ [reserveInventory]  sku=SKU-LAPTOP-PRO  qty=5  reservation=RES-SKULAPTOPPRO-001
  💳 [chargeCard]        amount=£1299.99  card=****4242  ...
  ❌  card declined — insufficient funds
  ↩️  [voidCharge]       charge was never processed — nothing to void
  ↩️  [releaseInventory] reservation RES-SKULAPTOPPRO-001 released

  Inventory reservations after compensation: EMPTY ✅

━━━ Scenario 2: HR — Employee Onboarding (Saga + Compensation) ━━━

  ✅ [createADAccount]   user=james.wilson@acme.com  id=ad-jwil-001
  ✅ [provisionSlack]    workspace=acme  user=james.wilson@acme.com
  💼 [setupPayroll]      account=ad-jwil-001  salary=£85000  ...
  ❌  payroll system timeout
  ↩️  [removeFromPayroll] enrollment never completed — nothing to reverse
  ↩️  [deprovisionSlack] james.wilson@acme.com removed from Slack workspace
  ↩️  [deleteADAccount]  account ad-jwil-001 deleted

  Active Directory accounts: EMPTY ✅
  Slack users: EMPTY ✅

━━━ Scenario 3: Fintech — Refund Approval (@Workflow + @Gate) ━━━━

  ✅ [Stage 1] validateRefund    order=ORDER-88210  eligibility=CONFIRMED
  ✅ [Stage 2] issueRefund       validation=validation-ORDER-88210  refundId=REF-12345
  ⏸️  [Stage 3] Workflow paused at compliance gate.
      Status : PAUSED_AT_GATE
      POST /sagacity/workflows/{runId}/gates/notifyCompliance/approve

  Simulating compliance officer approval (auto-approving in demo)...

  ✅ [Stage 3] notifyCompliance  refundId=REF-12345  — compliance logged
  ✅ [Stage 4] sendConfirmation  refundId=REF-12345  — email sent to customer

  Workflow status : COMPLETED
  Refund ID       : REF-12345
  Email sent      : YES ✅
```

---

## Add Sagacity to your project

```xml
<!-- Core: compensation + audit trail -->
<dependency>
    <groupId>io.github.sumitvairagar</groupId>
    <artifactId>sagacity-spring-boot-starter</artifactId>
    <version>0.3.0</version>
</dependency>

<!-- Optional: declarative workflow engine -->
<dependency>
    <groupId>io.github.sumitvairagar</groupId>
    <artifactId>sagacity-workflows</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Links

- 📖 Docs: https://sagacity-ai.github.io/sagacity/
- 🔄 Workflows guide: https://sagacity-ai.github.io/sagacity/guides/workflows/
- ⭐ GitHub: https://github.com/sagacity-ai/sagacity
- 🖥️ Dashboard: https://sagacity-dashboard.vercel.app
- 📦 Maven Central: https://central.sonatype.com/artifact/io.github.sumitvairagar/sagacity-spring-boot-starter
