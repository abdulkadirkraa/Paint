package com.abdulkadirkara.paint

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.drawToBitmap
import androidx.room.Room
import com.abdulkadirkara.paint.databinding.ActivityMainBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private var dirPath=""
    private var fileName=""
    private lateinit var db: PaintDB
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var binding: ActivityMainBinding

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        db= Room.databaseBuilder(
            this,
            PaintDB::class.java,
            "paints"
        ).build()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        bottomSheetBehavior= BottomSheetBehavior.from(binding.bottomSheetSave.bottomSheet)
        bottomSheetBehavior.peekHeight=0
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED


        val filepath=intent.getStringExtra("filePath")
        val filename=intent.getStringExtra("fileName")

        if (!filename.isNullOrEmpty()){
            binding.drawingView.visibility= View.GONE
            binding.img.visibility= View.VISIBLE

            val imgFile= File("$filepath$filename.jpg")
            if (imgFile.exists()){
                val myBitmap= BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.img.setImageBitmap(myBitmap)
                binding.ll.visibility=View.GONE
                binding.toolbar.visibility=View.VISIBLE

                setSupportActionBar(binding.toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowHomeEnabled(true)
                binding.toolbar.setNavigationOnClickListener {
                    onBackPressed()
                }
                binding.tvFilename.text=filename
            }
        }

        binding.btnSave.setOnClickListener {
            bottomSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED
            val sdf= SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
            val date=sdf.format(Date())
            fileName="paint_image_$date"
            binding.bottomSheetSave.filenameInput.setText(fileName)
        }

        binding.bottomSheetSave.btnCancel.setOnClickListener {
            if (File("$dirPath$fileName.jpg").exists()){
                GlobalScope.launch {
                    File("$dirPath$fileName.jpg").delete()
                }
            }
            dismiss()
            binding.drawingView.clearDrawingBoard()
        }

        binding.bottomSheetSave.btnSave.setOnClickListener {
            dismiss()
            save(fileName)
        }

        binding.btnUndo.setOnClickListener {
            binding.drawingView.undo()
        }

        binding.btnRedo.setOnClickListener {
            binding.drawingView.redo()
        }

        binding.btnColor.setOnClickListener {
            openColorPicker()
        }

        binding.btnBrush.setOnClickListener {
            val builder= AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.alert, null)
            builder.setView(dialogView)

            val seekbarSize = dialogView.findViewById<SeekBar>(R.id.seekbarSize)
            val seekbarTransparan = dialogView.findViewById<SeekBar>(R.id.seekbarTransparan)
            val textViewSize = dialogView.findViewById<TextView>(R.id.textViewSize)
            val textViewTranspaaran = dialogView.findViewById<TextView>(R.id.textViewTranspaaran)

            seekbarSize.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textViewSize.text=progress.toString()
                    binding.drawingView.setSizeForBrush(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })

            seekbarTransparan.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    textViewTranspaaran.text=progress.toString()
                    binding.drawingView.setBrushAlpha(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            val alertDialog = builder.create()
            alertDialog.setCancelable(true)
            alertDialog.show()
        }

        binding.btnClean.setOnClickListener {
            binding.drawingView.clearDrawingBoard()
        }

        binding.btnDelete.setOnClickListener {
            binding.drawingView.erase()
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun save(fileName: String){
        val newfilename=binding.bottomSheetSave.filenameInput.text.toString()
        if (newfilename.isEmpty() || newfilename.isBlank()){
            Toast.makeText(this@MainActivity,"A name required for save !",Toast.LENGTH_SHORT).show()
        }else{
            if (newfilename != fileName){
                var newfile=File("$dirPath$newfilename.jpg")
                File("$dirPath$fileName.jpg").renameTo(newfile)
                this.fileName=newfilename

            }

            val imageBitmap: Bitmap=binding.drawingView.drawToBitmap()
            dirPath="${getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath}/"

            val imageFile=File("$dirPath${this.fileName}.jpg")
            try {
                val fos = FileOutputStream(imageFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()

                val image=Paint(this.fileName,dirPath)
                GlobalScope.launch {
                    db.paintDao().insert(image)
                    addImageToGallery(applicationContext, imageFile)
                }

                Toast.makeText(this,"${this.fileName} picture saved",Toast.LENGTH_SHORT).show()

            }catch (e:IOException){
                Toast.makeText(this,"An error occurred while saving the $fileName image",Toast.LENGTH_SHORT).show()
            }

            val intent=Intent(this,EntranceActivity::class.java)
            Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)

        }
    }

    private fun dismiss(){
        hideKeyboard(binding.bottomSheetSave.filenameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
        },100)
    }

    private fun hideKeyboard(view: View){
        val imm=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }

    private fun openColorPicker(){
        ColorPickerDialog
            .Builder(this)
            .setTitle("Pick Color")
            .setColorShape(ColorShape.SQAURE)
            .setColorListener { color, colorHex ->
                binding.drawingView.setBrushColor(color)
            }
            .show()
    }

    private fun addImageToGallery(context: Context, file: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}