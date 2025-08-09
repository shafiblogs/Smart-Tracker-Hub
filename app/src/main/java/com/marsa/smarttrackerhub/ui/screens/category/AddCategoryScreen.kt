package com.marsa.smarttrackerhub.ui.screens.category

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * Created by Muhammed Shafi on 26/07/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
@Composable
fun AddCategoryScreen(
    viewModel: CategoryViewModel = viewModel(),
    onCategoryAdded: () -> Unit
) {
//    val context = LocalContext.current
//    val activity = context as ComponentActivity
//
//
//    val isEdit = categoryItem != null
//
//    var categoryName by remember { mutableStateOf("") }
//    var categoryDesc by remember { mutableStateOf("") }
//
//    LaunchedEffect(categoryItem) {
//        viewModel.initDatabase(context)
//
//        if (isEdit) {
//            categoryName = categoryItem!!.name
//            categoryDesc = categoryItem.description ?: ""
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        InputField(
//            label = "Category Name",
//            value = categoryName,
//            onValueChange = { categoryName = it },
//            imeAction = ImeAction.Next,
//            modifier = Modifier.fillMaxWidth(),
//            maxLength = 20
//        )
//
//        Spacer(Modifier.height(16.dp))
//
//        InputField(
//            label = "Description (optional)",
//            value = categoryDesc,
//            onValueChange = { categoryDesc = it },
//            imeAction = ImeAction.Done,
//            modifier = Modifier.fillMaxWidth(),
//            maxLength = 50
//        )
//
//        Spacer(Modifier.height(16.dp))
//
//        DropdownField(
//            label = "Category Type",
//            selectedValue = selectedScreenType?.name ?: "Select Type",
//            options = listOf(ScreenType.Purchase.name, ScreenType.Expense.name),
//            onOptionSelected = { selectedScreenType = ScreenType.valueOf(it) },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(Modifier.height(32.dp))
//
//        Button(
//            onClick = {
//                viewModel.updateCategory(
//                    id = categoryItem?.id ?: 0,
//                    isEdit = isEdit,
//                    name = categoryName.trim(),
//                    description = categoryDesc.takeIf { it.isNotBlank() },
//                    screenType = selectedScreenType,
//                    onSuccess = {
//                        sharedViewModel.selectedCategory = null
//                        onCategoryAdded()
//                    },
//                    onFail = { errorMessage ->
//                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
//                    }
//                )
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(50.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
//            shape = MaterialTheme.shapes.medium
//        ) {
//            Text(
//                text = if (isEdit) "Update Category" else "Add Category",
//                fontSize = 18.sp
//            )
//        }
//    }
}

