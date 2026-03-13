package com.marsa.smarttrackerhub.domain

import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.entity.ShopInfo
import com.marsa.smarttrackerhub.ui.screens.statement.ShopListDto


/**
 * Maps a Room [ShopInfo] entity → [ShopListDto] for use in ViewModels.
 *
 * shopType stored as String (e.g. "Grocery", "Cafeteria") → [ShopCategory]
 * shopRegion stored as String (e.g. "UAE", "KUWAIT") → [ShopRegion]
 * folderPath constructed from shopId using the standard SmartTracker Storage path.
 *
 * Created by Muhammed Shafi on 13/03/2026.
 * Moro Hub
 */
fun ShopInfo.toShopListDto(): ShopListDto {
    val category = when (shopType) {
        "Cafeteria" -> ShopCategory.CAFE
        "Super", "Hyper" -> ShopCategory.SUPERMARKET
        else -> ShopCategory.GROCERY  // Grocery + any unknown
    }
    val region = runCatching { ShopRegion.valueOf(shopRegion) }.getOrDefault(ShopRegion.UAE)
    return ShopListDto(
        name = shopName,
        address = shopAddress,
        shopId = shopId,
        category = category,
        region = region,
        // SmartTracker uploads to gs://smart-tracker-8012f.firebasestorage.app/shops/{shopId}/
        folderPath = "gs://smart-tracker-8012f.firebasestorage.app/shops/$shopId"
    )
}

// ── OPS virtual shops (AccountTracker aggregated views — kept hardcoded) ─────────

private val opsShops = listOf(
    ShopListDto(
        name = "Shops In UAE",
        address = "Region - UAE",
        shopId = "ops_uae",
        category = ShopCategory.OPS,
        region = ShopRegion.UAE,
        folderPath = "gs://accounts-tracker-16f93.firebasestorage.app/shop/ops_uae"
    ),
    ShopListDto(
        name = "Shops In Kuwait",
        address = "Region - Kuwait",
        shopId = "ops_kuwait",
        category = ShopCategory.OPS,
        region = ShopRegion.KUWAIT,
        folderPath = "gs://accounts-tracker-16f93.firebasestorage.app/shop/ops_kuwait"
    )
)

// ── Dynamic filter functions (read from Room DB) ──────────────────────────────

/**
 * Returns the shop list visible to [userAccessCode], loaded from Room DB.
 * Replaces the old hardcoded shopList approach.
 */
suspend fun getHomeShopUser(userAccessCode: AccessCode, db: AppDatabase): List<ShopListDto> {
    val shops = db.shopDao().getAllShopsAsList().map { it.toShopListDto() }
    return when (userAccessCode) {
        AccessCode.GROCERY      -> shops.filter { it.category == ShopCategory.GROCERY }
        AccessCode.CAFE         -> shops.filter { it.category == ShopCategory.CAFE }
        AccessCode.SUPERMARKET  -> shops.filter { it.category == ShopCategory.SUPERMARKET }
        AccessCode.OPS_UAE      -> shops.filter { it.region == ShopRegion.UAE }
        AccessCode.OPS_KUWAIT   -> shops.filter { it.region == ShopRegion.KUWAIT }
        AccessCode.ADMIN        -> shops   // all real shops
        AccessCode.GUEST        -> emptyList()
    }
}

/**
 * Returns the OPS-aggregated shops for the Account (Summary) tab.
 * These are virtual AccountTracker entries — always hardcoded.
 */
fun getSummaryShopList(userAccessCode: AccessCode): List<ShopListDto> {
    return when (userAccessCode) {
        AccessCode.ADMIN      -> opsShops
        AccessCode.OPS_UAE    -> opsShops.filter { it.region == ShopRegion.UAE }
        AccessCode.OPS_KUWAIT -> opsShops.filter { it.region == ShopRegion.KUWAIT }
        else                  -> emptyList()
    }
}

/**
 * Returns the full shop list for Statement screen, loaded from Room DB.
 * Admin and OPS users see all / region-filtered shops including OPS virtual entries.
 */
suspend fun getStatementShopList(userAccessCode: AccessCode, db: AppDatabase): List<ShopListDto> {
    val shops = db.shopDao().getAllShopsAsList().map { it.toShopListDto() }
    return when (userAccessCode) {
        AccessCode.ADMIN       -> shops + opsShops
        AccessCode.OPS_UAE     -> shops.filter { it.region == ShopRegion.UAE } +
                                   opsShops.filter { it.region == ShopRegion.UAE }
        AccessCode.OPS_KUWAIT  -> shops.filter { it.region == ShopRegion.KUWAIT } +
                                   opsShops.filter { it.region == ShopRegion.KUWAIT }
        AccessCode.GROCERY     -> shops.filter { it.category == ShopCategory.GROCERY }
        AccessCode.CAFE        -> shops.filter { it.category == ShopCategory.CAFE }
        AccessCode.SUPERMARKET -> shops.filter { it.category == ShopCategory.SUPERMARKET }
        else                   -> emptyList()
    }
}
