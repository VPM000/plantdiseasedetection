package com.example.plantdiseasedetection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import java.io.*


class MainActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap
    private lateinit var mCameraButton: Button
    private lateinit var mGalleryButton: Button
    private lateinit var mDetectButton: Button

    lateinit var mResultTextView: TextView
    lateinit var mPhotoImageView: ImageView


    //private const val mCameraRequestCode = 22
    //private const val mGalleryRequestCode = 1000
    companion object {
        private const val REQUEST_CODE = 22
        private const val IMAGE_PICK_CODE = 1000
        private const val PERMISSION_CODE = 1001
        private const val FILE_NAME = "photo.jpg"}

    //

    private val mInputSize = 224
    private val mModelPath = "plant_disease_model.tflite"
    private val mLabelPath = "plant_labels.txt"
    private val mSamplePath = "automn.jpg"
    //var filePhoto = getPhotoFile(FILE_NAME)



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        // to make the Navigation drawer icon always appear on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        val mPhotoImageView = findViewById<ImageView>(R.id.mPhotoImageView)
        val mCameraButton = findViewById<Button>(R.id.mCameraButton)
        val mGalleryButton = findViewById<Button>(R.id.mGalleryButton)
        val mDetectButton = findViewById<Button>(R.id.mDetectButton)
        val mResultTextView = findViewById<TextView>(R.id.mResultTextView)
        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            mPhotoImageView.setImageBitmap(mBitmap)
        }
        mCameraButton.setOnClickListener { selectImage() }
        mDetectButton.setOnClickListener {
            val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
            mResultTextView.text= results?.title+"\n Confidence:"+results?.confidence
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }
    private fun selectImage() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo!")
        println("ok")
        builder.setItems(options) { dialog, item ->
            when (options[item]) {
                "Take Photo" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val f = File(
                        Environment.getExternalStorageDirectory(),
                        "temp.jpg"
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f))
                    startActivityForResult(intent, 1)
                    println("ok1")
                }
                "Choose from Gallery" -> {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    startActivityForResult(intent, 2)
                    println("ok2")
                }
                "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }
    @SuppressLint("LongLogTag")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                1 -> {
                    var f = File(Environment.getExternalStorageDirectory().toString())
                    for (temp in f.listFiles()) {
                        if (temp.name == "temp.jpg") {
                            f = temp
                            break
                        }
                    }
                    try {
                        var bitmap: Bitmap
                        val bitmapOptions = BitmapFactory.Options()
                        bitmap = BitmapFactory.decodeFile(f.absolutePath, bitmapOptions)
                        mPhotoImageView.setImageBitmap(bitmap)
                        val path = (
                                android.os.Environment.getExternalStorageDirectory()
                                    .toString() + File.separator + "Phoenix"
                                        + File.separator + "default"
                                )
                        f.delete()
                        var outFile: OutputStream? = null
                        val file = File(
                            path,
                            "${System.currentTimeMillis()}.jpg"
                        )
                        try {
                            outFile = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile)
                            outFile.flush()
                            outFile.close()
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                2 -> {
                    val selectedImage = data?.data
                    val filePath = arrayOf(MediaStore.Images.Media.DATA)
                    val c = contentResolver.query(
                        selectedImage!!,
                        filePath,
                        null,
                        null,
                        null
                    )
                    c!!.moveToFirst()
                    val columnIndex = c.getColumnIndex(filePath[0])
                    val picturePath = c.getString(columnIndex)
                    c.close()
                    val thumbnail = BitmapFactory.decodeFile(picturePath)
                    Log.w("path of image from gallery......******************.........","$picturePath")
                    mPhotoImageView.setImageBitmap(thumbnail)
                }
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }
}





    // override the onOptionsItemSelected()
    // function to implement
    // the item click listener callback
    // to open and close the navigation
    // drawer when the icon is clicked


