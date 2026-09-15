package dev.sagacity.quickstart;

import dev.sagacity.springai.SagaResult;
import dev.sagacity.springai.Sagacity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Sagacity Quickstart
 *
 * Demonstrates the SAGA pattern for Spring AI agents:
 *   - An agent is given a task that requires 3 sequential tool calls
 *   - Step 3 fails (simulated SMTP error)
 *   - Sagacity automatically compensates steps 1 and 2 in reverse order
 *   - The audit trail shows exactly what happened — tamper-evident
 *
 * Run: mvn spring-boot:run
 * Requires: SPRING_AI_OPENAI_API_KEY environment variable
 */
@SpringBootApplication
public class QuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(Sagacity sagacity, ChatClient.Builder builder, TodoTools todoTools) {
        return args -> {
            ChatClient chatClient = builder.build();

            printHeader();

            // Wrap the tools — Sagacity intercepts every call to journal it
            // and register the compensation handlers
            var tools = sagacity.wrap(todoTools);

            System.out.println("🤖 Asking the agent: \"Create a high-priority todo for fixing");
            System.out.println("   the login bug, assign it to Alice, and notify her by email.\"\n");

            // Run the agent inside a saga scope.
            // If any tool throws, Sagacity compensates all previous steps
            // in reverse order before returning.
            SagaResult<String> result = sagacity.saga("demo-saga-1", () ->
                    chatClient.prompt()
                            .user("""
                                    Create a high-priority todo item titled 'Fix login bug'.
                                    Assign it to Alice.
                                    Notify Alice at alice@example.com about the assignment.
                                    """)
                            .toolCallbacks(tools)
                            .call()
                            .content()
            );

            printResult(result, sagacity, todoTools);
        };
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          Sagacity Quickstart — SAGA Pattern for AI Agents       ║");
        System.out.println("║          github.com/sumitvairagar/sagacity                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("The agent will run 3 tool calls:");
        System.out.println("  Step 1: createTodo   ← succeeds, writes to DB");
        System.out.println("  Step 2: assignTodo   ← succeeds, updates DB");
        System.out.println("  Step 3: notifyAssignee ← FAILS (simulated SMTP error)");
        System.out.println();
        System.out.println("Expected: Sagacity compensates step 2 then step 1 automatically.");
        System.out.println("          DB should be empty after compensation.");
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println("Tool calls:");
        System.out.println();
    }

    private static void printResult(SagaResult<String> result, Sagacity sagacity, TodoTools todoTools) {
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.printf("Saga status: %s%n", result.status());

        if (result.failure() != null) {
            System.out.printf("Failure cause: %s%n", result.failure().getMessage());
        }

        System.out.println();
        System.out.printf("Database state after compensation: %s%n",
                todoTools.getDb().isEmpty() ? "EMPTY ✅ (all side effects undone)" : todoTools.getDb());

        System.out.println();
        System.out.println("Audit trail (tamper-evident hash chain):");
        System.out.println("┌─────┬────────────────────┬───────────────────────┬──────────────────────────┐");
        System.out.printf("│ %3s │ %-18s │ %-21s │ %-24s │%n", "#", "tool", "phase", "payload");
        System.out.println("├─────┼────────────────────┼───────────────────────┼──────────────────────────┤");

        sagacity.journal().entries("demo-saga-1").forEach(e ->
                System.out.printf("│ %3d │ %-18s │ %-21s │ %-24s │%n",
                        e.seq(),
                        truncate(e.toolName(), 18),
                        e.phase(),
                        truncate(e.payload(), 24)
                )
        );

        System.out.println("└─────┴────────────────────┴───────────────────────┴──────────────────────────┘");
        System.out.println();
        System.out.println("📖 Full docs: https://sumitvairagar.github.io/sagacity/");
        System.out.println("⭐ Star the project: https://github.com/sumitvairagar/sagacity");
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
