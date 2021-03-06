package com.bin.binstagram

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {

    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001

    var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener {
            createAndLoginEmail()
        }

        google_signin_button.setOnClickListener {
            googleLogin()
        }

        facebook_signin_button.setOnClickListener {
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

    }

    fun createAndLoginEmail(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())?.addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }else if(task.exception?.message.isNullOrEmpty()){
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }else{

            }
        }
    }

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
                ?.addOnCompleteListener {   task ->
                    if(task.isSuccessful){
                        moveMainPage(auth?.currentUser)
                    }else{
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
        }
    }

    fun moveMainPage(user :FirebaseUser?){
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun googleLogin(){
        
        var signInIntent = googleSignInClient?.signInIntent

        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun firebaseAuthWithGoogle(account : GoogleSignInAccount){
        var credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }
    }

    fun facebookLogin(){
        LoginManager
                .getInstance()
                .logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        LoginManager
                .getInstance()
                .registerCallback(callbackManager, object  : FacebookCallback<LoginResult>{
                    override fun onSuccess(result: LoginResult?) {
                        handleFacebookAccessToken(result?.accessToken)
                    }

                    override fun onCancel() {

                    }

                    override fun onError(error: FacebookException?) {

                    }

                })
    }

    fun handleFacebookAccessToken(token:AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                moveMainPage(auth?.currentUser)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        moveMainPage(auth?.currentUser)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }
}
