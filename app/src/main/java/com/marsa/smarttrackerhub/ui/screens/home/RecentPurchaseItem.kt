package com.marsa.smarttrackerhub.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


/**
 * Created by Muhammed Shafi on 22/06/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentPurchaseItemWithBottomSheet(
    entryEntity: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit() },
                onLongClick = {
                    onDelete()
                }
            )
    ) {
//        RecentPurchaseItem(entryEntity = entryEntity)
    }
}


//@Composable
//fun RecentPurchaseItem(entryEntity: EntryWithCategory) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 12.dp, horizontal = 16.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    imageVector = Icons.Outlined.ShoppingCart,
//                    contentDescription = entryEntity.entry.paymentType,
//                    modifier = Modifier.size(24.dp),
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Spacer(modifier = Modifier.width(12.dp))
//                Column {
//                    Text(
//                        text = entryEntity.categoryName,
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        OutlinedChip(
//                            text = entryEntity.entry.paymentType,
//                            color = if (entryEntity.entry.paymentType == "Credit Card" || entryEntity.entry.paymentType == "Not Applicable")
//                                MaterialTheme.colorScheme.error
//                            else
//                                MaterialTheme.colorScheme.primary
//                        )
//                        if (entryEntity.vendorName.isNotBlank()) {
//                            Spacer(modifier = Modifier.width(4.dp))
//                            OutlinedChip(
//                                text = entryEntity.vendorName,
//                                color = MaterialTheme.colorScheme.secondary
//                            )
//                        }
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.width(8.dp))
//
//            val amount = entryEntity.entry.amount.toDoubleOrNull() ?: 0.0
//            Text(
//                text = "$${"%.2f".format(amount)}",
//                fontSize = 14.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = MaterialTheme.colorScheme.onSurface
//            )
//        }
//    }
//}
