package com.bin.binstagram

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bin.binstagram.model.AlarmDTO
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.item_comment.view.*

class AlarmFragment : Fragment(){

    var alarmSnapshot : ListenerRegistration? = null
    var recyclerView: RecyclerView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var view =  inflater.inflate(R.layout.fragment_alarm,container,false)
        recyclerView = view.findViewById<RecyclerView>(R.id.alarmfragment_recyclerview)

        return view
    }

    override fun onResume() {
        super.onResume()
        recyclerView?.adapter = AlarmRecyclerViewAdapter()
        recyclerView?.layoutManager = LinearLayoutManager(activity)
    }

    override fun onStop() {
        super.onStop()
        alarmSnapshot?.remove()
        // - 스냅샷을 사용할 때는
        // ListenerRegistration at Resume at Stop
        // - Null 맨 상단
    }
    inner class AlarmRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        val alarmDTOlist = ArrayList<AlarmDTO>()

        init {
            var uid = FirebaseAuth.getInstance().currentUser!!.uid
            alarmSnapshot = FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid",uid).orderBy("timestamp").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null)   return@addSnapshotListener
                alarmDTOlist.clear()
                for(snapshot in querySnapshot.documents!!){
                    alarmDTOlist.add(snapshot.toObject(AlarmDTO::class.java))
                }

                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View?) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOlist.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            val profileImage = holder.itemView.commentviewItem_imageview_profile
            val commentTextView = holder.itemView.commentviewItem_textview_profile

            FirebaseFirestore.getInstance().collection("profileImages")?.document(alarmDTOlist[position].uid!!)?.get()?.addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    var url = task.result["image"]
                    Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(profileImage)
                }
            }

            when (alarmDTOlist[position].kind){

                0-> {
                    val str_0 = alarmDTOlist[position].userId + "님이 좋아요를 눌렀습니다."
                    commentTextView.text = str_0
                }
                1-> {
                    val str_1 = alarmDTOlist[position].userId +
                            "님이"+"\""+
                            alarmDTOlist[position].message + "\"" +
                            "메세지를 남겼습니다."
                    commentTextView.text = str_1
                }
                2-> {
                    val str_2 = alarmDTOlist[position].userId + "님이 당신의 계정을 팔로우하기 시작했습니다."
                    commentTextView.text = str_2
                }
            }
        }

    }
}