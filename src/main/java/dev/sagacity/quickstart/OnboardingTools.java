package dev.sagacity.quickstart;

import dev.sagacity.core.annotation.Compensable;
import dev.sagacity.core.annotation.Compensation;
import dev.sagacity.core.compensation.CompensationContext;
import dev.sagacity.workflows.annotation.Stage;
import dev.sagacity.workflows.annotation.Workflow;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Scenario 4 — HR: Employee onboarding.
 *
 * Four stages that mirror a real enterprise onboarding workflow:
 *
 *   Stage 1: createADAccount  — provisions the employee in AD. Undo: deleteADAccount.
 *   Stage 2: provisionSlack   — adds to Slack workspace. Undo: deprovisionSlack.
 *   Stage 3: setupPayroll     — enrolls in payroll. FAILS (timeout).
 *   Stage 4: sendWelcomeEmail — sends the welcome pack. Never reached.
 *
 * When stage 3 fails, stage 2 compensates (Slack removed), then stage 1
 * compensates (AD account deleted) — in reverse order.
 *
 * Without Sagacity: James Wilson has an AD account and Slack access but is not
 * in payroll and never got a welcome email. IT has to manually clean up.
 */
@Workflow(value = "employee-onboarding", description = "New employee onboarding workflow")
@Component
public class OnboardingTools {

    private final Map<String, Map<String, String>> adAccounts = new HashMap<>();
    private final Map<String, String> slackUsers = new HashMap<>();

    // ── Stage 1: Create Active Directory account ──────────────────────────

    @Stage(order = 1, name = "createADAccount")
    @Compensable(by = "deleteADAccount")
    public String createActiveDirectoryAccount(String input) {
        String email = "james.wilson@acme.com";
        String accountId = "ad-jwil-001";
        adAccounts.put(accountId, Map.of("email", email, "dept", "Engineering"));
        System.out.printf("  ✅ [Stage 1] createADAccount   user=%-30s  id=%s%n", email, accountId);
        return accountId;
    }

    @Compensation
    public void deleteADAccount(CompensationContext ctx) {
        String accountId = ctx.result().replace("\"", "");
        Map<String, String> account = adAccounts.remove(accountId);
        String email = account != null ? account.get("email") : accountId;
        System.out.printf("  ↩  [Compensate 1] deleteADAccount  account %s (%s) deleted%n",
                accountId, email);
    }

    // ── Stage 2: Provision Slack ───────────────────────────────────────────

    @Stage(order = 2, name = "provisionSlack")
    @Compensable(by = "deprovisionSlack")
    public String provisionSlack(String adAccountId) {
        Map<String, String> account = adAccounts.get(adAccountId);
        String email = account != null ? account.get("email") : adAccountId;
        slackUsers.put(email, "acme");
        System.out.printf("  ✅ [Stage 2] provisionSlack    workspace=acme  user=%s%n", email);
        return "slack-" + adAccountId;
    }

    @Compensation
    public void deprovisionSlack(CompensationContext ctx) {
        // Find the email from the AD account map to remove from Slack
        String removed = slackUsers.entrySet().stream()
                .filter(e -> e.getValue().equals("acme"))
                .map(Map.Entry::getKey)
                .findFirst().orElse("unknown");
        slackUsers.remove(removed);
        System.out.printf("  ↩  [Compensate 2] deprovisionSlack  %s removed from Slack%n", removed);
    }

    // ── Stage 3: Setup payroll — fails in this demo ───────────────────────

    @Stage(order = 3, name = "setupPayroll")
    @Compensable(by = "removeFromPayroll")
    public String setupPayroll(String slackId) {
        System.out.printf("  💼 [Stage 3] setupPayroll      account=ad-jwil-001  salary=£85000%n");
        // Simulate a payroll system timeout — triggers compensation of stages 2 and 1
        throw new RuntimeException("Payroll system timeout — service unavailable");
    }

    @Compensation
    public void removeFromPayroll(CompensationContext ctx) {
        // Enrollment never completed — nothing to reverse
        System.out.println("  ↩  [Compensate 3] removeFromPayroll  enrollment never completed — nothing to reverse");
    }

    // ── Stage 4: Send welcome email — never reached ───────────────────────

    @Stage(order = 4, name = "sendWelcomeEmail")
    public void sendWelcomeEmail(String payrollId) {
        System.out.printf("  📧 [Stage 4] sendWelcomeEmail  to=james.wilson@acme.com%n");
    }

    // ── Accessors for demo output ──────────────────────────────────────────

    public Map<String, Map<String, String>> getAdAccounts() { return adAccounts; }
    public Map<String, String> getSlackUsers() { return slackUsers; }

    public void reset() {
        adAccounts.clear();
        slackUsers.clear();
    }
}
