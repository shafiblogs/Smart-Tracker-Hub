package com.marsa.smarttrackerhub.ui.screens.investments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.smarttrackerhub.data.AppDatabase
import com.marsa.smarttrackerhub.data.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One shop row on the shop-first Investments dashboard.
 */
data class InvestmentShopRow(
    val shopId: Int,
    val shopName: String,
    val shopStatus: String,
    val totalCapital: Double,
    val investorCount: Int,
    val allocatedPercentage: Double
)

data class InvestmentsUiState(
    val shops: List<InvestmentShopRow> = emptyList(),
    val totalCapital: Double = 0.0,
    val shopCount: Int = 0,
    val investorCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * Read-only rollup over the investment domain, shop-first: total capital raised, shop and
 * investor counts, plus a per-shop row (capital / investors / allocated %). Each row drills
 * into the existing [ShopInvestmentDashboard]; entry stays on those screens unchanged.
 *
 * Reactive to the shop table (capital totals are cached in shop_info and re-pushed on every
 * transaction edit), so the list refreshes when investments change.
 */
class InvestmentsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()

    private var initialised = false

    fun init(context: Context) {
        if (initialised) return
        initialised = true

        val db = AppDatabase.getDatabase(context)
        val shopRepo = ShopRepository(db.shopDao())
        val shopInvestorDao = db.shopInvestorDao()
        val txDao = db.investmentTransactionDao()
        val investorDao = db.investorDao()

        viewModelScope.launch {
            shopRepo.getAllShops().collect { shops ->
                val rows = shops.map { shop ->
                    InvestmentShopRow(
                        shopId = shop.id,
                        shopName = shop.shopName,
                        shopStatus = shop.shopStatus,
                        totalCapital = txDao.getTotalPaidForShop(shop.id),
                        investorCount = shopInvestorDao.getInvestorCountForShop(shop.id),
                        allocatedPercentage = shopInvestorDao.getTotalPercentageForShop(shop.id)
                    )
                }.sortedByDescending { it.totalCapital }

                _uiState.value = InvestmentsUiState(
                    shops = rows,
                    totalCapital = rows.sumOf { it.totalCapital },
                    shopCount = rows.size,
                    investorCount = investorDao.getAllInvestorsAsList().size,
                    isLoading = false
                )
            }
        }
    }
}
