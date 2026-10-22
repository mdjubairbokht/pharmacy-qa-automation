package com.rxcare.qa.api.models;

public record InventoryAdjustment(Integer quantityChange, String type, String reason) {

    public static InventoryAdjustment receive(int quantity, String reference) {
        return new InventoryAdjustment(quantity, "RECEIVE", reference);
    }

    public static InventoryAdjustment adjust(int quantity, String reason) {
        return new InventoryAdjustment(quantity, "ADJUST", reason);
    }
}
