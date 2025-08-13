package com.marsa.smarttrackerhub.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
data class ShopWithInvestors(
    @Embedded val shop: ShopInfo,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ShopInvestorCrossRef::class,
            parentColumn = "shopId",
            entityColumn = "investorId"
        )
    )
    val investors: List<InvestorInfo>
)