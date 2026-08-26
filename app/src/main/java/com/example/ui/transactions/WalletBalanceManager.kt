package com.example.ui.transactions

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

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
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete())
            .collection("wallets")
            .document(walletId)
            .update("balance", FieldValue.increment(change))
            .addOnCompleteListener { onComplete() }
    }

    fun resetAllWalletBalances(onComplete: () -> Unit, onError: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onError()
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).collection("wallets").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onComplete()
                    return@addOnSuccessListener
                }
                val batch = db.batch()
                snapshot.documents.forEach { wallet ->
                    batch.update(wallet.reference, "balance", 0.0)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onError() }
            }
            .addOnFailureListener { onError() }
    }
}
