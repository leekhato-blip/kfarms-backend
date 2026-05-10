package com.kfarms.entity;

public enum StockAdjustmentReason {
    PURCHASE,       // + stock
    SALE,           // - stock
    CONSUMPTION,    // - stock
    TRANSFER_IN,    // + stock
    TRANSFER_OUT,   // - stock
    OTHER           // any other reason
}
