package com.marsa.smarttrackerhub.ui.screens.entry

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    navController: NavController,
    screenType: Int,
) {
//    val context = LocalContext.current
//    val activity = context as ComponentActivity
//    val sharedViewModel: SharedEntryViewModel = remember {
//        ViewModelProvider(activity)[SharedEntryViewModel::class.java]
//    }
//    val entryItem = sharedViewModel.selectedEntry
//
//    val viewModel: AddEntryViewModel = viewModel()
//    val formData by viewModel.formData.collectAsState()
//    val categoryList by viewModel.categoryList.collectAsState()
//    val vendorList by viewModel.vendorList.collectAsState()
//    val selectedDate by viewModel.selectedDate.collectAsState()
//    val saveSuccess by viewModel.saveSuccess.collectAsState()
//
//    var showDatePicker by remember { mutableStateOf(false) }
//    val interactionSource = remember { MutableInteractionSource() }
//
//    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
//
//    val initialMillis = try {
//        LocalDate.parse(selectedDate, dateFormatter)
//            .atStartOfDay(ZoneId.systemDefault())
//            .toInstant()
//            .toEpochMilli()
//    } catch (e: Exception) {
//        System.currentTimeMillis()
//    }
//
//    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
//
//    val categoryOptions = remember(categoryList) {
//        listOf("Select Category") + categoryList.map { it.name }
//    }
//    val selectedCategory = formData.category.takeIf {
//        categoryOptions.contains(it)
//    } ?: "Select Category"
//
//    LaunchedEffect(saveSuccess) {
//        if (saveSuccess) navController.popBackStack()
//    }
//
//    LaunchedEffect(screenType, entryItem) {
//        viewModel.updateScreenType(type = screenType, context = context, entryItem)
//    }
//
//    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//                .padding(paddingValues)
//                .padding(horizontal = 16.dp, vertical = 24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            InputField(
//                label = "Amount",
//                value = formData.amount,
//                onValueChange = viewModel::updateAmount,
//                keyboardType = KeyboardType.Number,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable(
//                        interactionSource = interactionSource,
//                        indication = null
//                    ) {
//                        showDatePicker = true
//                    }
//            ) {
//                ThemedOutlinedTextField(
//                    value = selectedDate,
//                    onValueChange = {},
//                    readOnly = true,
//                    enabled = false,
//                    label = {
//                        SmallTextField("Date", fontWeight = FontWeight.SemiBold)
//                    },
//                    trailingIcon = {
//                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            DropdownField(
//                label = "Category",
//                selectedValue = selectedCategory,
//                options = categoryOptions,
//                onOptionSelected = { selectedCategoryName ->
//                    val selected = categoryList.find { it.name == selectedCategoryName }
//                    val categoryId = selected?.id ?: 0
//                    viewModel.updateCategory(categoryId)
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            DropdownField(
//                label = "Payment Type",
//                selectedValue = formData.paymentType,
//                options = viewModel.paymentTypes,
//                onOptionSelected = { selected ->
//                    if (selected != "Select Payment Type") {
//                        viewModel.updatePaymentType(selected)
//                    }
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            DropdownField(
//                label = "Vendor",
//                selectedValue = formData.vendor,
//                options = vendorList.map { it.name },
//                onOptionSelected = { selectedVendorName ->
//                    val selected = vendorList.find { it.name == selectedVendorName }
//                    selected?.let { viewModel.updateVendor(it.id) }
//                },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            Button(
//                onClick = { viewModel.saveEntry(context) },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
//                shape = MaterialTheme.shapes.medium
//            ) {
//                val btnText = if (entryItem == null) "Add $screenType" else "Update $screenType"
//                Text(
//                    btnText,
//                    fontSize = 18.sp,
//                    color = MaterialTheme.colorScheme.onPrimary
//                )
//            }
//
//            if (showDatePicker) {
//                DatePickerDialog(
//                    onDismissRequest = { showDatePicker = false },
//                    confirmButton = {
//                        TextButton(onClick = {
//                            showDatePicker = false
//                            datePickerState.selectedDateMillis?.let { millis ->
//                                val date = Instant.ofEpochMilli(millis)
//                                    .atZone(ZoneId.systemDefault())
//                                    .toLocalDate()
//                                viewModel.updateDate(date)
//                            }
//                        }) {
//                            Text("OK", color = MaterialTheme.colorScheme.primary)
//                        }
//                    },
//                    dismissButton = {
//                        TextButton(onClick = { showDatePicker = false }) {
//                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
//                        }
//                    }
//                ) {
//                    DatePicker(state = datePickerState)
//                }
//            }
//        }
//    }
}