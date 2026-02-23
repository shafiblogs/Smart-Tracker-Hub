package com.marsa.smarttrackerhub.ui.screens.investers

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsa.smarttrackerhub.ui.components.DropdownField

/**
 * Assigns an investor to a shop with a fixed share percentage.
 * Actual payment amounts are recorded separately via AddTransactionScreen.
 *
 * When adding this investor would push the total past 100 %, the user can
 * choose HOW to free up the required share:
 *   • Proportional — scale every existing investor down equally
 *   • From one investor — deduct the full amount from a specific investor
 *
 * Created by Muhammed Shafi on 19/02/2026.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun AddShopInvestmentScreen(
    prefilledInvestorId: Int = 0,
    prefilledShopId: Int = 0,
    onSaveSuccess: () -> Unit
) {
    val viewModel: AddShopInvestmentViewModel = viewModel()
    val state by viewModel.formState.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val investors by viewModel.investors.collectAsState()
    val isValid by viewModel.isFormValid.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val error by viewModel.error.collectAsState()
    val remainingPercentage by viewModel.remainingPercentage.collectAsState()
    val existingTotal by viewModel.existingTotal.collectAsState()
    val redistributionPreview by viewModel.redistributionPreview.collectAsState()
    val context = LocalContext.current

    val shopLocked = prefilledShopId > 0
    val investorLocked = prefilledInvestorId > 0

    // Whether redistribution UI should be shown at all:
    // only when there are existing investors AND the new share would push active total past 100%
    val newShare = state.sharePercentage.toDoubleOrNull() ?: 0.0
    val needsRedistribution = state.donorOptions.isNotEmpty() &&
            newShare > 0.0 && (existingTotal + newShare) > 100.0

    LaunchedEffect(Unit) {
        viewModel.initDatabase(
            context = context,
            prefilledInvestorId = prefilledInvestorId,
            prefilledShopId = prefilledShopId
        )
    }

    LaunchedEffect(isSaved) {
        if (isSaved) onSaveSuccess()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Assign Investor to Shop",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set the fixed share % for this investor. Payment amounts are recorded separately per phase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Shop Dropdown ──────────────────────────────────────────
                DropdownField(
                    label = "Shop",
                    selectedValue = state.selectedShopName.ifBlank { "Select a shop" },
                    options = shops.map { it.shopName },
                    onOptionSelected = { name ->
                        shops.find { it.shopName == name }?.let { viewModel.selectShop(it.id, it.shopName) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !shopLocked
                )
                if (state.shopError != null) {
                    Text(
                        text = state.shopError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                // ── Current allocation info card ───────────────────────────
                if (state.selectedShopId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = if (remainingPercentage > 0)
                                "Available to allocate: ${String.format("%.1f", remainingPercentage)}%"
                            else
                                "Fully allocated (100%). Adding a new investor will reduce an existing investor's share.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Investor Dropdown ──────────────────────────────────────
                DropdownField(
                    label = "Investor",
                    selectedValue = state.selectedInvestorName.ifBlank { "Select an investor" },
                    options = investors.map { it.investorName },
                    onOptionSelected = { name ->
                        investors.find { it.investorName == name }?.let {
                            viewModel.selectInvestor(it.id, it.investorName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !investorLocked
                )
                if (state.investorError != null) {
                    Text(
                        text = state.investorError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Share Percentage ───────────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Share Percentage",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = state.sharePercentage,
                        onValueChange = viewModel::updateSharePercentage,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("e.g. 5.0") },
                        suffix = { Text("%") },
                        isError = state.shareError != null,
                        supportingText = if (state.shareError != null) {
                            { Text(state.shareError!!, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                // ── Redistribution section (only when total would exceed 100 %) ──
                if (needsRedistribution) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Share source",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "The total would exceed 100 %. Choose how to free up the required share:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ── Mode toggle chips ──────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.distributionMode == DistributionMode.PROPORTIONAL,
                                    onClick = { viewModel.selectDistributionMode(DistributionMode.PROPORTIONAL) },
                                    label = { Text("Reduce all equally", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                )
                                FilterChip(
                                    selected = state.distributionMode == DistributionMode.SINGLE_DONOR,
                                    onClick = { viewModel.selectDistributionMode(DistributionMode.SINGLE_DONOR) },
                                    label = { Text("From one investor", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                )
                            }

                            // ── Donor picker (only in SINGLE_DONOR mode) ───
                            if (state.distributionMode == DistributionMode.SINGLE_DONOR) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Who gives their share?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                DropdownField(
                                    label = "Select investor",
                                    selectedValue = state.selectedDonorName.ifBlank { "Choose investor" },
                                    options = state.donorOptions.map { d ->
                                        "${d.investorName}  (${String.format("%.1f", d.currentShare)}%)"
                                    },
                                    onOptionSelected = { label ->
                                        val donor = state.donorOptions.find { d ->
                                            label.startsWith(d.investorName)
                                        }
                                        donor?.let {
                                            viewModel.selectDonor(it.shopInvestorId, it.investorName)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (state.donorError != null) {
                                    Text(
                                        text = state.donorError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Redistribution preview card ────────────────────────────
                if (redistributionPreview.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Share adjustment preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val previewDescription = when (state.distributionMode) {
                                DistributionMode.PROPORTIONAL ->
                                    "All existing investors will be scaled down proportionally:"
                                DistributionMode.SINGLE_DONOR ->
                                    "${state.selectedDonorName} will give their share to the new investor:"
                            }
                            Text(
                                text = previewDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            redistributionPreview.forEach { preview ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = preview.investorName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${String.format("%.1f", preview.oldShare)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "  →  ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${String.format("%.1f", preview.newShare)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (preview.newShare < 0)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (!error.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.saveAssignment(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, "Investor assigned successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onFail = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = when {
                            !needsRedistribution -> "Assign Investor"
                            state.distributionMode == DistributionMode.SINGLE_DONOR ->
                                "Assign & Transfer Share"
                            else -> "Assign & Adjust Shares"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
