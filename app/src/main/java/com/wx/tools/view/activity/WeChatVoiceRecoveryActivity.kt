package com.wx.tools.view.activity

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wx.tools.R
import com.wx.tools.adapter.DataAdapter
import com.wx.tools.bean.FileBean
import com.wx.tools.bean.FileStatus
import com.wx.tools.bean.FileWithType
import com.wx.tools.callback.DialogCallback
import com.wx.tools.callback.FileWithTypeCallback
import com.wx.tools.callback.VoiceCallback
import com.wx.tools.config.Constant
import com.wx.tools.controller.DBManager
import com.wx.tools.controller.PayManager
import com.wx.tools.controller.WxManager
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.ToastUtil
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.view.views.ExportFileDialog
import com.wx.tools.view.views.QuitDialog
import kotlinx.android.synthetic.main.a_wechat_video_recovery.*
import kotlinx.android.synthetic.main.item_pic.view.*
import kotlinx.android.synthetic.main.item_voice.view.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class WeChatVoiceRecoveryActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var picRv: RecyclerView
    private lateinit var mAdapter: DataAdapter<FileWithType>
    private var mainVoices = mutableListOf<FileWithType>()
    private var sortVoices = mutableListOf<FileWithType>()
    private var checkedVoices = mutableListOf<FileWithType>()
    private lateinit var from: AppCompatSpinner
    private lateinit var size: AppCompatSpinner
    private lateinit var time: AppCompatSpinner
    private lateinit var noData: ImageView
    private var prepared = false
    private var pay = false
    private var serviceId: Int = 0
    private lateinit var title: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchStatus: TextView
    private lateinit var progressBarLayout: LinearLayout
    private lateinit var searchPause: ImageView
    private lateinit var recovery: Button
    private lateinit var delete: Button
    private lateinit var option: ImageView
    private lateinit var sortView: LinearLayout
    private lateinit var desc: TextView
    private var type = "default"
    private var minSize: Long = 0L
    private var maxSize: Long = 1024 * 1024 * 1024L
    private var minDate: Long = 0L
    private var maxDate = System.currentTimeMillis()
    private var initSpinnerSort = false
    private var initSpinnerSize = false
    private var initSpinnerTime = false
    private var chooseAll = false

    override fun setLayout(): Int {
        return R.layout.a_wechat_voice_recovery
    }

    override fun initView() {
        back = findViewById(R.id.iv_back)
        noData = findViewById(R.id.no_data)
        picRv = findViewById(R.id.rv_voice)
        title = findViewById(R.id.wx_name)
        progressBar = findViewById(R.id.progress)
        searchStatus = findViewById(R.id.search_status)
        progressBarLayout = findViewById(R.id.ll_progressbar)
        sortView = findViewById(R.id.ll_1)
        searchPause = findViewById(R.id.pause)
        recovery = findViewById(R.id.recovery)
        delete = findViewById(R.id.delete)
        option = findViewById(R.id.option)
        desc = findViewById(R.id.progress_des)
        from = findViewById(R.id.spinner_from)
        size = findViewById(R.id.spinner_size)
        time = findViewById(R.id.spinner_time)

        back.setOnClickListener { onBackPressed() }
        searchPause.setOnClickListener { checkPause() }
        option.setOnClickListener { checkOption() }
        recovery.setOnClickListener { nextStep(this) }
        delete.setOnClickListener { deletePics() }

        title.text = getString(R.string.voice_list_title)
        desc.text = getString(R.string.progress_des)

        loadVoices()
        spinnerListener()
    }

    override fun initData() {
        serviceId = intent.getIntExtra("serviceId", 0)
        searchVoices()
    }

    override fun onResume() {
        super.onResume()
        checkPay()
    }

    private fun loadVoices() {
        val width = AppUtil.getScreenWidth(this)
        mAdapter = DataAdapter.Builder<FileWithType>()
            .setData(mainVoices)
            .setLayoutId(R.layout.item_voice)
            .addBindView { itemView, itemData ->
                val layoutParam = itemView.layoutParams
                layoutParam.width = width / 4
                layoutParam.height = width / 4
                itemView.layoutParams = layoutParam

                if (itemData.check) {
                    itemView.voice_select.visibility = View.VISIBLE
                } else {
                    itemView.voice_select.visibility = View.GONE
                }

                val date = AppUtil.timeStamp2Date(itemData.date.toString(), "yyyy-MM-dd")
                itemView.voice_description.text = date

                itemView.setOnClickListener {
                    //可见状态和选中状态改变
                    if (itemData.check) {
                        itemView.voice_select.visibility = View.GONE
                        itemData.check = false
                        checkedVoices.remove(itemData)
                        if (checkedVoices.isEmpty()) {
                            recovery.text = getString(R.string.pics_list_button)
                            delete.text = getString(R.string.list_delete_button)
                        } else {
                            recovery.text = getString(R.string.pics_list_button) + "(选中${checkedVoices.size})"
                            delete.text = getString(R.string.list_delete_button) + "(选中${checkedVoices.size})"
                        }
                    } else {
                        itemView.voice_select.visibility = View.VISIBLE
                        itemData.check = true
                        checkedVoices.add(itemData)
                        recovery.text = getString(R.string.pics_list_button) + "(选中${checkedVoices.size})"
                        delete.text = getString(R.string.list_delete_button) + "(选中${checkedVoices.size})"
                    }
                }

                itemView.setOnLongClickListener {
                    toVoiceDetailPage(itemData)
                    return@setOnLongClickListener true
                }
            }
            .create()

        picRv.layoutManager = GridLayoutManager(this, 4)
        picRv.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
    }

    private fun spinnerListener() {
        from.setPopupBackgroundResource(R.color.color_white)
        size.setPopupBackgroundResource(R.color.color_white)
        time.setPopupBackgroundResource(R.color.color_white)

        from.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!initSpinnerSort) {
                    initSpinnerSort = true
                    return
                }

                when (position) {
                    0 -> {
                        type = "default"
                        sortAllSelectedList()
                    }

                    1 -> {
                        type = "date_desc"
                        sortAllSelectedList()
                    }

                    2 -> {
                        type = "date_asc"
                        sortAllSelectedList()
                    }

                    3 -> {
                        type = "size_desc"
                        sortAllSelectedList()
                    }

                    4 -> {
                        type = "size_asc"
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
                        maxSize = 1024 * 1024 * 1024L
                        sortAllSelectedList()
                    }

                    1 -> {
                        minSize = 0
                        maxSize = 10 * 1024L
                        sortAllSelectedList()
                    }

                    2 -> {
                        minSize = 10 * 1024L
                        maxSize = 1024 * 1024 * 1024L
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

    private fun sortAllSelectedList() {
        if (!prepared && sortVoices.isEmpty()) return
        launch(Dispatchers.IO) {
            sortVoices = DBManager.getVoiceByKey(this@WeChatVoiceRecoveryActivity, type, minSize, maxSize, minDate, maxDate)
            launch(Dispatchers.Main) {
                mainVoices.clear()
                if (sortVoices.size > 0) {
                    mainVoices.addAll(sortVoices)

                    val tText = getString(R.string.voice_list_title) + "(${sortVoices.size})"
                    title.text = tText
                } else {
                    title.text = getString(R.string.voice_list_title)
                }
                mAdapter.notifyItemRangeChanged(0, mainVoices.size)
            }
        }
    }

    private fun searchVoices() {

        val callback = object : VoiceCallback {
            override fun onSuccess(step: Enum<FileStatus>) {
                launch(Dispatchers.Main) {
                    searchFinish()
                }
            }

            override fun onProgress(step: Enum<FileStatus>, index: Int) {

            }

            override fun onProgress(step: Enum<FileStatus>, file: FileWithType) {
                launch(Dispatchers.Main) {
                    mainVoices.add(file)
                    sortVoices.add(file)

                    //如果没有按下了暂停键，继续刷新界面
                    if (progressBar.progress >= 9000) {
                        progressBar.progress = 9000
                    } else {
                        progressBar.progress = progressBar.progress + 1
                    }

                    mAdapter.notifyItemInserted(mainVoices.size - 1)
                    title.text = getString(R.string.voice_list_title) + "(${sortVoices.size})"
                }
            }

            override fun onFailed(step: Enum<FileStatus>, message: String) {
                launch(Dispatchers.Main) {
                    prepared = true
                    progressBarLayout.visibility = View.GONE
                    noData.visibility = View.VISIBLE
                    ToastUtil.showShort(this@WeChatVoiceRecoveryActivity, message)
                }
            }
        }

        thread { WxManager.getInstance(this@WeChatVoiceRecoveryActivity).getWxVoices(this, callback) }
    }

    private fun checkPay() {
        PayManager.getInstance().checkRecoveryPay(this, serviceId) {
            pay = it
        }
    }

    private fun nextStep(context: Context) {
        if (checkedVoices.isEmpty()) {
            ToastUtil.showShort(context, "未选中语音")
            return
        }

        if (pay) {
            //pay success , do something
            ExportFileDialog(context, checkedVoices, "export_voice").show()
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

    private fun initProgressBar() {
        prepared = false
        Constant.ScanStop = false
        sortView.visibility = View.GONE
        searchPause.setImageResource(R.drawable.pause)
        val tText = getString(R.string.voice_list_title)
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

        mainVoices.clear()
        sortVoices.clear()
        checkedVoices.clear()
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
        if (checkedVoices.isEmpty()) {
            ToastUtil.showShort(this, "未选中语音")
            return
        }

        if (!pay) {
            toPayPage()
            return
        }

        launch(Dispatchers.IO) {
            val deleteList = arrayListOf<FileWithType>()
            deleteList.addAll(checkedVoices)
            WxManager.getInstance(this@WeChatVoiceRecoveryActivity).deleteFile(deleteList, object : FileWithTypeCallback {
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

    private fun checkOption() {
        if (!chooseAll) {
            chooseAll = true
            if (sortVoices.isNotEmpty()) {
                checkedVoices.clear()
                checkedVoices.addAll(sortVoices)
                recovery.text = getString(R.string.pics_list_button) + "(选中${checkedVoices.size})"
                delete.text = getString(R.string.list_delete_button) + "(选中${checkedVoices.size})"
                mainVoices.clear()
                for (child in sortVoices) {
                    mainVoices.add(FileWithType(child.name, child.path, child.size, child.date, child.type, true))
                }
                mAdapter.notifyDataSetChanged()
            }
        } else {
            chooseAll = false
            if (sortVoices.isNotEmpty()) {
                checkedVoices.clear()
                recovery.text = getString(R.string.pics_list_button)
                delete.text = getString(R.string.list_delete_button)
                mainVoices.clear()
                for (child in sortVoices) {
                    mainVoices.add(FileWithType(child.name, child.path, child.size, child.date, child.type, false))
                }
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun toPayPage() {
        val intent = Intent()
        intent.setClass(this, PayActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        intent.putExtra("title", "微信语音恢复")
        startActivity(intent)
    }

    private fun toSinglePayPage() {
        val intent = Intent()
        intent.setClass(this, SinglePayActivity::class.java)
        intent.putExtra("serviceCode", Constant.DELETE)
        startActivity(intent)
    }

    private fun toVoiceDetailPage(itemData: FileWithType) {
        if (pay) {
            val intent = Intent()
            intent.setClass(this, VoiceDetailActivity::class.java)
            intent.putExtra("file", itemData)
            startActivityForResult(intent, 0x106)
        } else {
            toPayPage()
        }
    }

    private fun notify(file: FileWithType) {
        if (Constant.ScanStop) return

        mAdapter.notifyItemRemoved(mainVoices.indexOf(file))
        mainVoices.remove(file)
        sortVoices.remove(file)
        checkedVoices.remove(file)
        if (checkedVoices.isEmpty()) {
            recovery.text = getString(R.string.voice_list_button)
            delete.text = getString(R.string.list_delete_button)
        } else {
            recovery.text = getString(R.string.voice_list_button) + "(选中${checkedVoices.size})"
            delete.text = getString(R.string.list_delete_button) + "(选中${checkedVoices.size})"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0x106 && resultCode == 0x301) {
            //delete success
            if (data != null) {
                val file = data.getParcelableExtra<FileWithType>("file")
                if (file != null) {
                    notify(file)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainVoices.clear()
        sortVoices.clear()
        checkedVoices.clear()
    }

}