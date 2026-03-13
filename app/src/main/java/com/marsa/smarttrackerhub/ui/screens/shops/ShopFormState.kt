package com.marsa.smarttrackerhub.ui.screens.shops

import com.marsa.smarttrackerhub.domain.ShopRegion
import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType

data class ShopFormState(
    val shopName: String = "",
    val shopAddress: String = "",
    val shopId: String = "",
    var zakathStatus: ZakathStatus? = null, // Changed from shopStatus
    var shopType: ShopType? = null,
    var shopRegion: ShopRegion? = null,     // UAE | KUWAIT | KSA
    val licenseExpiryDate: Long? = null,
    val shopOpeningDate: Long? = null, // New field
    val stockValue: String = "", // New field - as String for input
    val stockTakenDate: Long? = null, // New field
    var shopStatus: ShopStatus? = null // Running | Initial | Closed
)