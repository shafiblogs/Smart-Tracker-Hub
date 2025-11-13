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
    ), ShopListDto(
        name = "Shops In UAE",
        address = "Region - UAE",
        shopId = "ops_uae",
        category = ShopCategory.OPS,
        region = ShopRegion.UAE
    ),
    ShopListDto(
        name = "Shops In Kuwait",
        address = "Region - Kuwait",
        shopId = "ops_kuwait",
        category = ShopCategory.OPS,
        region = ShopRegion.KUWAIT
    )
)

fun getHomeShopUser(userAccessCode: AccessCode): List<ShopListDto> {
    return when (userAccessCode) {
        // Category-specific users see only their category (all regions)
        AccessCode.GROCERY -> shopList.filter { it.category == ShopCategory.GROCERY }
        AccessCode.CAFE -> shopList.filter { it.category == ShopCategory.CAFE }
        AccessCode.SUPERMARKET -> shopList.filter { it.category == ShopCategory.SUPERMARKET }

        // Operations users see all categories but only their region
        AccessCode.OPS_UAE -> shopList.filter { it.region == ShopRegion.UAE && it.category != ShopCategory.OPS }
        AccessCode.OPS_KUWAIT -> shopList.filter { it.region == ShopRegion.KUWAIT && it.category != ShopCategory.OPS }

        // Admin sees everything
        AccessCode.ADMIN -> shopList.filter { it.category != ShopCategory.OPS }


        // Guest sees nothing
        AccessCode.GUEST -> emptyList()
    }
}

fun getSummaryShopList(userAccessCode: AccessCode): List<ShopListDto> {
    return when (userAccessCode) {
        AccessCode.ADMIN -> shopList.filter { it.category == ShopCategory.OPS }
        AccessCode.OPS_UAE -> shopList.filter { it.region == ShopRegion.UAE && it.category == ShopCategory.OPS }
        AccessCode.OPS_KUWAIT -> shopList.filter { it.region == ShopRegion.KUWAIT && it.category == ShopCategory.OPS }
        else -> emptyList()
    }
}

fun getStatementShopList(userAccessCode: AccessCode): List<ShopListDto> {
    return when (userAccessCode) {
        AccessCode.ADMIN -> shopList
        AccessCode.OPS_UAE -> shopList.filter { it.region == ShopRegion.UAE && it.category != ShopCategory.OPS }
        AccessCode.OPS_KUWAIT -> shopList.filter { it.region == ShopRegion.KUWAIT && it.category != ShopCategory.OPS }
        else -> emptyList()
    }
}