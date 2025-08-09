package com.marsa.smarttrackerhub.ui.screens.category

import androidx.compose.runtime.Composable

@Composable
fun CategoryListScreen(onAddClick: () -> Unit, onItemClick: (Int) -> Unit) {
//    val context = LocalContext.current
//    val viewModel: CategoryViewModel = viewModel()
//    val categories by viewModel.categories.collectAsState()
//    val selectedTodayFilter by viewModel.selectedTrackFilter.collectAsState()
//    var showDeleteDialog by remember { mutableStateOf(false) }
//    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
//
//    LaunchedEffect(Unit) {
//        viewModel.initDatabase(context)
//    }
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 16.dp, vertical = 16.dp)
//        ) {
//            FilterDropdown(
//                label = selectedTodayFilter,
//                options = listOf(ScreenType.Purchase.name, ScreenType.Expense.name),
//                onOptionSelected = { viewModel.updateFilter(it) },
//                isActive = true,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            if (categories.isEmpty()) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CommonTextField(
//                        value = "No records found.",
//                        style = sTypography.bodyMedium.copy(
//                            fontWeight = FontWeight.Medium,
//                            fontSize = 20.sp
//                        )
//                    )
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(top = 8.dp)
//                ) {
//                    items(categories) { category ->
//                        CategoryItem(category = category, onDelete = {
//                            categoryToDelete = category
//                            showDeleteDialog = true
//                        }, onItemClick = {
//                            onItemClick(it)
//                        })
//                    }
//                }
//            }
//            if (showDeleteDialog && categoryToDelete != null) {
//                AlertDialog(
//                    onDismissRequest = {
//                        showDeleteDialog = false
//                        categoryToDelete = null
//                    },
//                    title = { Text("Delete Category") },
//                    text = { Text("Do you want to delete '${categoryToDelete?.name}'?") },
//                    confirmButton = {
//                        TextButton(onClick = {
//                            viewModel.deleteCategory(categoryToDelete!!.id) {
//                                Toast.makeText(
//                                    context,
//                                    "Cannot delete: Category in use",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                            showDeleteDialog = false
//                            categoryToDelete = null
//                        }) {
//                            Text("Delete")
//                        }
//                    },
//                    dismissButton = {
//                        TextButton(onClick = {
//                            showDeleteDialog = false
//                            categoryToDelete = null
//                        }) {
//                            Text("Cancel")
//                        }
//                    }
//                )
//            }
//        }
//
//
//        FloatingActionButton(
//            onClick = onAddClick,
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(16.dp),
//            containerColor = MaterialTheme.colorScheme.primary
//        ) {
//            Icon(Icons.Default.Add, contentDescription = "Add Category")
//        }
//    }
//}
//
//@Composable
//private fun CategoryItem(
//    category: Category,
//    onDelete: () -> Unit,
//    onItemClick: (Category) -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onItemClick(category) }
//            .padding(vertical = 4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 8.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = category.name,
//                    fontSize = 14.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//
//                if (!category.description.isNullOrBlank()) {
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = category.description,
//                        fontSize = 12.sp,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            IconButton(onClick = onDelete) {
//                Icon(
//                    imageVector = Icons.Outlined.Delete,
//                    tint = MaterialTheme.colorScheme.error,
//                    contentDescription = "Delete",
//                    modifier = Modifier.size(24.dp)
//                )
//            }
//        }
//    }
}


