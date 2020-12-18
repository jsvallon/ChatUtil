package com.jsv.chatutil.view

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jsv.chatutil.R
import com.jsv.chatutil.data.model.ChatMessage
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessageRow(val chatMessage: ChatMessage):  Item<ViewHolder>() {
    var chartPartnerUser: User? = null

    override fun getLayout(): Int {
        return R.layout.latest_message_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {

        val chatPartNerId: String = if(chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            chatMessage.toId
        } else {
            chatMessage.fromId
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartNerId")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) = Unit
            override fun onDataChange(snapshot: DataSnapshot) {
                chartPartnerUser = snapshot.getValue(User::class.java)
                viewHolder.itemView.latest_username.text = chartPartnerUser?.username
                Picasso.get().load(chartPartnerUser?.profileImageUrl).into(viewHolder.itemView.latest_message_imageView)
            }

        })
        viewHolder.itemView.latest_message.text = chatMessage.text
    }
}