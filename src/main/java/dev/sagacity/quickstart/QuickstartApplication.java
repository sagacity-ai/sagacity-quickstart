package dev.sagacity.quickstart;

import dev.sagacity.workflows.WorkflowHandle;
import dev.sagacity.workflows.WorkflowRuntime;
import dev.sagacity.workflows.WorkflowStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Sagacity Quickstart — Human oversight and audit for Spring AI agents.
 *
 * No API key required. Four scenarios, all running against simulated
 * business services. Clone, run, open /sagacity/ui in your browser.
 *
 *   Scenario 1 — Refund: gate APPROVED  → workflow completes
 *   Scenario 2 — Refund: gate REJECTED  → stages 2 and 1 unwind automatically
 *   Scenario 3 — Payment: stage fails   → compensation releases inventory
 *   Scenario 4 — Onboarding: fails at stage 3 → reverse multi-step compensation
 *
 * Run:
 *   mvn spring-boot:run
 *
 * Then open:
 *   http://localhost:8080/sagacity/ui
 */
@SpringBootApplication
public class QuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(
            WorkflowRuntime workflowRuntime,
            RefundWorkflow refundWorkflow,
            PaymentTools paymentTools,
            OnboardingTools onboardingTools) {

        return args -> {
            printBanner();

            runScenario1_GateApproved(workflowRuntime, refundWorkflow);
            runScenario2_GateRejected(workflowRuntime, refundWorkflow);
            runScenario3_PaymentFailure(workflowRuntime, paymentTools);
            runScenario4_OnboardingFailure(workflowRuntime, onboardingTools);

            printFooter();
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scenario 1 — Compliance gate: APPROVED
    // ─────────────────────────────────────────────────────────────────────

    private static void runScenario1_GateApproved(
            WorkflowRuntime runtime, RefundWorkflow refundWorkflow) throws Exception {

        printScenarioHeader(1,
                "Refund — Compliance Gate: APPROVED",
                "Workflow pauses at stage 3 for human approval.\n" +
                "  Once approved, it continues and completes normally.");

        refundWorkflow.reset();
        WorkflowHandle handle = runtime.runAsync(refundWorkflow, "ORDER-88210");

        waitForGate(handle);

        System.out.println("  ⏸  [Stage 3] Paused at compliance gate — waiting for approval");
        System.out.println("      Run ID : " + handle.runId());
        System.out.println();
        System.out.println("      In production, a compliance officer approves from the UI:");
        System.out.println("      → http://localhost:8080/sagacity/ui");
        System.out.println();
        System.out.println("      Or via REST:");
        System.out.printf( "      POST /sagacity/workflows/%s/gates/notifyCompliance/approve%n%n",
                handle.runId());
        System.out.println("  ✔  Simulating compliance officer approval...");
        System.out.println();

        runtime.approveGate(handle.runId(), "notifyCompliance");
        handle.awaitCompletion(10, TimeUnit.SECONDS);

        printWorkflowResult(handle, refundWorkflow);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scenario 2 — Compliance gate: REJECTED → auto-unwind
    // ─────────────────────────────────────────────────────────────────────

    private static void runScenario2_GateRejected(
            WorkflowRuntime runtime, RefundWorkflow refundWorkflow) throws Exception {

        printScenarioHeader(2,
                "Refund — Compliance Gate: REJECTED → Auto-unwind",
                "Gate rejection triggers automatic compensation.\n" +
                "  Stages 2 and 1 are undone in reverse order — no manual cleanup needed.");

        refundWorkflow.reset();
        WorkflowHandle handle = runtime.runAsync(refundWorkflow, "ORDER-99301");

        waitForGate(handle);

        System.out.println("  ⏸  [Stage 3] Paused at compliance gate — waiting for decision");
        System.out.println();
        System.out.println("  ✖  Simulating compliance officer REJECTION...");
        System.out.println("     Reason: Refund amount exceeds policy threshold — requires manager sign-off");
        System.out.println();

        runtime.rejectGate(handle.runId(), "notifyCompliance",
                "Refund amount exceeds policy threshold — requires manager sign-off");
        handle.awaitCompletion(10, TimeUnit.SECONDS);

        printWorkflowResult(handle, refundWorkflow);
        System.out.println("  Both completed stages compensated automatically.");
        System.out.println("  No orphaned refund. No confirmation email sent to customer.");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scenario 3 — Payment order: stage failure → compensation
    // ─────────────────────────────────────────────────────────────────────

    private static void runScenario3_PaymentFailure(
            WorkflowRuntime runtime, PaymentTools paymentTools) throws Exception {

        printScenarioHeader(3,
                "Payment Order — Stage Failure → Compensation",
                "Stage 2 (chargeCard) fails — card declined.\n" +
                "  Stage 1 (reserveInventory) compensates automatically — stock released.");

        paymentTools.reset();
        WorkflowHandle handle = runtime.runAsync(paymentTools, "ORDER-55123");
        handle.awaitCompletion(10, TimeUnit.SECONDS);

        System.out.println();
        System.out.printf("  Workflow outcome : %s%n", handle.status());
        handle.failureReason().ifPresent(r -> System.out.printf("  Failure reason   : %s%n", r));
        System.out.printf("  Reservations     : %s%n",
                paymentTools.getReservations().isEmpty()
                        ? "EMPTY ✅  — no orphaned inventory reservation"
                        : paymentTools.getReservations());
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scenario 4 — Employee onboarding: multi-step reverse compensation
    // ─────────────────────────────────────────────────────────────────────

    private static void runScenario4_OnboardingFailure(
            WorkflowRuntime runtime, OnboardingTools onboardingTools) throws Exception {

        printScenarioHeader(4,
                "Employee Onboarding — Multi-step Reverse Compensation",
                "Stage 3 (setupPayroll) fails — timeout.\n" +
                "  Stage 2 (Slack) compensates, then stage 1 (AD account) compensates.\n" +
                "  No orphaned credentials. No half-onboarded employee.");

        onboardingTools.reset();
        WorkflowHandle handle = runtime.runAsync(onboardingTools, "NEW-HIRE-JAMES-WILSON");
        handle.awaitCompletion(10, TimeUnit.SECONDS);

        System.out.println();
        System.out.printf("  Workflow outcome : %s%n", handle.status());
        handle.failureReason().ifPresent(r -> System.out.printf("  Failure reason   : %s%n", r));
        System.out.printf("  AD accounts      : %s%n",
                onboardingTools.getAdAccounts().isEmpty()
                        ? "EMPTY ✅  — no orphaned credentials"
                        : onboardingTools.getAdAccounts());
        System.out.printf("  Slack users      : %s%n",
                onboardingTools.getSlackUsers().isEmpty()
                        ? "EMPTY ✅  — no orphaned workspace access"
                        : onboardingTools.getSlackUsers());
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static void waitForGate(WorkflowHandle handle) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 6_000;
        while (handle.status() != WorkflowStatus.PAUSED_AT_GATE
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    private static void printWorkflowResult(WorkflowHandle handle, RefundWorkflow workflow) {
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.printf("  Workflow status   : %s%n", handle.status());
        if (handle.status() == WorkflowStatus.COMPLETED) {
            System.out.printf("  Refund ID         : %s%n", workflow.getLastRefundId());
            System.out.printf("  Confirmation sent : YES ✅%n");
        } else {
            handle.failureReason().ifPresent(r ->
                    System.out.printf("  Rejection reason  : %s%n",
                            r.replace("gate rejected: ", "")));
            System.out.printf("  Confirmation sent : NO ✅  (gate rejected — email blocked)%n");
        }
        System.out.println();
    }

    private static void printScenarioHeader(int num, String title, String description) {
        String bar = "━".repeat(Math.max(4, 62 - title.length()));
        System.out.println("━━━ Scenario " + num + ": " + title + " " + bar);
        System.out.println();
        for (String line : description.split("\n")) {
            System.out.println("  " + line.trim());
        }
        System.out.println();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║   Sagacity Quickstart  ·  v0.4.0                                 ║");
        System.out.println("║   Human oversight and audit for Spring AI agents                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Scenario 1: Compliance gate → APPROVED  → workflow completes");
        System.out.println("  Scenario 2: Compliance gate → REJECTED  → stages unwind automatically");
        System.out.println("  Scenario 3: Payment failure → compensation releases inventory");
        System.out.println("  Scenario 4: Onboarding failure → reverse multi-step compensation");
        System.out.println();
        System.out.println("  No API key required.");
        System.out.println();
        System.out.println("  ┌─ Open this now ──────────────────────────────────────────────┐");
        System.out.println("  │  http://localhost:8080/sagacity/ui                           │");
        System.out.println("  │  All workflow runs appear here live.                         │");
        System.out.println("  └──────────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println();
    }

    private static void printFooter() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("  All 4 scenarios complete.");
        System.out.println();
        System.out.println("  The embedded UI shows every run with node-graph flow diagrams");
        System.out.println("  and a tamper-evident audit trail:");
        System.out.println("  → http://localhost:8080/sagacity/ui");
        System.out.println();
        System.out.println("  The server is still running. To see a live gate approval:");
        System.out.println("  restart the app and approve scenario 1 manually from the UI.");
        System.out.println();
        System.out.println("  📖  Docs   : https://sagacity-ai.github.io/sagacity/");
        System.out.println("  ⭐  GitHub : https://github.com/sagacity-ai/sagacity");
        System.out.println("  📦  Maven  : io.github.sumitvairagar:sagacity-spring-boot-starter:0.4.0");
        System.out.println();
    }
}
