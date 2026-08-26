package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed
import com.example.viewmodel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel,onNavigateToCategories:()->Unit={},onNavigateToBudget:()->Unit={},modifier:Modifier=Modifier){
 val context=LocalContext.current; var showResetDialog by remember{mutableStateOf(false)}; var showLogoutDialog by remember{mutableStateOf(false)}; var resetInProgress by remember{mutableStateOf(false)}
 Scaffold(modifier=modifier.fillMaxSize()){LazyColumn(modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{Text("Settings",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
  item{Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp)){Column{
   SettingsItem(Icons.Default.SystemUpdate,"Check for Updates","Check if a newer app version is available",MaterialTheme.colorScheme.primary,{android.widget.Toast.makeText(context,"Update checker will be available after the next release build.",android.widget.Toast.LENGTH_SHORT).show()},"settings_check_updates")
   SettingsItem(Icons.Default.DeleteForever,"Reset Data","Delete all transactions and reset all wallet balances to zero",ExpenseRed,{showResetDialog=true},"settings_reset_data")
   SettingsItem(Icons.Default.Logout,"Logout","Sign out from this account",MaterialTheme.colorScheme.error,{showLogoutDialog=true},"settings_logout")
  }}}
 }}
 if(showResetDialog){AlertDialog(onDismissRequest={if(!resetInProgress)showResetDialog=false},icon={Icon(Icons.Default.DeleteForever,null,tint=ExpenseRed)},title={Text("Reset Data?")},text={Text("This will permanently delete all your transactions. Wallet balances calculated from those transactions will become ₹0. Your categories will not be deleted.")},confirmButton={Button(enabled=!resetInProgress,onClick={resetInProgress=true;viewModel.resetData({resetInProgress=false;showResetDialog=false;android.widget.Toast.makeText(context,"All transactions deleted. Wallets are now ₹0",android.widget.Toast.LENGTH_SHORT).show()},{resetInProgress=false;android.widget.Toast.makeText(context,"Reset failed. Please try again.",android.widget.Toast.LENGTH_SHORT).show()})},colors=ButtonDefaults.buttonColors(containerColor=ExpenseRed)){Text(if(resetInProgress)"Resetting..." else "Reset Data",color=Color.White)}},dismissButton={TextButton(enabled=!resetInProgress,onClick={showResetDialog=false}){Text("Cancel")}})}
 if(showLogoutDialog){AlertDialog(onDismissRequest={showLogoutDialog=false},icon={Icon(Icons.Default.Logout,null,tint=MaterialTheme.colorScheme.error)},title={Text("Logout?")},text={Text("Are you sure you want to logout?")},confirmButton={Button(onClick={showLogoutDialog=false;FirebaseAuth.getInstance().signOut()}){Text("Logout")}},dismissButton={TextButton(onClick={showLogoutDialog=false}){Text("Cancel")}})}
}

@Composable
private fun SettingsItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,tint:Color,onClick:()->Unit,testTag:String){Row(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=18.dp).testTag(testTag),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=tint,modifier=Modifier.size(24.dp));Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))}}}
