package com.tolgadirek.t_media.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.core.View
import com.squareup.picasso.Picasso
import com.tolgadirek.t_media.R
import com.tolgadirek.t_media.databinding.RecyclerRowBinding
import com.tolgadirek.t_media.model.Post

class PostAdapter(val postList : ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {
    class PostHolder(val binding : RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        holder.binding.recyclerKullaniciAdiText.text = postList[position].kullaniciAdi
        if (postList[position].yorum.isNotEmpty()){
            holder.binding.recyclerCommentText.text = postList[position].yorum } else{ holder.binding.recyclerCommentText.visibility = android.view.View.GONE }
        holder.binding.recyclerTarihText.text = postList[position].tarih
        if (postList[position].gonderiUrl.isNotEmpty()) { Picasso.get().load(postList[position].gonderiUrl).into(holder.binding.recyclerImageView) } else {
            holder.binding.recyclerImageView.visibility = android.view.View.GONE}
        Picasso.get().load(postList[position].ppUrl).into(holder.binding.recyclerProfilFotografi)
    }
}