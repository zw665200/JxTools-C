package com.wx.tools.view.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wx.tools.R
import com.wx.tools.adapter.DataAdapter
import com.wx.tools.bean.FileStatus
import com.wx.tools.bean.FileWithType
import com.wx.tools.callback.FileWithTypeCallback
import com.wx.tools.callback.PicCallback
import com.wx.tools.controller.DBManager
import com.wx.tools.controller.PayManager
import com.wx.tools.controller.WxManager
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.ToastUtil
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.view.views.ExportFileDialog
import com.wx.tools.bean.FileBean
import com.wx.tools.callback.DialogCallback
import com.wx.tools.config.Constant
import com.wx.tools.view.views.QuitDialog
import kotlinx.android.synthetic.main.item_pic.view.*
import kotlinx.android.synthetic.main.layout_progressbar.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class WeChatPicsRecoveryActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var recovery: Button
    private lateinit var picRv: RecyclerView
    private lateinit var title: TextView
    private lateinit var mAdapter: DataAdapter<FileWithType>
    private var mainPics = mutableListOf<FileWithType>()
    private var sortMainPics = mutableListOf<FileWithType>()
    private var checkedPics = mutableListOf<FileWithType>()
    private var minSize = 0L
    private var maxSize = 100 * 1024 * 1024L
    private var minDate: Long = 0
    private var maxDate = System.currentTimeMillis()
    private var type = "wx"
    private lateinit var noData: ImageView
    private var prepared = false
    private var pay = false
    private var serviceId: Int = 0
    private lateinit var from: AppCompatSpinner
    private lateinit var size: AppCompatSpinner
    private lateinit var time: AppCompatSpinner
    private lateinit var sortView: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var searchStatus: TextView
    private lateinit var progressBarLayout: LinearLayout
    private lateinit var searchPause: ImageView
    private lateinit var delete: Button
    private lateinit var option: ImageView
    private var lastClickTime = 0L
    private var initSpinnerFrom = false
    private var initSpinnerSize = false
    private var initSpinnerTime = false
    private lateinit var desc: TextView
    private var chooseAll = false


    override fun setLayout(): Int {
        return R.layout.a_wechat_pics_recovery
    }

    override fun initView() {
        back = findViewById(R.id.iv_back)
        noData = findViewById(R.id.no_data)
        picRv = findViewById(R.id.rv_pics)
        recovery = findViewById(R.id.recovery)
        title = findViewById(R.id.wx_name)
        sortView = findViewById(R.id.ll_1)
        progressBar = findViewById(R.id.progress)
        searchStatus = findViewById(R.id.search_status)
        progressBarLayout = findViewById(R.id.ll_progressbar)
        searchPause = findViewById(R.id.pause)
        delete = findViewById(R.id.delete)
        option = findViewById(R.id.option)
        desc = findViewById(R.id.progress_des)

        back.setOnClickListener { onBackPressed() }
        searchPause.setOnClickListener { checkPause() }
        delete.setOnClickListener { deletePics() }
        option.setOnClickListener { checkOption() }

        recovery.setOnClickListener { nextStep(this) }

        from = findViewById(R.id.spinner_from)
        size = findViewById(R.id.spinner_size)
        time = findViewById(R.id.spinner_time)

        title.text = getString(R.string.pics_list_title)
        desc.text = getString(R.string.progress_des)

        loadPics()
        spinnerListener()

    }

    override fun onResume() {
        super.onResume()
        checkPay()
    }

    override fun initData() {
        serviceId = intent.getIntExtra("serviceId", 0)
        val callback = object : PicCallback {
            override fun onSuccess(step: Enum<FileStatus>) {
                launch(Dispatchers.Main) { searchFinish() }
            }

            override fun onProgress(step: Enum<FileStatus>, index: Int) {
            }

            override fun onProgress(step: Enum<FileStatus>, file: FileWithType) {
                launch(Dispatchers.Main) {
                    mainPics.add(file)
                    sortMainPics.add(file)

                    if (progressBar.progress >= 9000) {
                        progressBar.progress = 9000
                    } else {
                        progressBar.progress = progressBar.progress + 1
                    }

                    val tText = getString(R.string.pics_list_title) + "(${sortMainPics.size})"
                    title.text = tText
                    mAdapter.notifyItemInserted(mainPics.size - 1)
                }
            }

            override fun onFailed(step: Enum<FileStatus>, message: String) {
                launch(Dispatchers.Main) {
                    prepared = true
                    noData.visibility = View.VISIBLE
                    ToastUtil.showShort(this@WeChatPicsRecoveryActivity, message)
                }
            }
        }

        thread { WxManager.getInstance(this@WeChatPicsRecoveryActivity).getWxPics(this, callback) }
    }

    private fun spinnerListener() {

        from.setPopupBackgroundResource(R.color.color_white)
        size.setPopupBackgroundResource(R.color.color_white)
        time.setPopupBackgroundResource(R.color.color_white)

        from.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!initSpinnerFrom) {
                    initSpinnerFrom = true
                    return
                }
                when (position) {
                    0 -> {
                        type = "default"
                        sortAllSelectedList()
                    }

                    1 -> {
                        type = "dcim"
                        sortAllSelectedList()
                    }

                    2 -> {
                        type = "wechat"
                        sortAllSelectedList()
                    }

                    3 -> {
                        type = "qq"
                        sortAllSelectedList()
                    }

                    4 -> {
                        type = "soul"
                        sortAllSelectedList()
                    }

                    5 -> {
                        type = "momo"
                        sortAllSelectedList()
                    }

                    6 -> {
                        type = "game"
                        sortAllSelectedList()
                    }

                    7 -> {
                        type = "shop"
                        sortAllSelectedList()
                    }

                    8 -> {
                        type = "other"
                        sortAllSelectedList()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        size.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!initSpinnerSize) {
                    initSpinnerSize = true
                    return
                }
                when (position) {
                    0 -> {
                        minSize = 0
                        maxSize = 100 * 1024 * 1024L
                        sortAllSelectedList()
                    }

                    1 -> {
                        minSize = 0
                        maxSize = 100 * 1024L
                        sortAllSelectedList()
                    }

                    2 -> {
                        minSize = 100 * 1024L
                        maxSize = 1 * 1024 * 1024L
                        sortAllSelectedList()
                    }

                    3 -> {
                        minSize = 1 * 1024 * 1024L
                        maxSize = 100 * 1024 * 1024L
                        sortAllSelectedList()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        time.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!initSpinnerTime) {
                    initSpinnerTime = true
                    return
                }
                when (position) {
                    0 -> {
                        minDate = 0
                        maxDate = System.currentTimeMillis()
                        sortAllSelectedList()
                    }

                    1 -> {
                        minDate = System.currentTimeMillis() - 7 * 86400000L
                        maxDate = System.currentTimeMillis()
                        sortAllSelectedList()
                    }

                    2 -> {
                        minDate = System.currentTimeMillis() - 30 * 86400000L
                        maxDate = System.currentTimeMillis() - 7 * 86400000L
                        sortAllSelectedList()
                    }

                    3 -> {
                        minDate = 0
                        maxDate = System.currentTimeMillis() - 30 * 86400000L
                        sortAllSelectedList()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    /**
     * 加载图片
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun loadPics() {
        val width = AppUtil.getScreenWidth(this)
        mAdapter = DataAdapter.Builder<FileWithType>()
            .setData(mainPics)
            .setLayoutId(R.layout.item_pic)
            .addBindView { itemView, itemData ->
                val layoutParam = itemView.layoutParams
                layoutParam.width = width / 4
                layoutParam.height = width / 4 + 30
                itemView.layoutParams = layoutParam

                Glide.with(this).load(itemData.path).into(itemView.rv_pic)
                val size = itemData.size / 1024
                if (size > 1024) {
                    val d = size.toFloat() / 1024
                    val f = String.format("%.2f", d) + "MB"
                    itemView.pic_size.text = f
                } else {
                    val f = "${size}KB"
                    itemView.pic_size.text = f
                }

                if (itemData.check) {
                    itemView.pic_select.visibility = View.VISIBLE
                } else {
                    itemView.pic_select.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    //可见状态和选中状态改变
                    if (itemData.check) {
                        itemView.pic_select.visibility = View.GONE
                        itemData.check = false
                        checkedPics.remove(itemData)
                        if (checkedPics.isEmpty()) {
                            recovery.text = getString(R.string.pics_list_button)
                            delete.text = getString(R.string.list_delete_button)
                        } else {
                            val rText = getString(R.string.pics_list_button) + "(选中${checkedPics.size})"
                            val dText = getString(R.string.list_delete_button) + "(选中${checkedPics.size})"
                            recovery.text = rText
                            delete.text = dText
                        }
                    } else {
                        itemView.pic_select.visibility = View.VISIBLE
                        itemData.check = true
                        checkedPics.add(itemData)
                        val rText = getString(R.string.pics_list_button) + "(选中${checkedPics.size})"
                        val dText = getString(R.string.list_delete_button) + "(选中${checkedPics.size})"
                        recovery.text = rText
                        delete.text = dText
                    }
                }

                itemView.setOnLongClickListener {
                    toPicDetailPage(itemData)
                    return@setOnLongClickListener true
                }
            }
            .create()

        picRv.layoutManager = GridLayoutManager(this, 4)
        picRv.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun sortAllSelectedList() {
        if (!prepared && sortMainPics.isEmpty()) return
        launch(Dispatchers.IO) {
            sortMainPics = DBManager.getPicByKey(this@WeChatPicsRecoveryActivity, type, minSize, maxSize, minDate, maxDate)
            launch(Dispatchers.Main) {
                mainPics.clear()
                if (sortMainPics.size > 0) {
                    mainPics.addAll(sortMainPics)

                    val tText = getString(R.string.pics_list_title) + "(${sortMainPics.size})"
                    title.text = tText
                } else {
                    title.text = getString(R.string.pics_list_title)
                }
                mAdapter.notifyDataSetChanged()
            }
        }
    }


    private fun checkPay() {
        PayManager.getInstance().checkRecoveryPay(this, serviceId) {
            pay = it
        }
    }


    private fun toPayPage() {
        val intent = Intent()
        intent.setClass(this, PayActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        intent.putExtra("title", "微信图片恢复")
        startActivity(intent)
    }

    private fun toSinglePayPage() {
        val intent = Intent()
        intent.setClass(this, SinglePayActivity::class.java)
        intent.putExtra("serviceCode", Constant.DELETE)
        startActivity(intent)
    }

    private fun toPicDetailPage(itemData: FileWithType) {
        val intent = Intent()
        intent.setClass(this, PicDetailActivity::class.java)
        intent.putExtra("file", itemData)
        intent.putExtra("pay", pay)
        intent.putExtra("serviceId", serviceId)
        startActivity(intent)
    }


    private fun nextStep(context: Context) {
        if (checkedPics.isEmpty()) {
            ToastUtil.showShort(this, "未选中图片")
            return
        }

        if (lastClickTime == 0L) {
            lastClickTime = System.currentTimeMillis()
        } else {
            if (System.currentTimeMillis() - lastClickTime < 1000) {
                return
            }
        }
        lastClickTime = System.currentTimeMillis()

        if (pay) {

            ExportFileDialog(context, checkedPics, "recovery_pic").show()

        } else {
            toPayPage()
        }
    }

    private fun checkPause() {
        if (!prepared) {
            Constant.ScanStop = true
            searchFinish()
        } else {
            initProgressBar()
            initData()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initProgressBar() {
        prepared = false
        Constant.ScanStop = false
        sortView.visibility = View.GONE
        searchPause.setImageResource(R.drawable.pause)
        val tText = getString(R.string.pics_list_title)
        title.text = tText
        recovery.text = getString(R.string.pics_list_button)
        delete.text = getString(R.string.list_delete_button)

        progressBar.progress = 0
        searchStatus.text = getString(R.string.progress_status_searching)

        from.setSelection(0)
        size.setSelection(0)
        time.setSelection(0)
        minSize = 0L
        maxSize = 100 * 1024 * 1024L
        minDate = 0
        maxDate = System.currentTimeMillis()
        type = "default"

        mainPics.clear()
        sortMainPics.clear()
        checkedPics.clear()
        mAdapter.notifyDataSetChanged()
    }

    private fun searchFinish() {
        prepared = true
        progressBar.progress = progressBar.max
        searchStatus.text = getString(R.string.search_status_finish)
        searchPause.setImageResource(R.drawable.play)
        sortView.visibility = View.VISIBLE
    }

    private fun deletePics() {
        if (checkedPics.isEmpty()) {
            ToastUtil.showShort(this, "未选中图片")
            return
        }

        if (!pay) {
            toPayPage()
            return
        }

        launch(Dispatchers.IO) {
            val deleteList = arrayListOf<FileWithType>()
            deleteList.addAll(checkedPics)
            WxManager.getInstance(this@WeChatPicsRecoveryActivity).deleteFile(deleteList, object : FileWithTypeCallback {
                override fun onSuccess(step: Enum<FileStatus>) {
                    launch(Dispatchers.Main) {
                    }
                }

                override fun onProgress(step: Enum<FileStatus>, file: FileWithType) {
                    launch(Dispatchers.Main) {
                        notify(file)
                    }
                }

                override fun onFailed(step: Enum<FileStatus>, message: String) {
                    launch(Dispatchers.Main) {
                    }
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun checkOption() {
        if (!chooseAll) {
            chooseAll = true
            if (sortMainPics.isNotEmpty()) {
                checkedPics.clear()
                checkedPics.addAll(sortMainPics)
                val rText = getString(R.string.pics_list_button) + "(选中${checkedPics.size})"
                val dText = getString(R.string.list_delete_button) + "(选中${checkedPics.size})"
                recovery.text = rText
                delete.text = dText
                mainPics.clear()
                for (child in sortMainPics) {
                    mainPics.add(FileWithType(child.name, child.path, child.size, child.date, child.type, true))
                }
                mAdapter.notifyDataSetChanged()
            }
        } else {
            chooseAll = false
            if (sortMainPics.isNotEmpty()) {
                checkedPics.clear()
                recovery.text = getString(R.string.pics_list_button)
                delete.text = getString(R.string.list_delete_button)
                mainPics.clear()
                for (child in sortMainPics) {
                    mainPics.add(FileWithType(child.name, child.path, child.size, child.date, child.type, false))
                }
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun notify(file: FileWithType) {
        if (Constant.ScanStop) return

        mAdapter.notifyItemRemoved(mainPics.indexOf(file))
        mainPics.remove(file)
        sortMainPics.remove(file)
        checkedPics.remove(file)
        if (checkedPics.isEmpty()) {
            recovery.text = getString(R.string.pics_list_button)
            delete.text = getString(R.string.list_delete_button)
        } else {
            val rText = getString(R.string.pics_list_button) + "(选中${checkedPics.size})"
            val dText = getString(R.string.list_delete_button) + "(选中${checkedPics.size})"
            recovery.text = rText
            delete.text = dText
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0x105 && resultCode == 0x301) {
            if (data != null) {
                val file = data.getParcelableExtra<FileWithType>("file")
                if (file != null) {
                    notify(file)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (prepared) {
            finish()
        } else {
            QuitDialog(this, "正在扫描中，确定要退出吗？", object : DialogCallback {
                override fun onSuccess(file: FileBean) {
                    Constant.ScanStop = true
                    finish()
                }

                override fun onCancel() {
                }
            }).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainPics.clear()
        sortMainPics.clear()
        checkedPics.clear()
    }

}