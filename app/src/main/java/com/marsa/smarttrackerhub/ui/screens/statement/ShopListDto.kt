package com.marsa.smarttrackerhub.ui.screens.statement


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class ShopListDto(
    val name: String? = null,
    val address: String? = null,
    val shopId: String? = null,
    val statementFiles: List<StatementFile> = emptyList()
)