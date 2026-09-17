package dev.sagacity.quickstart;

import dev.sagacity.core.annotation.Compensable;
import dev.sagacity.core.annotation.Compensation;
import dev.sagacity.core.compensation.CompensationContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HR scenario — employee onboarding.
 *
 * Four steps that mirror a real enterprise onboarding workflow:
 *
 *   1. createActiveDirectoryAccount — provisions the employee in AD/Azure. Undo: deleteADAccount.
 *   2. provisionSlack               — adds the employee to the Slack workspace. Undo: deprovisionSlack.
 *   3. setupPayroll                 — enrolls in payroll system. FAILS in this demo (timeout).
 *                                     Undo: removeFromPayroll (no-op, enrollment never completed).
 *   4. sendWelcomeEmail             — sends the welcome pack. Never reached.
 *
 * When step 3 fails, Sagacity compensates step 2 (deprovisionSlack) then step 1
 * (deleteADAccount) in reverse order. The new hire ends up with no orphaned accounts.
 *
 * Without Sagacity: James Wilson has an Active Directory account and a Slack account
 * but is not in payroll and never got a welcome email. IT has to manually clean up.
 * With Sagacity: clean slate, automatic, tamper-evident audit trail of every step.
 */
@Component
public class OnboardingTools {

    /** Simulated Active Directory — maps account ID → employee details. */
    private final Map<String, Map<String, String>> adAccounts = new HashMap<>();

    /** Simulated Slack workspace — maps email → workspace. */
    private final Map<String, String> slackUsers = new HashMap<>();

    // ── Step 1: Create Active Directory account ────────────────────────────

    @Tool(description = "Create an Active Directory account for a new employee.")
    @Compensable(by = "deleteADAccount")
    public String createActiveDirectoryAccount(String firstName, String lastName,
                                               String department, String manager) {
        String email = (firstName.toLowerCase() + "." + lastName.toLowerCase() + "@acme.com");
        String accountId = "ad-" + firstName.toLowerCase().charAt(0)
                + lastName.toLowerCase().substring(0, 3)
                + "-001";
        adAccounts.put(accountId, Map.of(
                "email", email,
                "department", department,
                "manager", manager,
                "status", "ACTIVE"
        ));
        System.out.printf("  ✅ [createADAccount]   user=%-30s  id=%s%n", email, accountId);
        return accountId;
    }

    @Compensation
    public void deleteADAccount(CompensationContext ctx) {
        String accountId = ctx.result().replace("\"", "");
        Map<String, String> account = adAccounts.remove(accountId);
        String email = account != null ? account.get("email") : accountId;
        System.out.printf("  ↩️  [deleteADAccount]  account %s (%s) deleted — no orphaned credentials%n",
                accountId, email);
    }

    // ── Step 2: Provision Slack ────────────────────────────────────────────

    @Tool(description = "Add the new employee to the company Slack workspace.")
    @Compensable(by = "deprovisionSlack")
    public String provisionSlack(String adAccountId, String workspace) {
        Map<String, String> account = adAccounts.get(adAccountId);
        String email = account != null ? account.get("email") : adAccountId;
        slackUsers.put(email, workspace);
        System.out.printf("  ✅ [provisionSlack]    workspace=%-15s user=%s%n", workspace, email);
        return "slack-" + adAccountId;
    }

    @Compensation
    public void deprovisionSlack(CompensationContext ctx) {
        // ctx.input() holds the args: {"adAccountId":"...","workspace":"..."}
        String adAccountId = ctx.input().replaceAll(".*\"adAccountId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        Map<String, String> account = adAccounts.get(adAccountId);
        String email = account != null ? account.get("email") : adAccountId;
        slackUsers.remove(email);
        System.out.printf("  ↩️  [deprovisionSlack] %s removed from Slack workspace%n", email);
    }

    // ── Step 3: Setup payroll — this one fails ─────────────────────────────

    @Tool(description = "Enroll the new employee in the payroll system.")
    @Compensable(by = "removeFromPayroll")
    public String setupPayroll(String adAccountId, String salary, String startDate) {
        System.out.printf("  💼 [setupPayroll]      account=%-12s salary=%s  start=%s%n",
                adAccountId, salary, startDate);
        // Simulate a payroll system timeout — this triggers compensation of steps 1 and 2
        throw new RuntimeException("Payroll system timeout — service unavailable");
    }

    @Compensation
    public void removeFromPayroll(CompensationContext ctx) {
        // Payroll enrollment never completed (the tool threw before writing anything)
        System.out.println("  ↩️  [removeFromPayroll] enrollment never completed — nothing to reverse");
    }

    // ── Step 4: Send welcome email — never reached in this demo ───────────

    @Tool(description = "Send a welcome email and onboarding pack to the new employee.")
    @Compensable(by = "retractWelcomeEmail")
    public String sendWelcomeEmail(String adAccountId, String managerEmail) {
        Map<String, String> account = adAccounts.get(adAccountId);
        String email = account != null ? account.get("email") : adAccountId;
        System.out.printf("  📧 [sendWelcomeEmail]  to=%s  manager=%s%n", email, managerEmail);
        return "welcome-sent";
    }

    @Compensation
    public void retractWelcomeEmail(CompensationContext ctx) {
        System.out.println("  ↩️  [retractWelcomeEmail] sent a retraction email");
    }

    // ── Accessors for demo output ──────────────────────────────────────────

    public Map<String, Map<String, String>> getAdAccounts() {
        return adAccounts;
    }

    public Map<String, String> getSlackUsers() {
        return slackUsers;
    }
}
