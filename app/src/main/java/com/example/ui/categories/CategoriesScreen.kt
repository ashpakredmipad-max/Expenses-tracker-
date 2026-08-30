package com.example.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.database.entity.CategoryEntity
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.CategoryIconHelper
import com.example.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(viewModel: ExpenseViewModel,onNavigateBack:()->Unit,modifier:Modifier=Modifier){
 val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle(); val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
 var selectedTab by remember{mutableIntStateOf(0)}; val currentList=if(selectedTab==0)expenseCategories else incomeCategories
 var categoryToEdit by remember{mutableStateOf<CategoryEntity?>(null)}; var categoryToDelete by remember{mutableStateOf<CategoryEntity?>(null)}; var showAddDialog by remember{mutableStateOf(false)}; var dependentTxCount by remember{mutableIntStateOf(0)}
 Scaffold(modifier=modifier.fillMaxSize(),topBar={TopAppBar(title={Text("Manage Categories",fontWeight=FontWeight.Bold)},navigationIcon={IconButton(onClick=onNavigateBack,modifier=Modifier.testTag("categories_back_button")){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface),windowInsets=WindowInsets(0,0,0,0))},floatingActionButton={FloatingActionButton(onClick={showAddDialog=true},containerColor=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,shape=RoundedCornerShape(16.dp),modifier=Modifier.testTag("fab_add_category")){Icon(Icons.Default.Add,"Add Category",modifier=Modifier.size(28.dp))}}){innerPadding->
  LazyColumn(Modifier.fillMaxSize().padding(innerPadding),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){SegmentedButton(selected=selectedTab==0,onClick={selectedTab=0},shape=SegmentedButtonDefaults.itemShape(0,2),colors=SegmentedButtonDefaults.colors(activeContainerColor=ExpenseRed.copy(alpha=.15f),activeContentColor=ExpenseRed),modifier=Modifier.testTag("tab_expense_categories")){Text("Expense Categories",fontWeight=FontWeight.Bold)};SegmentedButton(selected=selectedTab==1,onClick={selectedTab=1},shape=SegmentedButtonDefaults.itemShape(1,2),colors=SegmentedButtonDefaults.colors(activeContainerColor=IncomeGreen.copy(alpha=.15f),activeContentColor=IncomeGreen),modifier=Modifier.testTag("tab_income_categories")){Text("Income Categories",fontWeight=FontWeight.Bold)}}}
   items(currentList,key={it.id}){category->Card(Modifier.fillMaxWidth().testTag("category_card_${category.name}"),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(1.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically){CategoryIconBadge(category.iconName,category.colorHex,44);Spacer(Modifier.width(14.dp));Column{Row(verticalAlignment=Alignment.CenterVertically){Text(category.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);if(category.isDefault){Spacer(Modifier.width(8.dp));Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(6.dp)){Text("Default",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp))}}}}};Row{IconButton(onClick={categoryToEdit=category},modifier=Modifier.testTag("edit_category_${category.name}")){Icon(Icons.Default.Edit,"Edit Category",tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp))};IconButton(onClick={viewModel.checkCategoryUsage(category.id){count->dependentTxCount=count;categoryToDelete=category}},modifier=Modifier.testTag("delete_category_${category.name}")){Icon(Icons.Default.Delete,"Delete Category",tint=ExpenseRed,modifier=Modifier.size(20.dp))}}}}}
   item{Spacer(Modifier.height(64.dp))}
  }
 }
 if(showAddDialog||categoryToEdit!=null){val isEditing=categoryToEdit!=null;var catName by remember(categoryToEdit){mutableStateOf(categoryToEdit?.name?:"")};var selectedIcon by remember(categoryToEdit){mutableStateOf(categoryToEdit?.iconName?:if(selectedTab==0)"Restaurant" else "Payments")};var selectedColor by remember(categoryToEdit){mutableStateOf(categoryToEdit?.colorHex?:if(selectedTab==0)"#FF7043" else "#4CAF50")};var inputError by remember{mutableStateOf<String?>(null)};AlertDialog(onDismissRequest={showAddDialog=false;categoryToEdit=null},title={Text(if(isEditing)"Edit Category" else "Add New Category",fontWeight=FontWeight.Bold)},text={Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)){OutlinedTextField(catName,{catName=it;inputError=null},label={Text("Category Name")},singleLine=true,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().testTag("category_name_input"));if(inputError!=null)Text(inputError!!,color=ExpenseRed,style=MaterialTheme.typography.bodySmall);Text("Choose Icon",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold);FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){CategoryIconHelper.AVAILABLE_ICONS.take(16).forEach{(iconKey,vector)->val sel=selectedIcon==iconKey;Box(Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(if(sel)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).clickable{selectedIcon=iconKey},contentAlignment=Alignment.Center){Icon(vector,iconKey,tint=if(sel)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(22.dp))}}};Text("Choose Color",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold);FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){CategoryIconHelper.PRESET_COLORS.take(12).forEach{colorHex->val sel=selectedColor.equals(colorHex,true);Box(Modifier.size(34.dp).clip(CircleShape).background(CategoryIconHelper.parseColor(colorHex)).clickable{selectedColor=colorHex},contentAlignment=Alignment.Center){if(sel)Icon(Icons.Default.Check,null,tint=Color.White,modifier=Modifier.size(18.dp))}}}}},confirmButton={Button(onClick={if(catName.isBlank()){inputError="Category name cannot be empty";return@Button};if(isEditing){viewModel.updateCategory(categoryToEdit!!.copy(name=catName.trim(),iconName=selectedIcon,colorHex=selectedColor)){categoryToEdit=null}}else{viewModel.addCategory(catName.trim(),if(selectedTab==0)"EXPENSE" else "INCOME",selectedIcon,selectedColor){if(it)showAddDialog=false}}}){Text(if(isEditing)"Save Changes" else "Create")}},dismissButton={TextButton(onClick={showAddDialog=false;categoryToEdit=null}){Text("Cancel")}})}
 if(categoryToDelete!=null){val cat=categoryToDelete!!;val fallbackOther=currentList.firstOrNull{it.name.equals("Other",true)&&it.id!=cat.id};AlertDialog(onDismissRequest={categoryToDelete=null},icon={Icon(if(dependentTxCount>0)Icons.Default.Warning else Icons.Default.Delete,null,tint=ExpenseRed)},title={Text("Delete '${cat.name}'?")},text={Text(if(dependentTxCount>0)"This category has $dependentTxCount existing transaction${if(dependentTxCount>1)"s" else ""}.\n\nTo safely protect your records, these transactions will be moved to the 'Other' category upon deletion." else "Are you sure you want to delete this category?")},confirmButton={Button(onClick={categoryToDelete=null;viewModel.deleteCategory(cat,fallbackOther){}},colors=ButtonDefaults.buttonColors(containerColor=ExpenseRed)){Text("Delete",color=Color.White)}},dismissButton={TextButton(onClick={categoryToDelete=null}){Text("Cancel")}})}
}
