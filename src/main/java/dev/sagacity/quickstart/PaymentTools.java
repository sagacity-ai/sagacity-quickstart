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
 * Scenario 3 — Fintech: Payment order processing.
 *
 * Four stages that mirror a real payment flow:
 *
 *   Stage 1: reserveInventory — holds stock. Undo: releaseInventory.
 *   Stage 2: chargeCard       — charges the customer. FAILS (card declined).
 *   Stage 3: sendReceipt      — emails the receipt. Never reached.
 *   Stage 4: updateLoyalty    — awards loyalty points. Never reached.
 *
 * When stage 2 fails, stage 1 compensates automatically (inventory released).
 * The card was never charged so there's nothing to reverse on the payment side.
 *
 * Without Sagacity: inventory stays reserved indefinitely. The customer was
 * never charged, has no receipt, and operations has an orphaned reservation.
 */
@Workflow(value = "payment-order", description = "Payment order with card charge")
@Component
public class PaymentTools {

    private final Map<String, Map<String, String>> reservations = new HashMap<>();

    // ── Stage 1: Reserve inventory ────────────────────────────────────────

    @Stage(order = 1, name = "reserveInventory")
    @Compensable(by = "releaseInventory")
    public String reserveInventory(String input) {
        // input is the order context passed to WorkflowRuntime.runAsync()
        String sku = "SKU-LAPTOP-PRO";
        int qty = 5;
        String reservationId = "RES-" + sku.replaceAll("[^A-Z0-9]", "") + "-001";
        reservations.put(reservationId, Map.of("sku", sku, "qty", String.valueOf(qty)));
        System.out.printf("  ✅ [Stage 1] reserveInventory  sku=%s  qty=%d  reservation=%s%n",
                sku, qty, reservationId);
        return reservationId;
    }

    @Compensation
    public void releaseInventory(CompensationContext ctx) {
        String reservationId = ctx.result().replace("\"", "");
        reservations.remove(reservationId);
        System.out.printf("  ↩  [Compensate 1] releaseInventory  reservation %s released%n",
                reservationId);
    }

    // ── Stage 2: Charge card — fails in this demo ─────────────────────────

    @Stage(order = 2, name = "chargeCard")
    @Compensable(by = "voidCharge")
    public String chargeCard(String reservationId) {
        System.out.printf("  💳 [Stage 2] chargeCard        reservation=%s  amount=£1299.99  card=****4242%n",
                reservationId);
        // Simulate card decline — triggers compensation of stage 1
        throw new RuntimeException("Card declined — insufficient funds");
    }

    @Compensation
    public void voidCharge(CompensationContext ctx) {
        // Charge never completed — nothing to reverse
        System.out.println("  ↩  [Compensate 2] voidCharge  charge was never processed — nothing to void");
    }

    // ── Stage 3: Send receipt — never reached ─────────────────────────────

    @Stage(order = 3, name = "sendReceipt")
    @Compensable(by = "recallReceipt")
    public String sendReceipt(String chargeId) {
        System.out.printf("  📧 [Stage 3] sendReceipt       charge=%s%n", chargeId);
        return "receipt-sent";
    }

    @Compensation
    public void recallReceipt(CompensationContext ctx) {
        System.out.println("  ↩  [Compensate 3] recallReceipt  sent a receipt correction");
    }

    // ── Stage 4: Update loyalty — never reached ───────────────────────────

    @Stage(order = 4, name = "updateLoyalty")
    public void updateLoyaltyPoints(String receiptId) {
        System.out.printf("  ⭐ [Stage 4] updateLoyalty     receipt=%s  +1300 points%n", receiptId);
    }

    // ── Accessor for demo output ───────────────────────────────────────────

    public Map<String, Map<String, String>> getReservations() {
        return reservations;
    }

    public void reset() {
        reservations.clear();
    }
}
