package com.marsa.smarttrackerhub.ui.screens.shops

import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType

data class ShopFormState(
    val shopName: String = "",
    val shopAddress: String = "",
    val shopId: String = "", // Changed from shopCode
    var shopStatus: ShopStatus? = null,
    var shopType: ShopType? = null,
    val licenseExpiryDate: Long? = null // Added
)