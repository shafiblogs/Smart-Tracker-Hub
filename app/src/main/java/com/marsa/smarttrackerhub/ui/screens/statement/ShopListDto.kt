package com.marsa.smarttrackerhub.ui.screens.statement

import com.marsa.smarttrackerhub.domain.ShopCategory
import com.marsa.smarttrackerhub.domain.ShopRegion


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class ShopListDto(
    val name: String? = null,
    val address: String? = null,
    val shopId: String? = null,
    val category: ShopCategory,
    val region: ShopRegion,
    val statementFiles: List<StatementFile> = emptyList()
)