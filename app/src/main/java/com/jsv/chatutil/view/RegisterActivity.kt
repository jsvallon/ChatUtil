package com.jsv.chatutil.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.jsv.chatutil.R
import com.jsv.chatutil.message.LatestMessageActivity
import com.jsv.chatutil.view.login.LoginActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase



    var selectedPhotoUri: Uri? = null

    private val TAG = RegisterActivity::class.java.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        auth = Firebase.auth
        storage = Firebase.storage
        database = Firebase.database



        val currentUser = auth.currentUser


        register_button_register.setOnClickListener {
            performRegister()
        }

        already_have_account_textView.setOnClickListener {
          val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        take_picture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data !=null) {

            selectedPhotoUri = data.data


            //BUG Certain Image bot showing.

            selectedPhotoUri?.let {


            val bitmap: Bitmap
            if (Build.VERSION.SDK_INT >= 28) {
                val source: ImageDecoder.Source = ImageDecoder.createSource(
                    applicationContext.contentResolver,
                    it
                )
                try {
                    bitmap = ImageDecoder.decodeBitmap(source)
                    select_photo_image_view.setImageBitmap(bitmap)
                    take_picture.alpha = 0f
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                        applicationContext.contentResolver,
                        it
                    )
                    select_photo_image_view.setImageBitmap(bitmap)
                    take_picture.alpha = 0f
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            //val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)


            Log.d(TAG, "Select Photo successful ")

            //val bitmapDrawable = BitmapDrawable(bitmap)
            //take_picture.setBackgroundDrawable(bitmapDrawable)

            }
        } else {

            Log.d(TAG, "Select Photo Equal to null")

        }
    }

    private fun performRegister() {
        val email = email_edittext_register.text.toString()
        val password = password_editext_register.text.toString()
        if(email.isEmpty() || password.isEmpty()) {
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener

                //else statement
                Log.d(TAG, "Suss add ${it.result?.user?.uid}")
                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d(TAG, "Error  ${it.message}")

            }
    }

    private fun uploadImageToFirebaseStorage() {
        //check if the uri is different of null
        //if(selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = storage.getReference("/images/$filename")

        selectedPhotoUri?.let {
            ref.putFile(it)
                .addOnSuccessListener {
                    Log.d(TAG, "RegisterActivity Successfully uploaded image:${it.metadata?.path} ")

                    ref.downloadUrl.addOnSuccessListener {
                        Log.d(TAG, "RegisterActivity upload Image Path :$it ")

                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                    Log.d(TAG, "RegisterActivity Fail To add File :$it ")
                }
        }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uuid = auth.uid ?:""
        val ref = database.getReference("/users/$uuid")
        val user = User(uuid,username_editext_register.text.toString(), profileImageUrl)
        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "RegisterActivity Finally User Save User in Database")
                val intent = Intent(this,
                    LatestMessageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                Log.d(TAG, "RegisterActivity Fail To save in Database :$it ")
            }
    }
}



//data class User(val uid: String,val username: String, val profileImageUrl: String): Parcelable {
//    constructor(): this("","","")
//}


@Parcelize
class User(val uid: String,val username: String, val profileImageUrl: String): Parcelable {
    constructor(): this("","","")
}