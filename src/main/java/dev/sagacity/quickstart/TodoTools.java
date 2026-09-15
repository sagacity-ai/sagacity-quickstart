package dev.sagacity.quickstart;

import dev.sagacity.core.annotation.Compensable;
import dev.sagacity.core.annotation.Compensation;
import dev.sagacity.core.compensation.CompensationContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simulated todo-list tools with compensation declared on each one.
 *
 * Three tools, three steps:
 *   1. createTodo   — writes to an in-memory DB. Undo: deleteTodo.
 *   2. assignTodo   — sets an assignee field. Undo: unassignTodo.
 *   3. notifyTodo   — simulates sending an email (always fails in this demo).
 *                     Undo: noOp (email was never sent, nothing to reverse).
 *
 * When step 3 fails, Sagacity walks backward and calls unassignTodo then
 * deleteTodo automatically. The DB ends up empty — clean state restored.
 */
@Component
public class TodoTools {

    // Simulated in-memory database — represents real side effects
    private final Map<String, Map<String, String>> db = new HashMap<>();

    // ── Step 1 ────────────────────────────────────────────────────────────

    @Tool(description = "Create a new todo item with a title and priority level")
    @Compensable(by = "deleteTodo")
    public String createTodo(String title, String priority) {
        String id = "todo-" + UUID.randomUUID().toString().substring(0, 6);
        db.put(id, new HashMap<>(Map.of(
                "title", title,
                "priority", priority,
                "status", "open"
        )));
        System.out.printf("  ✅ [createTodo]  id=%-12s title=%s priority=%s%n", id, title, priority);
        return id; // return the ID — the compensation needs it
    }

    @Compensation
    public void deleteTodo(CompensationContext ctx) {
        // ctx.result() holds what createTodo returned — the todo ID
        String todoId = ctx.result().replace("\"", "");
        db.remove(todoId);
        System.out.printf("  ↩️  [deleteTodo]  removed %s from DB%n", todoId);
    }

    // ── Step 2 ────────────────────────────────────────────────────────────

    @Tool(description = "Assign a todo item to a team member by their name")
    @Compensable(by = "unassignTodo")
    public String assignTodo(String todoId, String assignee) {
        Map<String, String> todo = db.get(todoId);
        if (todo != null) {
            todo.put("assignee", assignee);
        }
        System.out.printf("  ✅ [assignTodo]  %s → %s%n", todoId, assignee);
        return "assigned " + todoId + " to " + assignee;
    }

    @Compensation
    public void unassignTodo(CompensationContext ctx) {
        // ctx.input() holds the original JSON args: {"todoId":"...","assignee":"..."}
        String todoId = ctx.input().replaceAll(".*\"todoId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        Map<String, String> todo = db.get(todoId);
        if (todo != null) {
            todo.remove("assignee");
        }
        System.out.printf("  ↩️  [unassignTodo] removed assignee from %s%n", todoId);
    }

    // ── Step 3 — this one always fails ───────────────────────────────────

    @Tool(description = "Notify the assignee about the todo via email")
    @Compensable(by = "noOp")
    public String notifyAssignee(String todoId, String email) {
        System.out.printf("  📧 [notifyAssignee] sending email to %s ...%n", email);
        // Simulate an SMTP failure — this is what triggers compensation
        throw new RuntimeException("SMTP server unavailable");
    }

    @Compensation
    public void noOp(CompensationContext ctx) {
        // Email was never sent (the tool threw before any real side effect)
        // Nothing to reverse, but we still log it for the audit trail
        System.out.println("  ↩️  [noOp]          email was never sent — nothing to undo");
    }

    // ── Accessor for the demo to inspect DB state ────────────────────────

    public Map<String, Map<String, String>> getDb() {
        return db;
    }
}
