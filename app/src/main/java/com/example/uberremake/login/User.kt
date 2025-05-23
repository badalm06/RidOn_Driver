package com.example.uberremake.login

import android.widget.Toast
import com.example.uberremake.Common
import com.example.uberremake.Model.TokenModel
import com.example.uberremake.Services.MyFirebaseMessagingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class User(var name: String ="", var email: String = "", var number: String ="", var profileImageUrl: String? = null
) {

    companion object {
        fun updateToken(context: MyFirebaseMessagingService, token: String) {
            val tokenModel = TokenModel()
            tokenModel.token = token;

            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(tokenModel)
                .addOnFailureListener { e -> Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {  }
        }
    }

}
