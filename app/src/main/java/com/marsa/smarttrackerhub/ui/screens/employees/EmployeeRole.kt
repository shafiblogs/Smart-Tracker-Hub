package com.marsa.smarttrackerhub.ui.screens.employees

enum class EmployeeRole {
    ShopManager,
    ShopAssistant,
    OperationManager,
    PurchaseManager;

    fun displayName(): String {
        return when (this) {
            ShopManager -> "Shop Manager"
            ShopAssistant -> "Shop Assistant"
            OperationManager -> "Operation Manager"
            PurchaseManager -> "Purchase Manager"
        }
    }
}