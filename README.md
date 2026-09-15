# Sagacity Quickstart

A runnable 5-minute demo of [Sagacity](https://github.com/sumitvairagar/sagacity) — the SAGA pattern for Spring AI agents.

**What it shows:**
- An AI agent runs 3 tool calls (create todo → assign → notify)
- Step 3 fails (simulated SMTP error)
- Sagacity automatically compensates steps 1 and 2 **in reverse order**
- The database ends up clean — as if nothing happened
- A tamper-evident audit trail records every step

## Prerequisites

- Java 21+
- Maven 3.9+
- An OpenAI API key (or swap for Anthropic/Ollama — see below)

## Run in 3 steps

```bash
# 1. Clone
git clone https://github.com/sumitvairagar/sagacity-quickstart.git
cd sagacity-quickstart

# 2. Set your API key
export SPRING_AI_OPENAI_API_KEY=sk-...

# 3. Run
mvn spring-boot:run
```

## Expected output

```
╔══════════════════════════════════════════════════════════════════╗
║          Sagacity Quickstart — SAGA Pattern for AI Agents       ║
╚══════════════════════════════════════════════════════════════════╝

Step 1: createTodo   ← succeeds, writes to DB
Step 2: assignTodo   ← succeeds, updates DB
Step 3: notifyAssignee ← FAILS (simulated SMTP error)

Tool calls:
  ✅ [createTodo]   id=todo-a3f2c1  title=Fix login bug  priority=high
  ✅ [assignTodo]   todo-a3f2c1 → Alice
  📧 [notifyAssignee] sending email to alice@example.com ...
  ↩️  [noOp]          email was never sent — nothing to undo
  ↩️  [unassignTodo]  removed assignee from todo-a3f2c1
  ↩️  [deleteTodo]    removed todo-a3f2c1 from DB

─────────────────────────────────────────────────────────────────
Saga status: COMPENSATED
Failure cause: SMTP server unavailable

Database state after compensation: EMPTY ✅ (all side effects undone)

Audit trail (tamper-evident hash chain):
┌─────┬────────────────────┬───────────────────────┬──────────────────────────┐
│   # │ tool               │ phase                 │ payload                  │
├─────┼────────────────────┼───────────────────────┼──────────────────────────┤
│   1 │ createTodo         │ INTENT                │ {"title":"Fix login bu…  │
│   2 │ createTodo         │ EXECUTED              │ "todo-a3f2c1"            │
│   3 │ assignTodo         │ INTENT                │ {"todoId":"todo-a3f2c1…  │
│   4 │ assignTodo         │ EXECUTED              │ "assigned todo-a3f2c1…   │
│   5 │ notifyAssignee     │ INTENT                │ {"todoId":"todo-a3f2c1…  │
│   6 │ notifyAssignee     │ FAILED                │ SMTP server unavailable  │
│   7 │ notifyAssignee     │ COMPENSATED           │                          │
│   8 │ assignTodo         │ COMPENSATED           │                          │
│   9 │ createTodo         │ COMPENSATED           │                          │
└─────┴────────────────────┴───────────────────────┴──────────────────────────┘
```

## Using Anthropic Claude instead of OpenAI

Replace the OpenAI dependency in `pom.xml`:

```xml
<!-- Remove this -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- Add this -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-anthropic</artifactId>
</dependency>
```

Update `application.yml`:

```yaml
spring:
  ai:
    anthropic:
      api-key: ${SPRING_AI_ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-5-haiku-20241022
```

## Using a real Postgres journal (for production)

Add a DataSource to `application.yml` and the Spring Boot starter wires the Postgres journal automatically:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myuser
    password: mypass
```

No extra code needed — Sagacity auto-detects the DataSource and switches from in-memory to Postgres.

## What to explore next

- **Approval gates** — mark a tool as `IRREVERSIBLE` and it suspends until a human approves via REST API. See [guides/approval-gates](https://sumitvairagar.github.io/sagacity/guides/approval-gates/).
- **Audit verification** — prove the journal hasn't been tampered with via `/sagacity/audit/{sagaId}/verify`.
- **EU AI Act Article 12** — Sagacity's hash-chained journal maps directly to Article 12 compliance requirements.

## Links

- 📖 [Full documentation](https://sumitvairagar.github.io/sagacity/)
- ⭐ [Star Sagacity on GitHub](https://github.com/sumitvairagar/sagacity)
- 🎬 [YouTube: EngineerInAI](https://youtube.com/@EngineerInAI)
