package com.example.ui.transactions

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object WalletBalanceManager {
    fun applyTransaction(
        walletId: String,
        type: String,
        amountInPaise: Long,
        onComplete: () -> Unit
    ) {
        val change = amountInPaise.toDouble() / 100.0 * if (type == "EXPENSE") -1.0 else 1.0
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete())
            .collection("wallets")
            .document(walletId)
            .update("balance", FieldValue.increment(change))
            .addOnCompleteListener { onComplete() }
    }
}
