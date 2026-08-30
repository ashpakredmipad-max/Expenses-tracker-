package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/ashpakredmipad-max/Expenses-tracker-/releases/latest"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel,onNavigateToCategories:()->Unit={},onNavigateToWallets:()->Unit={},onNavigateToTags:()->Unit={},modifier:Modifier=Modifier){
 val context=LocalContext.current
 var showResetDialog by remember{mutableStateOf(false)}
 var showLogoutDialog by remember{mutableStateOf(false)}
 var resetInProgress by remember{mutableStateOf(false)}
 var updateChecking by remember{mutableStateOf(false)}
 var updateDialog by remember{mutableStateOf<UpdateInfo?>(null)}
 Scaffold(modifier=modifier.fillMaxSize()){LazyColumn(modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{Text("Settings",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
  item{Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp)){Column{
   Text("Manage",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=16.dp,top=16.dp,bottom=4.dp))
   SettingsItem(Icons.Default.Settings,"Manage Categories","Add, edit or delete expense categories",MaterialTheme.colorScheme.primary,onNavigateToCategories,"settings_manage_categories")
   SettingsItem(Icons.Default.AccountBalanceWallet,"Manage Wallets","Add, edit or delete your wallets",MaterialTheme.colorScheme.primary,onNavigateToWallets,"settings_manage_wallets")
   SettingsItem(Icons.Default.Label,"Manage Tags","Manage your UPI tags",MaterialTheme.colorScheme.primary,onNavigateToTags,"settings_manage_tags")
  }}}
  item{Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),elevation=CardDefaults.cardElevation(defaultElevation=1.dp)){Column{
   SettingsItem(Icons.Default.SystemUpdate,"Check for Updates","Check if a newer app version is available",MaterialTheme.colorScheme.primary,{updateChecking=true},"settings_check_updates")
   LaunchedEffect(updateChecking){if(updateChecking){updateDialog=checkForUpdate();updateChecking=false}}
   SettingsItem(Icons.Default.DeleteForever,"Reset Data","Delete all transactions and reset all wallet balances to zero",ExpenseRed,{showResetDialog=true},"settings_reset_data")
   SettingsItem(Icons.Default.Logout,"Logout","Sign out from this account",MaterialTheme.colorScheme.error,{showLogoutDialog=true},"settings_logout")
  }}}
 }}
 if(updateChecking){AlertDialog(onDismissRequest={},title={Text("Checking for Updates")},text={Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(modifier=Modifier.size(24.dp));Spacer(Modifier.width(16.dp));Text("Checking GitHub for the latest version...")}},confirmButton={})}
 updateDialog?.let { info -> AlertDialog(onDismissRequest={updateDialog=null},title={Text(if(info.error)"Update Check Failed" else if(info.available)"Update Available" else "You're up to date")},text={Text(if(info.error)info.errorMessage else if(info.available)"Version ${info.version} is available. Your current version is ${info.currentVersion}." else "You're using the latest version (${info.currentVersion}).")},confirmButton={if(info.error||!info.available){TextButton(onClick={updateDialog=null}){Text("OK")}}else{Button(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(info.url)));updateDialog=null}){Text("Download Update")}}},dismissButton={if(info.available&&!info.error)TextButton(onClick={updateDialog=null}){Text("Cancel")}})}
 if(showResetDialog){AlertDialog(onDismissRequest={if(!resetInProgress)showResetDialog=false},icon={Icon(Icons.Default.DeleteForever,null,tint=ExpenseRed)},title={Text("Reset Data?")},text={Text("This will permanently delete all your transactions. Wallet balances calculated from those transactions will become ₹0. Your categories will not be deleted.")},confirmButton={Button(enabled=!resetInProgress,onClick={resetInProgress=true;viewModel.resetData({resetInProgress=false;showResetDialog=false;android.widget.Toast.makeText(context,"All transactions deleted. Wallets are now ₹0",android.widget.Toast.LENGTH_SHORT).show()},{resetInProgress=false;android.widget.Toast.makeText(context,"Reset failed. Please try again.",android.widget.Toast.LENGTH_SHORT).show()})},colors=ButtonDefaults.buttonColors(containerColor=ExpenseRed)){Text(if(resetInProgress)"Resetting..." else "Reset Data",color=Color.White)}},dismissButton={TextButton(enabled=!resetInProgress,onClick={showResetDialog=false}){Text("Cancel")}})}
 if(showLogoutDialog){AlertDialog(onDismissRequest={showLogoutDialog=false},icon={Icon(Icons.Default.Logout,null,tint=MaterialTheme.colorScheme.error)},title={Text("Logout?")},text={Text("Are you sure you want to logout?")},confirmButton={Button(onClick={showLogoutDialog=false;FirebaseAuth.getInstance().signOut()}){Text("Logout")}},dismissButton={TextButton(onClick={showLogoutDialog=false}){Text("Cancel")}})}
}

data class UpdateInfo(val available:Boolean,val version:String,val currentVersion:String,val url:String,val error:Boolean=false,val errorMessage:String="")

private suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
 val current=com.example.BuildConfig.VERSION_NAME
 try {
  val request=Request.Builder().url(LATEST_RELEASE_URL).header("Accept","application/vnd.github+json").header("User-Agent","Expense-Tracker-Android").build()
  OkHttpClient().newCall(request).execute().use { response ->
   if(!response.isSuccessful) return@withContext UpdateInfo(false,"",current,"",true,"GitHub returned HTTP ${response.code}.")
   val body=response.body?.string() ?: return@withContext UpdateInfo(false,"",current,"",true,"GitHub returned an empty response.")
   val json=JSONObject(body)
   val latest=json.optString("tag_name").removePrefix("v").trim()
   if(latest.isBlank()) return@withContext UpdateInfo(false,"",current,"",true,"Latest release version was not found.")
   val assets=json.optJSONArray("assets")
   var downloadUrl=json.optString("html_url")
   if(assets!=null){for(i in 0 until assets.length()){val asset=assets.optJSONObject(i);if(asset?.optString("name")=="app-release.apk"){downloadUrl=asset.optString("browser_download_url",downloadUrl);break}}}
   UpdateInfo(isNewerVersion(latest,current),latest,current,downloadUrl)
  }
 } catch(e:Exception) { UpdateInfo(false,"",current,"",true,"Could not connect to GitHub: ${e.javaClass.simpleName}.") }
}

private fun isNewerVersion(latest:String,current:String):Boolean{
 fun parts(v:String)=v.split(Regex("[^0-9]+" )).filter{it.isNotEmpty()}.map{it.toIntOrNull()?:0}
 val a=parts(latest);val b=parts(current);val size=maxOf(a.size,b.size)
 for(i in 0 until size){val x=a.getOrElse(i){0};val y=b.getOrElse(i){0};if(x!=y)return x>y}
 return false
}

@Composable
private fun SettingsItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,tint:Color,onClick:()->Unit,testTag:String=""){Row(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=18.dp).testTag(testTag),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=tint,modifier=Modifier.size(24.dp));Spacer(Modifier.width(16.dp));Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))}}}
