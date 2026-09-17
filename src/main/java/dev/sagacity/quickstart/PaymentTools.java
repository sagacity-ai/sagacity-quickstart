package dev.sagacity.quickstart;

import dev.sagacity.core.annotation.Compensable;
import dev.sagacity.core.annotation.Compensation;
import dev.sagacity.core.compensation.CompensationContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Fintech scenario — payment order processing.
 *
 * Four steps that mirror a real payment flow:
 *
 *   1. reserveInventory  — holds stock for the order. Undo: releaseInventory.
 *   2. chargeCard        — charges the customer. FAILS in this demo (card declined).
 *                          Since the charge never completed, the compensation is a no-op.
 *   3. sendReceipt       — emails the receipt. Never reached in this demo.
 *   4. updateLoyalty     — awards loyalty points. Never reached in this demo.
 *
 * When step 2 fails, Sagacity compensates step 1 (releaseInventory) automatically.
 * The card was never charged — nothing to reverse there. Inventory is cleanly released.
 *
 * This is the scenario that breaks without Sagacity: inventory stays reserved,
 * the customer gets no receipt, but was also not charged. The system is inconsistent.
 */
@Component
public class PaymentTools {

    /** Simulated inventory system — maps reservation ID → item details. */
    private final Map<String, Map<String, String>> reservations = new HashMap<>();

    // ── Step 1: Reserve inventory ──────────────────────────────────────────

    @Tool(description = "Reserve inventory for an order. Returns a reservation ID.")
    @Compensable(by = "releaseInventory")
    public String reserveInventory(String sku, int quantity) {
        String reservationId = "RES-" + sku.replaceAll("[^A-Z0-9]", "") + "-001";
        reservations.put(reservationId, Map.of(
                "sku", sku,
                "quantity", String.valueOf(quantity),
                "status", "RESERVED"
        ));
        System.out.printf("  ✅ [reserveInventory]  sku=%-8s qty=%d  reservation=%s%n",
                sku, quantity, reservationId);
        return reservationId;
    }

    @Compensation
    public void releaseInventory(CompensationContext ctx) {
        String reservationId = ctx.result().replace("\"", "");
        reservations.remove(reservationId);
        System.out.printf("  ↩️  [releaseInventory] reservation %s released — stock available again%n",
                reservationId);
    }

    // ── Step 2: Charge the card — this one fails ───────────────────────────

    @Tool(description = "Charge the customer's card for the order amount in GBP.")
    @Compensable(by = "voidCharge")
    public String chargeCard(String reservationId, double amountGbp, String cardLastFour) {
        System.out.printf("  💳 [chargeCard]        amount=£%.2f  card=****%s  reservation=%s%n",
                amountGbp, cardLastFour, reservationId);
        // Simulate a card decline — this triggers compensation of step 1
        throw new RuntimeException("Card declined — insufficient funds");
    }

    @Compensation
    public void voidCharge(CompensationContext ctx) {
        // The charge never completed (the tool threw before any real side effect)
        // Nothing to reverse on the payment processor side
        System.out.println("  ↩️  [voidCharge]       charge was never processed — nothing to void");
    }

    // ── Step 3: Send receipt — never reached in this demo ─────────────────

    @Tool(description = "Email a payment receipt to the customer.")
    @Compensable(by = "recallReceipt")
    public String sendReceipt(String reservationId, String customerEmail, double amountGbp) {
        System.out.printf("  📧 [sendReceipt]       receipt → %s  amount=£%.2f%n",
                customerEmail, amountGbp);
        return "receipt-sent";
    }

    @Compensation
    public void recallReceipt(CompensationContext ctx) {
        System.out.println("  ↩️  [recallReceipt]    sent a receipt correction email");
    }

    // ── Step 4: Update loyalty points — never reached in this demo ────────

    @Tool(description = "Award loyalty points to the customer for their purchase.")
    @Compensable(by = "revokePoints")
    public String updateLoyaltyPoints(String customerId, int points) {
        System.out.printf("  ⭐ [updateLoyalty]     customer=%s  +%d points%n", customerId, points);
        return "points-awarded";
    }

    @Compensation
    public void revokePoints(CompensationContext ctx) {
        System.out.println("  ↩️  [revokePoints]     loyalty points revoked");
    }

    // ── Accessor for demo output ───────────────────────────────────────────

    public Map<String, Map<String, String>> getReservations() {
        return reservations;
    }
}
