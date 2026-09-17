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
 * Two real-world scenarios demonstrating the SAGA pattern for Spring AI agents:
 *
 *   Scenario 1 — Fintech: Payment order processing
 *     Agent reserves inventory, attempts to charge a card (fails: card declined),
 *     Sagacity automatically releases the inventory reservation.
 *     Saga ID: payment-order-acme-2026-001
 *
 *   Scenario 2 — HR: Employee onboarding
 *     Agent creates an AD account, provisions Slack, attempts payroll setup
 *     (fails: system timeout). Sagacity automatically removes Slack access
 *     and deletes the AD account in reverse order.
 *     Saga ID: employee-onboarding-james-wilson-2026-001
 *
 * Both sagas write to the Sagacity Cloud journal (if configured) — open the
 * dashboard to see the hash-chain-verified audit trail for each one.
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
    CommandLineRunner demo(Sagacity sagacity, ChatClient.Builder builder,
                           PaymentTools paymentTools, OnboardingTools onboardingTools) {
        return args -> {
            ChatClient chatClient = builder.build();

            printBanner();

            runPaymentScenario(sagacity, chatClient, paymentTools);
            runOnboardingScenario(sagacity, chatClient, onboardingTools);

            printFooter();
        };
    }

    // ── Scenario 1: Fintech — Payment order ───────────────────────────────

    private static void runPaymentScenario(Sagacity sagacity, ChatClient chatClient,
                                           PaymentTools paymentTools) {
        System.out.println("━━━ Scenario 1: Fintech — Payment Order Processing ━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Steps: reserveInventory → chargeCard → sendReceipt → updateLoyaltyPoints");
        System.out.println("  Failure point: chargeCard (card declined)");
        System.out.println("  Saga ID: payment-order-acme-2026-001");
        System.out.println();
        System.out.println("🤖 Agent: \"Process an order for 5 units of SKU-LAPTOP-PRO.");
        System.out.println("           Charge £1,299.99 to card ending 4242 for customer C-8821.");
        System.out.println("           Send the receipt to sarah.chen@acme.com.\"");
        System.out.println();

        var tools = sagacity.wrap(paymentTools);

        SagaResult<String> result = sagacity.saga("payment-order-acme-2026-001", () ->
                chatClient.prompt()
                        .user("""
                                Process a payment order:
                                1. Reserve 5 units of SKU-LAPTOP-PRO from inventory.
                                2. Charge £1299.99 to card ending in 4242 for the reservation.
                                3. Send a receipt to sarah.chen@acme.com for £1299.99.
                                4. Award 1300 loyalty points to customer C-8821.
                                """)
                        .toolCallbacks(tools)
                        .call()
                        .content()
        );

        System.out.println();
        printScenarioResult(result, "payment-order-acme-2026-001", sagacity);
        System.out.printf("  Inventory reservations after compensation: %s%n",
                paymentTools.getReservations().isEmpty()
                        ? "EMPTY ✅ — no orphaned reservations"
                        : paymentTools.getReservations());
        System.out.println();
    }

    // ── Scenario 2: HR — Employee onboarding ──────────────────────────────

    private static void runOnboardingScenario(Sagacity sagacity, ChatClient chatClient,
                                              OnboardingTools onboardingTools) {
        System.out.println("━━━ Scenario 2: HR — Employee Onboarding ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("  Steps: createADAccount → provisionSlack → setupPayroll → sendWelcomeEmail");
        System.out.println("  Failure point: setupPayroll (system timeout)");
        System.out.println("  Saga ID: employee-onboarding-james-wilson-2026-001");
        System.out.println();
        System.out.println("🤖 Agent: \"Onboard new hire James Wilson, joining the Engineering");
        System.out.println("           team on 2026-10-01, reporting to Sarah Chen.");
        System.out.println("           Salary: £85,000. Start date: 2026-10-01.\"");
        System.out.println();

        var tools = sagacity.wrap(onboardingTools);

        SagaResult<String> result = sagacity.saga("employee-onboarding-james-wilson-2026-001", () ->
                chatClient.prompt()
                        .user("""
                                Onboard a new employee:
                                1. Create an Active Directory account for James Wilson,
                                   department Engineering, manager Sarah Chen.
                                2. Provision Slack access in the acme workspace.
                                3. Set up payroll with salary £85000, start date 2026-10-01.
                                4. Send a welcome email to James, CC manager sarah.chen@acme.com.
                                """)
                        .toolCallbacks(tools)
                        .call()
                        .content()
        );

        System.out.println();
        printScenarioResult(result, "employee-onboarding-james-wilson-2026-001", sagacity);
        System.out.printf("  Active Directory accounts after compensation: %s%n",
                onboardingTools.getAdAccounts().isEmpty()
                        ? "EMPTY ✅ — no orphaned accounts"
                        : onboardingTools.getAdAccounts());
        System.out.printf("  Slack users after compensation: %s%n",
                onboardingTools.getSlackUsers().isEmpty()
                        ? "EMPTY ✅ — no orphaned workspace access"
                        : onboardingTools.getSlackUsers());
        System.out.println();
    }

    // ── Shared output helpers ──────────────────────────────────────────────

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║       Sagacity Quickstart — SAGA Pattern for Spring AI Agents   ║");
        System.out.println("║       github.com/sagacity-ai/sagacity                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Two real-world scenarios. Both will fail mid-saga.");
        System.out.println("Watch Sagacity compensate automatically. Check the dashboard.");
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println();
    }

    private static void printScenarioResult(SagaResult<String> result, String sagaId,
                                            Sagacity sagacity) {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.printf("  Saga status  : %s%n", result.status());
        if (result.failure() != null) {
            System.out.printf("  Failure cause: %s%n", result.failure().getMessage());
        }
        System.out.println();
        System.out.println("  Audit trail (tamper-evident hash chain):");
        System.out.println("  ┌────┬──────────────────────────────┬─────────────────────────┐");
        System.out.printf("  │ %2s │ %-28s │ %-23s │%n", "#", "tool", "phase");
        System.out.println("  ├────┼──────────────────────────────┼─────────────────────────┤");
        sagacity.journal().entries(sagaId).forEach(e ->
                System.out.printf("  │ %2d │ %-28s │ %-23s │%n",
                        e.seq(),
                        truncate(e.toolName(), 28),
                        e.phase()
                )
        );
        System.out.println("  └────┴──────────────────────────────┴─────────────────────────┘");
    }

    private static void printFooter() {
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println();
        System.out.println("Both sagas are now in your Sagacity Cloud dashboard.");
        System.out.println("Open the dashboard to see the hash-chain-verified audit trail:");
        System.out.println();
        System.out.println("  🖥️  https://sagacity-dashboard.vercel.app");
        System.out.println();
        System.out.println("📖  Docs   : https://sagacity-ai.github.io/sagacity/");
        System.out.println("⭐  GitHub : https://github.com/sagacity-ai/sagacity");
        System.out.println("📦  Maven  : io.github.sumitvairagar:sagacity-spring-boot-starter:0.2.0");
        System.out.println();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
