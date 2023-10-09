package com.example.gsignin

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*


class MainActivity : AppCompatActivity() {
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code: Int = 123
    private lateinit var firebaseAuth: FirebaseAuth

    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase!!.getReference("emails");


        val Signin: SignInButton = findViewById(R.id.bt_sign_in);

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        logout()

        Signin.setOnClickListener { view: View? ->

            if (isInternetConnected(this@MainActivity)) {
                signInGoogle()
            }
            else{
                Toast.makeText(this@MainActivity,"Please check your internet connection and try again",Toast.LENGTH_LONG).show()
            }
        }


    }
    private fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }

    }

    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Req_Code)
    }

    // onActivityResult() function : this is where
    // we provide the task and data for the Google Account
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    private fun logout() {
        mGoogleSignInClient.signOut().addOnCompleteListener(OnCompleteListener<Void?> { task ->

            if (task.isSuccessful) {
                firebaseAuth.signOut()

            }
        })
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                UpdateUI(account)
            }
        } catch (e: ApiException) {
           // Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun UpdateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                checkEmail(account.email!!)
                logout()
            }
        }
    }


    private fun checkEmail(email: String) {

        databaseReference!!.child(email.replace(".","9999"))
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(this@MainActivity, "true", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "false", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

}