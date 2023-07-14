package com.abdulkadirkara.paint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.abdulkadirkara.paint.databinding.ActivityEntranceBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception

class EntranceActivity : AppCompatActivity(),OnItemClickListener {

    private lateinit var images: ArrayList<Paint>
    private lateinit var mAdapter: Adapter
    private lateinit var db: PaintDB

    private var allChecked=false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var binding: ActivityEntranceBinding

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityEntranceBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES),
                PackageManager.PERMISSION_GRANTED)
        }else{
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                PackageManager.PERMISSION_GRANTED)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        bottomSheetBehavior=BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_HIDDEN

        images= ArrayList()

        db= Room.databaseBuilder(
            this,
            PaintDB::class.java,
            "paints"
        ).build()

        mAdapter= Adapter(images,this)

        binding.recyclerview.apply {
            adapter=mAdapter
            layoutManager= LinearLayoutManager(context)
        }

        fetchAll()

        binding.searchInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query=s.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnClose.setOnClickListener {
            leaveEditMode()
        }

        binding.btnSelectAll.setOnClickListener {
            allChecked= !allChecked
            images.map { it.isChecked=allChecked }
            mAdapter.notifyDataSetChanged()

            if (allChecked){
                enableDelete()
                disableEdit()
            }else{
                disableDelete()
                disableEdit()
            }
        }

        binding.btnEdit.setOnClickListener {
            val builder=AlertDialog.Builder(this@EntranceActivity)
            val dialogView= this.layoutInflater.inflate(R.layout.rename_layout,null)
            builder.setView(dialogView)
            val dialog=builder.create()

            val paint= images.filter { it.isChecked }[0]
            val textInput=dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
            textInput.setText(paint.imageName)

            dialogView.findViewById<Button>(R.id.btnRename).setOnClickListener {
                val input=textInput.text.toString()
                if (input.isEmpty()){
                    Toast.makeText(this,"A name is required",Toast.LENGTH_SHORT).show()
                }else{

                    val newFile= File("${paint.imagePath}$input.jpg")
                    GlobalScope.launch {
                        File("${paint.imagePath}${paint.imageName}.jpg").renameTo(newFile)
                        paint.imageName=input
                        db.paintDao().update(paint)
                        runOnUiThread {
                            mAdapter.notifyItemChanged(images.indexOf(paint))
                            dialog.dismiss()
                            leaveEditMode()
                        }
                    }
                }
            }

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
                leaveEditMode()
            }

            dialog.show()
        }

        binding.btnDelete.setOnClickListener {
            val builder= AlertDialog.Builder(this)
            builder.setTitle("Delete paint")

            val nbPaints=images.count { it.isChecked }
            builder.setMessage("Are you sure you want to delete $nbPaints paint(s) ?")

            builder.setPositiveButton("Delete"){_,_->
                val toDelete=images.filter { it.isChecked }.toTypedArray()

                val toFile: Array<File> = toDelete.map { File("${it.imagePath}${it.imageName}.jpg") }.toTypedArray()
                var toFileSize=toFile.size

                GlobalScope.launch {
                    db.paintDao().delete(toDelete)

                    for (file in toFile){
                        try {
                            file.delete()
                            toFileSize--
                        }catch (e: Exception){
                            runOnUiThread {
                                Toast.makeText(this@EntranceActivity,"Error deleting ${file.name} paint ",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    if (toFileSize != 0){
                        runOnUiThread {
                            Toast.makeText(this@EntranceActivity,"An error occurred while deleting some paints",
                                Toast.LENGTH_SHORT).show()

                        }
                    }else{
                        runOnUiThread {
                            Toast.makeText(this@EntranceActivity,"All paints deleted successfully",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                    runOnUiThread {
                        images.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }
            }

            builder.setNegativeButton("Cancel"){_, _ ->
                //do not nothing
            }

            val dialog=builder.create()
            dialog.show()

        }
    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.editBar.visibility=View.GONE
        binding.bottomSheet.visibility=View.GONE
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED

        binding.btnAdd.visibility=View.VISIBLE

        allChecked=false
        images.map { it.isChecked=false }
        mAdapter.setEditMode(false)
    }

    private fun disableEdit(){
        binding.btnEdit.isClickable=false
        binding.btnEdit.backgroundTintList= ResourcesCompat.getColorStateList(resources,R.color.graydark,theme)
        binding.tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.graydark,theme))
    }
    private fun disableDelete(){
        binding.btnDelete.isClickable=false
        binding.btnDelete.backgroundTintList= ResourcesCompat.getColorStateList(resources,R.color.graydark,theme)
        binding.tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.graydark,theme))
    }
    private fun enableEdit(){
        binding.btnEdit.isClickable=true
        binding.btnEdit.backgroundTintList= ResourcesCompat.getColorStateList(resources,R.color.save,theme)
        binding.tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.save,theme))
    }
    private fun enableDelete(){
        binding.btnDelete.isClickable=true
        binding.btnDelete.backgroundTintList= ResourcesCompat.getColorStateList(resources,R.color.save,theme)
        binding.tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.save,theme))
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            images.clear()
            val queryResult=db.paintDao().searchDB("%$query%")
            images.addAll(queryResult)

            runOnUiThread{
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchAll(){
        GlobalScope.launch {
            images.clear()
            val queryResult=db.paintDao().getAll()
            images.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        val img=images[position]
        if (mAdapter.isEditMode()){
            images[position].isChecked = !images[position].isChecked
            mAdapter.notifyItemChanged(position)

            val nbSelected=images.count{it.isChecked}
            when(nbSelected){
                0->{
                    disableDelete()
                    disableEdit()
                }
                1->{
                    enableDelete()
                    enableEdit()
                }
                else->{
                    enableDelete()
                    disableEdit()
                }
            }
        }else{
            val intent=Intent(this,MainActivity::class.java)
            intent.putExtra("filePath",img.imagePath)
            intent.putExtra("fileName",img.imageName)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        images[position].isChecked= !images[position].isChecked
        mAdapter.notifyItemChanged(position)

        binding.bottomSheet.visibility=View.VISIBLE
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED

        if (mAdapter.isEditMode() && binding.editBar.visibility== View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            binding.editBar.visibility= View.VISIBLE
            binding.btnAdd.visibility=View.GONE

            enableDelete()
            enableEdit()
        }
    }
}