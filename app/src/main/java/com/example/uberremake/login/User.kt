package com.example.uberremake.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.uberremake.Common
import com.example.uberremake.Model.TokenModel
import com.example.uberremake.Services.MyFirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class User(
    var name: String ="",
    var email: String = "",
    var number: String ="",
    var profileImageUrl: String? = null,
    var car: String = "",
    var carNumber: String = "",
    var rcUrl: String? = null

) {

    companion object {
        fun updateToken(context: Context, token: String) {
            val tokenModel = TokenModel()
            tokenModel.token = token

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("FCM_TOKEN", "Trying to write token for UID: $currentUid")
            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(currentUid!!)
                .setValue(tokenModel)
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Token update failed: ${e.message}")
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "Token updated successfully in database")
                }
        }
    }


}