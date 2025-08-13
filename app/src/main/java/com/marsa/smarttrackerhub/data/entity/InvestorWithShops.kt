package com.marsa.smarttrackerhub.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation


/**
 * Created by Muhammed Shafi on 13/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

data class InvestorWithShops(
    @Embedded val investor: InvestorInfo,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ShopInvestorCrossRef::class,
            parentColumn = "investorId",
            entityColumn = "shopId"
        )
    )
    val shops: List<ShopInfo>
)