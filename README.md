# Sagacity Quickstart

**Human oversight and audit for Spring AI agents.**

No API key required. Clone, run, open your browser. See all 4 scenarios in under 60 seconds.

---

## What it shows

### Scenario 1 — Compliance gate: APPROVED
A refund workflow runs stages 1 and 2, then **pauses at stage 3** waiting for a compliance officer to approve before the confirmation email goes out.

The gate survives JVM restarts — state is in-memory here but JDBC-backed in production.

Once approved, stages 3 and 4 execute. Workflow completes.

### Scenario 2 — Compliance gate: REJECTED → auto-unwind
Same refund workflow, but this time the compliance officer **rejects** the gate.

Sagacity automatically compensates stage 2 (reverses the refund), then stage 1 (cancels the validation) — in reverse order. No orphaned state. No customer email sent.

**This is the unique part:** every other human-in-the-loop tool lets a human approve or reject. None of them automatically unwind what already happened when the answer is reject.

### Scenario 3 — Payment order: stage failure → compensation
Agent reserves inventory, then the card is declined. Stage 1 compensates automatically — inventory released, no orphaned reservation.

### Scenario 4 — Employee onboarding: multi-step reverse compensation
AD account created, Slack provisioned, then payroll times out. Stage 2 (Slack) compensates, then stage 1 (AD account) compensates — in reverse order. No orphaned credentials.

---

## Run in 3 steps

```bash
# 1. Clone
git clone https://github.com/sagacity-ai/sagacity-quickstart.git
cd sagacity-quickstart

# 2. Build and run
mvn package -DskipTests -q
java -Dspring.main.web-application-type=servlet -jar target/sagacity-quickstart-1.0.0.jar

# 3. Open the embedded UI
open http://localhost:8080/sagacity/ui
```

No API key. No database. No extra config.

> **Spring Boot 4 note:** The `-Dspring.main.web-application-type=servlet` flag is required because
> Spring Boot 4 changed how it detects servlet web apps. Without it, Tomcat doesn't start.
> This is a known Boot 4 behaviour — the flag tells the runtime to start Tomcat before
> `application.yml` is processed.

---

## What you'll see in the UI

Open `http://localhost:8080/sagacity/ui` while the app is running:

- All 4 workflow runs listed with status badges
- Click any row → node-graph flow diagram showing each stage
- For gate runs: see whether the gate was approved or rejected
- Compensation runs show the undo path in amber
- Every run has a tamper-evident audit journal — click to see it

---

## The code

The entire governance layer is annotations:

```java
@Workflow("refund-approval")
@Component
public class RefundWorkflow {

    @Stage(order = 1)
    @Compensable(by = "cancelValidation")       // auto-undone if anything fails
    public String validateRefund(String orderId) { ... }

    @Stage(order = 2)
    @Compensable(by = "reverseRefund")
    public String issueRefund(String validationId) { ... }

    @Stage(order = 3)
    @Gate(approvalRequired = true,               // workflow pauses here
          reason = "Compliance must approve before customer is notified")
    public String notifyCompliance(String refundId) { ... }

    @Stage(order = 4)
    public void sendConfirmation(String ref) { ... }
}
```

```java
// Approve from the UI, or via REST:
POST /sagacity/workflows/{runId}/gates/notifyCompliance/approve

// Reject — stages 2 and 1 compensate automatically:
POST /sagacity/workflows/{runId}/gates/notifyCompliance/reject
     {"reason": "Exceeds policy threshold"}
```

---

## Add to your project

```xml
<dependency>
    <groupId>io.github.sumitvairagar</groupId>
    <artifactId>sagacity-spring-boot-starter</artifactId>
    <version>0.4.0</version>
</dependency>

<!-- Workflow engine: @Workflow, @Stage, @Gate, @Check -->
<dependency>
    <groupId>io.github.sumitvairagar</groupId>
    <artifactId>sagacity-workflows</artifactId>
    <version>0.4.0</version>
</dependency>
```

---

## Links

- 📖 Docs: https://sagacity-ai.github.io/sagacity/
- 🔄 Workflows guide: https://sagacity-ai.github.io/sagacity/guides/workflows/
- ⭐ GitHub: https://github.com/sagacity-ai/sagacity
- 📦 Maven Central: https://central.sonatype.com/artifact/io.github.sumitvairagar/sagacity-spring-boot-starter
