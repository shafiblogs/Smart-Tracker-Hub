package com.marsa.smarttrackerhub.ui.screens.shops

import com.marsa.smarttrackerhub.ui.screens.enums.ShopType

data class ShopFormState(
    val shopName: String = "",
    val shopAddress: String = "",
    val shopId: String = "",
    var zakathStatus: ZakathStatus? = null, // Changed from shopStatus
    var shopType: ShopType? = null,
    val licenseExpiryDate: Long? = null,
    val shopOpeningDate: Long? = null, // New field
    val stockValue: String = "", // New field - as String for input
    val stockTakenDate: Long? = null // New field
)