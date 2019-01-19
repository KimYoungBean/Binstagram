package com.bin.binstagram

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bin.binstagram.model.AlarmDTO
import com.bin.binstagram.model.ContentDTO
import com.bin.binstagram.model.FollowDTO
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){

    var fragmentView : View? = null
    var PICK_PROFILE_FROM_ALBUM = 10
    var firestore : FirebaseFirestore? = null
    // 현재 나의 uid
    var currentUserUid : String? = null
    // 내가 선택한 uid
    var uid : String? = null

    var auth : FirebaseAuth?= null
    var fcmPush : FcmPush? = null

    var followListenerRegistration: ListenerRegistration? = null
    var followinglistenerRegistration : ListenerRegistration? = null
    var imageprofileListenerRegistration : ListenerRegistration? = null
    var recyclerListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        firestore = FirebaseFirestore.getInstance()
        fragmentView = inflater.inflate(R.layout.fragment_user,container,false)
        fcmPush = FcmPush()
        auth = FirebaseAuth.getInstance()

        if(arguments != null){

            uid = arguments!!.getString("destinationUid")

            if(uid != null && uid == currentUserUid){
                // 나의 유저페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth?.signOut()
                }

            }else{
                // 제 3자의 유저페이지
                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility - View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE
                mainActivity.toolbar_username.text = arguments!!.getString("userId")
                mainActivity.toolbar_btn_back.setOnClickListener {
                    mainActivity.bottom_navigation.selectedItemId = R.id.action_home
                }
                fragmentView?.account_btn_follow_signout?.setOnClickListener {
                    requestFollow()
                }
            }
        }




        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }



        return fragmentView
    }

    fun requestFollow(){
        var tsDocFollowiung = firestore!!.collection("users").document(currentUserUid!!)

        firestore?.runTransaction {
            transaction ->
            var followDTO = transaction.get(tsDocFollowiung).toObject(FollowDTO::class.java)
            // 내 아이디가 아무도 팔로잉 하지 않은 경우
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowiung, followDTO)
                return@runTransaction
            }
            // 내 아이디가 제 3자를 이미 팔로잉 하고 있는 경우 -> 팔로우 취소한다
            if(followDTO.followings.containsKey(uid)){
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            }
            // 내 아이디가 제 3자를 팔로잉 하지 않았을 경우 -> 팔로우한다
            else{
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowiung, followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)

            // 내 아이디가 아무도 팔로잉 하지 않은 경우
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO)
                return@runTransaction
            }
            // 내 아이디가 제 3자를 이미 팔로잉 하고 있는 경우 -> 팔로우 취소한다
            if(followDTO.followers.containsKey(currentUserUid!!)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)

            }
            // 내 아이디가 제 3자를 팔로잉 하지 않았을 경우 - > 팔로우한다
            else{
                followDTO.followerCount = followDTO.followerCount + 1
                followDTO.followers[currentUserUid!!] = true
                followerAlarm(uid)
            }

            transaction.set(tsDocFollowiung, followDTO)
            return@runTransaction
        }
    }

    fun getProfileImages(){
        imageprofileListenerRegistration = firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null)    return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity).load(url).apply(RequestOptions().circleCrop()).into(fragmentView!!.account_iv_profile)
            }
        }

    }

    fun followerAlarm(destinationUid : String?){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser!!.email
        alarmDTO.uid = auth?.currentUser!!.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        fcmPush?.sendMessage(destinationUid!!, "알림 메세지 입니다.", message)
    }

    override fun onResume() {
        super.onResume()
        getProfileImages()
        getFollowing()
        getFollower()
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity, 3)
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration?.remove()
        followinglistenerRegistration?.remove()
        imageprofileListenerRegistration?.remove()
        recyclerListenerRegistration?.remove()
    }

    fun getFollower(){
        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null)    return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount.toString()
            if(followDTO?.followers?.containsKey(currentUserUid)!!) {
                fragmentView?.account_btn_follow_signout?.text = "팔로우 취소"
                fragmentView?.account_btn_follow_signout
                        ?.background
                        ?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
            } else {
                if(uid != currentUserUid) {
                    fragmentView?.account_btn_follow_signout?.text = "팔로우"
                    fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                }
            }

        }
    }

    fun getFollowing(){
        followinglistenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null)    return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            fragmentView?.account_tv_following_count?.text = followDTO?.followingCount.toString()
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO>

        init {
            contentDTOs = ArrayList()
            recyclerListenerRegistration = firestore?.collection("images")?.whereEqualTo("uid", currentUserUid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null)   return@addSnapshotListener
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java))
                }
                account_tv_post_count.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels/3

            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        private inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

    }

}