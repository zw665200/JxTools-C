package com.wx.tools.view.activity

import android.content.Intent
import android.widget.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.wx.tools.R
import com.wx.tools.bean.FileStatus
import com.wx.tools.bean.FileWithType
import com.wx.tools.callback.FileWithTypeCallback
import com.wx.tools.controller.WxManager
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.JLog
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.view.views.ExportFileDialog
import kotlinx.coroutines.*


class VideoDetailActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var description: TextView
    private lateinit var video: PlayerView
    private lateinit var export: Button
    private lateinit var delete: Button
    private var file: FileWithType? = null
    private val mainList = arrayListOf<FileWithType>()
    private lateinit var videoFrameLayout: RelativeLayout
    private lateinit var player: SimpleExoPlayer


    override fun setLayout(): Int {
        return R.layout.video_detail
    }

    override fun initView() {
        back = findViewById(R.id.iv_back)
        video = findViewById(R.id.vv_video)
        description = findViewById(R.id.video_description)
        export = findViewById(R.id.recovery)
        delete = findViewById(R.id.delete)
        videoFrameLayout = findViewById(R.id.fl_video)

        back.setOnClickListener { finish() }
        export.setOnClickListener { nextStep() }
        delete.setOnClickListener { deleteVideos() }

        initializePlayer()
    }

    override fun initData() {
        file = intent.getParcelableExtra("file")
        if (file != null) {
            JLog.i("path = $file")
            mainList.add(file!!)

            val date = AppUtil.timeStamp2Date(file!!.date.toString(), null)
            val text = date + " / " + file!!.size / 1024 + "KB"
            description.text = text

            val mediaItem = MediaItem.fromUri(file!!.path)
            player.setMediaItem(mediaItem)
            player.prepare()

        }
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        video.player = player
    }


    private fun nextStep() {
        if (mainList.isEmpty()) return
        //pay success , do something
        ExportFileDialog(this, mainList, "recovery_video").show()
    }

    private fun deleteVideos() {
        if (mainList.isEmpty()) return

        launch(Dispatchers.IO) {
            WxManager.getInstance(this@VideoDetailActivity).deleteFile(mainList, object : FileWithTypeCallback {
                override fun onSuccess(step: Enum<FileStatus>) {
                    launch(Dispatchers.Main) {
                    }
                }

                override fun onProgress(step: Enum<FileStatus>, file: FileWithType) {
                    launch(Dispatchers.Main) {
                        val intent = Intent()
                        intent.putExtra("file", file)
                        setResult(0x301, intent)
                        finish()
                    }
                }

                override fun onFailed(step: Enum<FileStatus>, message: String) {
                    launch(Dispatchers.Main) {
                    }
                }
            })
        }
    }

//    private fun toSinglePayPage() {
//        val intent = Intent()
//        intent.setClass(this, SinglePayActivity::class.java)
//        intent.putExtra("serviceCode", Constant.DELETE)
//        startActivity(intent)
//    }


    override fun onDestroy() {
        player.pause()
        player.release()
        super.onDestroy()
    }
}