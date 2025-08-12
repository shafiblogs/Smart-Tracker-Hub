package com.marsa.smarttrackerhub.ui.screens.shops

import com.marsa.smarttrackerhub.ui.screens.enums.ShopStatus
import com.marsa.smarttrackerhub.ui.screens.enums.ShopType


/**
 * Created by Muhammed Shafi on 12/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class ShopFormState(
    val shopName: String = "",
    val shopAddress: String = "",
    val shopCode: String = "",
    var shopStatus: ShopStatus? = null,
    var shopType: ShopType? = null
)
