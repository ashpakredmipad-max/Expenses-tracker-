package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.CategoryIconHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class ManagedTag(
    val id: String,
    val name: String,
    val recipient: String,
    val iconName: String,
    val colorHex: String,
    val categoryName: String?
)

@Composable
fun TagsManagerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tags by remember { mutableStateOf<List<ManagedTag>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadTags() {
        scope.launch {
            loading = true
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                loading = false
                return@launch
            }
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).collection("upi_tags").get().await()
                tags = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("tagName")?.trim()
                    val recipient = doc.getString("recipient")?.trim()
                    if (name.isNullOrBlank() || recipient.isNullOrBlank()) null else ManagedTag(
                        id = doc.id,
                        name = name,
                        recipient = recipient,
                        iconName = doc.getString("iconName") ?: "Category",
                        colorHex = doc.getString("colorHex") ?: "#00897B",
                        categoryName = doc.getString("categoryName")?.trim()
                    )
                }
            } catch (e: Exception) {
                message = e.message ?: "Unable to load tags"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadTags() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            tags.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(CircleShape).then(Modifier), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Label, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No tags yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("UPI tags you create will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(tags, key = { it.id }) { tag ->
                    val tagColor = CategoryIconHelper.parseColor(tag.colorHex)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).then(Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = tagColor.copy(alpha = 0.14f)
                                ) {}
                                Icon(CategoryIconHelper.getIcon(tag.iconName), tag.name, tint = tagColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tag.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(tag.recipient, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                tag.categoryName?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                    try {
                                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                            .collection("upi_tags").document(tag.id).delete().await()
                                        tags = tags.filterNot { it.id == tag.id }
                                        message = "Tag deleted"
                                    } catch (e: Exception) {
                                        message = e.message ?: "Unable to delete tag"
                                    }
                                }
                            }) {
                                Icon(Icons.Default.DeleteOutline, "Delete tag", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        message?.let {
            Text(it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
