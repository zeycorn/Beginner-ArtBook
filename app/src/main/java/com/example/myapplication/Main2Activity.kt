package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.ByteArrayOutputStream

class Main2Activity : AppCompatActivity() {

    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val intent = intent

        val info = intent.getStringExtra("info")

        if(info.equals("new")){
            artText.setText("")
            artistText.setText("")
            yearText.setText("")
            button.visibility = View.VISIBLE

            val selectImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.ic_launcher_background)
            imageView.setImageBitmap(selectImageBackground)
        } else {
            button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val database  = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id=?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while(cursor.moveToNext()){
                artText.setText(cursor.getString(artNameIx))
                artistText.setText(cursor.getString(artistNameIx))
                yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                imageView.setImageBitmap(bitmap)
            }

            cursor.close()
        }
    }

    fun save(view : View){
        val artName = artText.text.toString()
        val artistName = artistText.text.toString()
        val year = yearText.text.toString()

        if(selectedBitmap!= null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)
            //Image View'u alma -- Görselleri bitmap olarak değil veri olarak kaydederiz veritabanına.
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

            val sqlString = "INSERT INTO arts (artname, artistname, year,image) VALUES (?,?,?,?)"
            val statement = database.compileStatement(sqlString)
            statement.bindString(1,artName)
            statement.bindString(2,artistName)
            statement.bindString(3,year)
            statement.bindBlob(4,byteArray)

            statement.execute()

            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        }

    fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap{
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if(bitmapRatio > 1){    // YATAY
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {    //DİKEY
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view : View){

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        } else {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intentToGallery,2)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray      // aldığımız sonuçlar.
    ) {
        if(requestCode == 1){
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intentToGallery,2)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, /* galeriye gidiyo mu 2 numara mı seçili yani ona bakıcaz*/
                                  resultCode: Int, /* resim mi seçti ,cancel mı etti sonucu gösteriyor*/
                                  data: Intent? /* cidden resim seçtiyse URI yolunu tutuyor.*/  )
    {
        if(requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){

            selectedPicture = data.data
            if(selectedPicture != null){    //iki ünlem yazdığımız için baştaki ek kontrolü yaptık appi çökermemek için
                if(Build.VERSION.SDK_INT >= 28){
                    // URI'ı Bitmap'e çeviriyoruz.
                    val source = ImageDecoder.createSource(this.contentResolver,selectedPicture!!) // iki ünlem burda kesin resim var merak etme demek
                    val selectedBitmap = ImageDecoder.decodeBitmap(source)
                    imageView.setImageBitmap(selectedBitmap)
                }else {
                    val selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedPicture)
                    imageView.setImageBitmap(selectedBitmap)
                }
            }



        }

        super.onActivityResult(requestCode, resultCode, data)
    }

}