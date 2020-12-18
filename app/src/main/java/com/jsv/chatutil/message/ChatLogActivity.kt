package com.jsv.chatutil.message

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.jsv.chatutil.R
import com.jsv.chatutil.data.model.ChatMessage
import com.jsv.chatutil.view.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ChatLogActivity"
    }

    private val adapter = GroupAdapter<ViewHolder>()
    private var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chat_log.adapter = adapter

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username

        listenToData()
        send_button_chat_log.setOnClickListener {
            performSendMessage()
        }
    }

    private fun listenToData() {
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId   = user?.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        reference.addChildEventListener(object: ChildEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if(chatMessage != null) {

                    Log.d(TAG, "On onChildAdded --- ${chatMessage.text} and ${snapshot.value}")

                    if(chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessageActivity.currentUser ?:return
                        adapter.add(ChatFromItem(chatMessage.text, currentUser))

                    } else {
                        toUser?.let {
                            adapter.add(ChatToItem(chatMessage.text, it))
                        }
                    }
                }

                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
        })
    }

    private fun performSendMessage() {
        val text = edit_text_chat_log.text.toString()

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId   = user?.uid

        if(fromId == null) return

        //val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        //Type Safe
        val chatMessage = reference.key?.let {it->
            toId?.let { it1 -> ChatMessage(it, text, fromId, it1, System.currentTimeMillis() / 1000 ) }
        }

        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "On Success ${reference.key}")
                edit_text_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount -1)
            }
            .addOnFailureListener {
                Log.d(TAG, "On Fail ${it.message}")
            }

        toReference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "On Success ${reference.key}")
            }
            .addOnFailureListener {
                Log.d(TAG, "On Fail ${it.message}")
            }

        val latestReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestReference.setValue(chatMessage)

        val latestToReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestToReference.setValue(chatMessage)

    }

}


class ChatFromItem(val text: String, private val currentUser : User) : Item<ViewHolder>() {
    override fun getLayout(): Int {
      return R.layout.chat_from_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_from_row.text = text
        Picasso.get().load(currentUser.profileImageUrl).into(viewHolder.itemView.imageView_from_row)
    }
}

class ChatToItem(val text: String, private val user: User): Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_to_row.text = text
        Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.imageView_to_row)
    }
}