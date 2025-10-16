package com.example.firstapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import Post
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class PostsAdapter(private val postList: List<Post>) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Ensure R.id.post_image_view is the correct ID in your list_item_post.xml
        val postImageView: ImageView = view.findViewById(R.id.post_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        // CRITICAL: Use the correct field name here
        val base64 = post.base64Image
        val context = holder.postImageView.context

        if (!base64.isNullOrEmpty()) {
            try {
                // 1. Decode Base64 string back into a byte array (using NO_WRAP)
                val decodedString: ByteArray = Base64.decode(base64, Base64.NO_WRAP)

                // 2. Convert the byte array into a Bitmap
                val decodedBitmap: Bitmap? = BitmapFactory.decodeByteArray(
                    decodedString, 0, decodedString.size
                )

                if (decodedBitmap != null) {
                    // 3. Load the *Bitmap* object directly using Glide
                    Glide.with(context)
                        .load(decodedBitmap) // Load the actual Bitmap object
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        // Add placeholder/error drawables to see if loading fails gracefully
                        .placeholder(R.drawable.loading_placeholder)
                        .error(R.drawable.error_placeholder)
                        .into(holder.postImageView)
                } else {
                    Log.e("PostsAdapter", "BitmapFactory failed to decode Base64 data.")
                    holder.postImageView.setImageResource(R.drawable.error_placeholder)
                }

            } catch (e: IllegalArgumentException) {
                Log.e("PostsAdapter", "Invalid Base64 string: ${e.message}")
                holder.postImageView.setImageResource(R.drawable.error_placeholder)
            } catch (e: Exception) {
                Log.e("PostsAdapter", "Error decoding or loading image: ${e.message}")
            }
        } else {
            // Data is null/empty, apply background/placeholder
            holder.postImageView.setBackgroundColor(context.getColor(R.color.LightGray))
        }
    }

    override fun getItemCount(): Int = postList.size
}