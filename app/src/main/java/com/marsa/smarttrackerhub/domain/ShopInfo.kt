package com.marsa.smarttrackerhub.domain

import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto


/**
 * Created by Muhammed Shafi on 13/11/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

private val shopList = listOf(
    ShopListDto(
        name = "AL Marsa Grocery",
        address = "Masfout",
        shopId = "MARSA_102",
        category = ShopCategory.GROCERY,
        region = ShopRegion.UAE
    ),
    ShopListDto(
        name = "AL Marsa Grocery",
        address = "Muzeira",
        shopId = "MARSA_101",
        category = ShopCategory.GROCERY,
        region = ShopRegion.UAE
    ),
    ShopListDto(
        name = "AL Masa Super Market",
        address = "Ajman",
        shopId = "MASA_103",
        category = ShopCategory.SUPERMARKET,
        region = ShopRegion.UAE
    ),
    ShopListDto(
        name = "AL Wadi Cafe",
        address = "Muzeira",
        shopId = "WADI_101",
        category = ShopCategory.CAFE,
        region = ShopRegion.UAE
    )
)

private val summaryList = listOf(
    ShopListDto(
        name = "Shops In UAE",
        address = "Region - UAE",
        shopId = "",
        category = ShopCategory.GROCERY,
        region = ShopRegion.UAE
    )
)

fun getShopsForUser(userAccessCode: AccessCode): List<ShopListDto> {
    return when (userAccessCode) {
        // Category-specific users see only their category (all regions)
        AccessCode.GROCERY -> shopList.filter { it.category == ShopCategory.GROCERY }
        AccessCode.CAFE -> shopList.filter { it.category == ShopCategory.CAFE }
        AccessCode.SUPERMARKET -> shopList.filter { it.category == ShopCategory.SUPERMARKET }

        // Operations users see all categories but only their region
        AccessCode.OPS_UAE -> shopList.filter { it.region == ShopRegion.UAE }
        AccessCode.OPS_KUWAIT -> shopList.filter { it.region == ShopRegion.KUWAIT }

        // Admin sees everything
        AccessCode.ADMIN -> shopList

        // Guest sees nothing
        AccessCode.GUEST -> emptyList()
    }
}

fun getSummaryListUser(userAccessCode: AccessCode): List<ShopListDto> {
    return summaryList
}