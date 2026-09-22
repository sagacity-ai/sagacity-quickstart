package dev.sagacity.quickstart;

import dev.sagacity.core.annotation.Compensable;
import dev.sagacity.core.annotation.Compensation;
import dev.sagacity.core.compensation.CompensationContext;
import dev.sagacity.workflows.annotation.Gate;
import dev.sagacity.workflows.annotation.Stage;
import dev.sagacity.workflows.annotation.Workflow;
import org.springframework.stereotype.Component;

/**
 * Scenario 3 — Fintech: Refund workflow.
 *
 * Demonstrates sagacity-workflows: @Stage, @Gate, and automatic compensation.
 *
 * Four stages that mirror a real refund approval process:
 *
 *   1. validateRefund    — checks the order is eligible. Compensable.
 *   2. issueRefund       — issues the refund on the payment processor. Compensable.
 *   3. notifyCompliance  — GATE: pauses the workflow for compliance officer approval
 *                          before the confirmation email goes out.
 *   4. sendConfirmation  — emails the customer. Only runs after gate is approved.
 *
 * What this shows vs. Scenarios 1 & 2:
 *   - The workflow is declared in annotations — no orchestration code.
 *   - Stage 3 pauses execution and waits for a human to approve via REST.
 *   - If stage 2 fails, stage 1 is compensated automatically in reverse order.
 *   - All stage executions are journaled with the workflow run ID.
 */
@Workflow(value = "refund-approval", description = "Customer refund with compliance gate")
@Component
public class RefundWorkflow {

    // Simulated state for demo output
    private String lastRefundId;
    private boolean confirmationSent;

    // ── Stage 1: Validate the refund request ──────────────────────────────

    @Stage(order = 1, name = "validateRefund")
    @Compensable(by = "cancelValidation")
    public String validateRefund(String orderId) {
        System.out.printf("  ✅ [Stage 1] validateRefund    order=%s  eligibility=CONFIRMED%n", orderId);
        return "validation-" + orderId;
    }

    @Compensation
    public void cancelValidation(CompensationContext ctx) {
        System.out.println("  ↩️  [Compensate 1] cancelValidation  — validation record removed");
    }

    // ── Stage 2: Issue the refund ──────────────────────────────────────────

    @Stage(order = 2, name = "issueRefund")
    @Compensable(by = "reverseRefund")
    public String issueRefund(String validationId) {
        // validationId injected automatically from stage 1's return value
        lastRefundId = "REF-" + System.currentTimeMillis() % 100000;
        System.out.printf("  ✅ [Stage 2] issueRefund       validation=%s  refundId=%s  amount=£149.99%n",
                validationId, lastRefundId);
        return lastRefundId;
    }

    @Compensation
    public void reverseRefund(CompensationContext ctx) {
        System.out.printf("  ↩️  [Compensate 2] reverseRefund  — refund %s reversed%n",
                ctx.result().replace("\"", ""));
    }

    // ── Stage 3: Compliance gate — pauses until a human approves ──────────

    @Stage(order = 3, name = "notifyCompliance")
    @Gate(approvalRequired = true, reason = "Compliance officer must approve before customer is notified")
    public String notifyCompliance(String refundId) {
        // refundId injected automatically from stage 2's return value
        // This stage body only runs AFTER the gate is approved
        System.out.printf("  ✅ [Stage 3] notifyCompliance  refundId=%s  — compliance logged%n", refundId);
        return refundId;
    }

    // ── Stage 4: Send confirmation to the customer ─────────────────────────

    @Stage(order = 4, name = "sendConfirmation")
    public void sendConfirmation(String refundId) {
        // refundId injected automatically from stage 3
        confirmationSent = true;
        System.out.printf("  ✅ [Stage 4] sendConfirmation  refundId=%s  — email sent to customer%n",
                refundId);
    }

    // ── Accessors for demo output ──────────────────────────────────────────

    public String getLastRefundId() { return lastRefundId; }
    public boolean isConfirmationSent() { return confirmationSent; }

    public void reset() {
        lastRefundId = null;
        confirmationSent = false;
    }
}
