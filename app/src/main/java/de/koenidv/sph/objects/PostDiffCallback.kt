package de.koenidv.sph.objects

import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil

//  Created by koenidv on 29.12.2020.
class PostDiffCallback(private val newPosts: List<Post>, private val oldPosts: List<Post>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldPosts.size
    }

    override fun getNewListSize(): Int {
        return newPosts.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldPosts[oldItemPosition] == newPosts[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldPosts[oldItemPosition] == newPosts[newItemPosition]
    }

    @Nullable
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        //you can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }

}