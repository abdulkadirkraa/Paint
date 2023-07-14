package com.abdulkadirkara.paint

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class Adapter(var images: ArrayList<Paint>,var listener: OnItemClickListener): RecyclerView.Adapter<Adapter.ViewHolder>() {

    private var editMode=false

    fun isEditMode(): Boolean{return editMode}

    @SuppressLint("NotifyDataSetChanged")
    fun setEditMode(mode: Boolean){
        if (editMode != mode){
            editMode=mode
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(itemview: View): RecyclerView.ViewHolder(itemview), View.OnClickListener,
        View.OnLongClickListener{
        var tvfilename=itemview.findViewById<TextView>(R.id.textviewFilename)
        var imagefilepath=itemview.findViewById<ImageView>(R.id.imageFilepath)
        var checkBox=itemview.findViewById<CheckBox>(R.id.checkbox)

        init {
            itemview.setOnClickListener(this)
            itemview.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val position=adapterPosition
            if (position != RecyclerView.NO_POSITION)
                listener.onItemClickListener(position)
        }

        override fun onLongClick(v: View?): Boolean {
            val position=adapterPosition
            if (position != RecyclerView.NO_POSITION)
                listener.onItemLongClickListener(position)

            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view= LayoutInflater.from(parent.context).inflate(R.layout.itemview_layout,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {

            val imgFile= File("${images[position].imagePath}${images[position].imageName}.jpg")
            if (imgFile.exists()){

                val myBitmap= BitmapFactory.decodeFile(imgFile.absolutePath)
                holder.imagefilepath.setImageBitmap(myBitmap)
                holder.tvfilename.text=imgFile.name

                if (editMode){
                    holder.checkBox.visibility= View.VISIBLE
                    holder.checkBox.isChecked=images[position].isChecked
                }else{
                    holder.checkBox.visibility= View.GONE
                    holder.checkBox.isChecked=false
                }
            }
        }
    }
}