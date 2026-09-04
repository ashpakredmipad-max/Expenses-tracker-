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

    fun transfer(
        fromWalletId: String,
        toWalletId: String,
        amount: Double,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || fromWalletId == toWalletId || amount <= 0.0) {
            onError()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val wallets = db.collection("users").document(uid).collection("wallets")
        val from = wallets.document(fromWalletId)
        val to = wallets.document(toWalletId)
        val batch = db.batch()
        batch.update(from, "balance", FieldValue.increment(-amount))
        batch.update(to, "balance", FieldValue.increment(amount))
        batch.commit().addOnSuccessListener { onSuccess() }.addOnFailureListener { onError() }
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
