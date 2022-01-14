package com.wx.tools.view.activity

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
import com.wx.tools.bean.FileBean
import com.wx.tools.bean.FileStatus
import com.wx.tools.bean.FileWithType
import com.wx.tools.callback.DialogCallback
import com.wx.tools.callback.DocCallback
import com.wx.tools.callback.FileWithTypeCallback
import com.wx.tools.config.Constant
import com.wx.tools.controller.DBManager
import com.wx.tools.controller.PayManager
import com.wx.tools.controller.WxManager
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.ToastUtil
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.view.views.ExportFileDialog
import com.wx.tools.view.views.QuitDialog
import kotlinx.android.synthetic.main.item_doc.view.*
import kotlinx.android.synthetic.main.item_pic.view.*
import kotlinx.android.synthetic.main.item_voice.view.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class WeChatDocRecoveryActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var picRv: RecyclerView
    private lateinit var mAdapter: DataAdapter<FileWithType>
    private var mainDocs = mutableListOf<FileWithType>()
    private var sortDocs = mutableListOf<FileWithType>()
    private var checkedDocs = mutableListOf<FileWithType>()
    private lateinit var noData: ImageView
    private var prepared = false
    private var pay = false
    private var serviceId: Int = 0
    private lateinit var title: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchStatus: TextView
    private lateinit var sortView: LinearLayout
    private lateinit var searchPause: ImageView
    private lateinit var recovery: Button
    private lateinit var delete: Button
    private lateinit var option: ImageView
    private lateinit var progressDes: TextView
    private lateinit var sort: AppCompatSpinner
    private lateinit var size: AppCompatSpinner
    private lateinit var time: AppCompatSpinner
    private var minSize = 0L
    private var maxSize = 100 * 1024 * 1024L
    private var minDate: Long = 0
    private var maxDate = System.currentTimeMillis()
    private var type = "default"
    private var initSpinnerSort = false
    private var initSpinnerSize = false
    private var initSpinnerTime = false
    private var chooseAll = false

    override fun setLayout(): Int {
        return R.layout.a_wechat_doc_recovery
    }

    override fun initView() {
        back = findViewById(R.id.iv_back)
        noData = findViewById(R.id.no_data)
        picRv = findViewById(R.id.rv_voice)
        title = findViewById(R.id.wx_name)
        progressBar = findViewById(R.id.progress)
        searchStatus = findViewById(R.id.search_status)
        sortView = findViewById(R.id.ll_1)
        searchPause = findViewById(R.id.pause)
        recovery = findViewById(R.id.recovery)
        delete = findViewById(R.id.delete)
        option = findViewById(R.id.option)
        progressDes = findViewById(R.id.progress_des)

        sort = findViewById(R.id.spinner_from)
        size = findViewById(R.id.spinner_size)
        time = findViewById(R.id.spinner_time)

        back.setOnClickListener { onBackPressed() }
        searchPause.setOnClickListener { checkPause() }
        option.setOnClickListener { checkOption() }
        recovery.setOnClickListener { nextStep(this) }
        delete.setOnClickListener { deleteDocs() }

        title.text = getString(R.string.doc_list_title)
        progressDes.text = getString(R.string.progress_des)

        loadDocs()
        spinnerListener()

    }

    override fun initData() {
        serviceId = intent.getIntExtra("serviceId", 0)
        searchDocs()
    }

    override fun onResume() {
        super.onResume()
        checkPay()
    }

    private fun spinnerListener() {

        sort.setPopupBackgroundResource(R.color.color_white)
        size.setPopupBackgroundResource(R.color.color_white)
        time.setPopupBackgroundResource(R.color.color_white)

        sort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                        minSize = 0L
                        maxSize = 10 * 1024 * 1024 * 1024L
                        sortAllSelectedList()
                    }

                    1 -> {
                        minSize = 0L
                        maxSize = 1024 * 1024L
                        sortAllSelectedList()
                    }

                    2 -> {
                        minSize = 1024 * 1024L
                        maxSize = 10 * 1024 * 1024 * 1024L
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

    private fun searchDocs() {

        val callback = object : DocCallback {
            override fun onSuccess(step: Enum<FileStatus>) {
                launch(Dispatchers.Main) {
                    searchFinish()
                }
            }

            override fun onProgress(step: Enum<FileStatus>, index: Int) {

            }

            override fun onProgress(step: Enum<FileStatus>, file: FileWithType) {
                launch(Dispatchers.Main) {
                    mainDocs.add(file)
                    sortDocs.add(file)

                    if (progressBar.progress >= 9000) {
                        progressBar.progress = 9000
                    } else {
                        progressBar.progress = progressBar.progress + 1
                    }
                    mAdapter.notifyDataSetChanged()
                    title.text = getString(R.string.doc_list_title) + "(${sortDocs.size})"
                }
            }

            override fun onFailed(step: Enum<FileStatus>, message: String) {
                launch(Dispatchers.Main) {
                    prepared = true
                    progressBar.progress = progressBar.max
                    noData.visibility = View.VISIBLE
                    searchStatus.text = getString(R.string.search_status_finish)
                    searchPause.setImageResource(R.drawable.play)
                    ToastUtil.showShort(this@WeChatDocRecoveryActivity, message)
                }
            }
        }

        thread {
            WxManager.getInstance(this@WeChatDocRecoveryActivity).getWxDocs(this, callback)
        }
    }

    private fun loadDocs() {
        val width = AppUtil.getScreenWidth(this)
        mAdapter = DataAdapter.Builder<FileWithType>()
            .setData(mainDocs)
            .setLayoutId(R.layout.item_doc)
            .addBindView { itemView, itemData ->
                val layoutParam = itemView.layoutParams
                layoutParam.width = width / 4
                layoutParam.height = width / 4
                itemView.layoutParams = layoutParam

                itemView.doc_title.text = itemData.name

                if (itemData.check) {
                    itemView.doc_select.visibility = View.VISIBLE
                } else {
                    itemView.doc_select.visibility = View.GONE
                }

                itemView.setOnLongClickListener {
                    if (pay) {
                        toDocDetailPage(itemData)
                    } else {
                        toPayPage()
                    }

                    true
                }

                itemView.setOnClickListener {
                    if (!itemData.check) {
                        itemView.doc_select.visibility = View.VISIBLE
                        checkedDocs.add(itemData)
                        itemData.check = true
                        val rText = getString(R.string.pics_list_button) + "(选中${checkedDocs.size})"
                        val dText = getString(R.string.list_delete_button) + "(选中${checkedDocs.size})"
                        recovery.text = rText
                        delete.text = dText
                    } else {
                        itemView.doc_select.visibility = View.GONE
                        checkedDocs.remove(itemData)
                        itemData.check = false
                        if (checkedDocs.isEmpty()) {
                            recovery.text = getString(R.string.pics_list_button)
                            delete.text = getString(R.string.list_delete_button)
                        } else {
                            val rText = getString(R.string.pics_list_button) + "(选中${checkedDocs.size})"
                            val dText = getString(R.string.list_delete_button) + "(选中${checkedDocs.size})"
                            recovery.text = rText
                            delete.text = dText
                        }
                    }

                }

                if (itemData.name.endsWith(".doc") || itemData.name.endsWith(".docx")) {
                    Glide.with(this).load(R.drawable.file_word).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".txt")) {
                    Glide.with(this).load(R.drawable.file_txt).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".ppt") || itemData.name.endsWith(".pptx")) {
                    Glide.with(this).load(R.drawable.file_ppt).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".xls") || itemData.name.endsWith(".xlsx") || itemData.name.endsWith(".csv")) {
                    Glide.with(this).load(R.drawable.file_excel).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".pdf")) {
                    Glide.with(this).load(R.drawable.file_pdf).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".html") || itemData.name.endsWith(".xhtml")) {
                    Glide.with(this).load(R.drawable.file_html).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".ai")) {
                    Glide.with(this).load(R.drawable.file_ai).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".cad")) {
                    Glide.with(this).load(R.drawable.file_cad).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".psd")) {
                    Glide.with(this).load(R.drawable.file_ps).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".java") || itemData.name.endsWith(".php") || itemData.name.endsWith(".exe")
                    || itemData.name.endsWith(".python") || itemData.name.endsWith(".class") || itemData.name.endsWith(".config")
                    || itemData.name.endsWith(".json") || itemData.name.endsWith(".xml") || itemData.name.endsWith(".kt")
                ) {
                    Glide.with(this).load(R.drawable.file_html).into(itemView.doc_icon)
                    return@addBindView
                }

                if (itemData.name.endsWith(".zip") || itemData.name.endsWith(".rar") || itemData.name.endsWith(".tar")) {
                    Glide.with(this).load(R.drawable.file_rar).into(itemView.doc_icon)
                    return@addBindView
                } else {
                    Glide.with(this).load(R.drawable.file_default).into(itemView.doc_icon)
                }


            }
            .create()

        picRv.layoutManager = GridLayoutManager(this, 4)
        picRv.adapter = mAdapter
    }

    private fun sortAllSelectedList() {
        if (!prepared && sortDocs.isEmpty()) return
        launch(Dispatchers.IO) {
            sortDocs = DBManager.getDocByKey(this@WeChatDocRecoveryActivity, type, minSize, maxSize, minDate, maxDate)
            launch(Dispatchers.Main) {
                mainDocs.clear()
                if (sortDocs.size > 0) {
                    mainDocs.addAll(sortDocs)

                    val tText = getString(R.string.doc_list_title) + "(${sortDocs.size})"
                    title.text = tText
                } else {
                    title.text = getString(R.string.doc_list_title)
                }

                mAdapter.notifyItemRangeChanged(0, mainDocs.size)
            }
        }
    }

    private fun nextStep(context: Context) {
        if (checkedDocs.isEmpty()) {
            ToastUtil.show(this, "未选择文档")
            return
        }

        if (pay) {

            //pay success , do something
            ExportFileDialog(context, checkedDocs, "export_doc").show()

        } else {
            toPayPage()
        }
    }

    private fun checkPay() {
        PayManager.getInstance().checkRecoveryPay(this, serviceId) {
            pay = it
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
        val tText = getString(R.string.doc_list_title)
        title.text = tText
        recovery.text = getString(R.string.pics_list_button)
        delete.text = getString(R.string.list_delete_button)

        progressBar.progress = 0
        searchStatus.text = getString(R.string.progress_status_searching)

        sort.setSelection(0)
        size.setSelection(0)
        time.setSelection(0)
        minSize = 0L
        maxSize = 100 * 1024 * 1024L
        minDate = 0
        maxDate = System.currentTimeMillis()
        type = "default"

        mAdapter.notifyItemRangeRemoved(0, mainDocs.size)

        mainDocs.clear()
        sortDocs.clear()
        checkedDocs.clear()

    }

    private fun searchFinish() {
        prepared = true
        progressBar.progress = progressBar.max
        searchStatus.text = getString(R.string.search_status_finish)
        searchPause.setImageResource(R.drawable.play)
        sortView.visibility = View.VISIBLE
    }

    private fun deleteDocs() {
        if (checkedDocs.isEmpty()) {
            ToastUtil.showShort(this, "未选中文档")
            return
        }

        if (!pay) {
            toPayPage()
            return
        }

        launch(Dispatchers.IO) {
            val deleteList = arrayListOf<FileWithType>()
            deleteList.addAll(checkedDocs)
            WxManager.getInstance(this@WeChatDocRecoveryActivity).deleteFile(deleteList, object : FileWithTypeCallback {
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

    private fun notify(file: FileWithType) {
        if (Constant.ScanStop) return

        mAdapter.notifyItemRemoved(mainDocs.indexOf(file))
        mainDocs.remove(file)
        sortDocs.remove(file)
        checkedDocs.remove(file)
        if (checkedDocs.isEmpty()) {
            recovery.text = getString(R.string.pics_list_button)
            delete.text = getString(R.string.list_delete_button)
        } else {
            val rText = getString(R.string.pics_list_button) + "(选中${checkedDocs.size})"
            val dText = getString(R.string.list_delete_button) + "(选中${checkedDocs.size})"
            recovery.text = rText
            delete.text = dText
        }
    }

    private fun checkOption() {
        if (!chooseAll) {
            chooseAll = true
            if (sortDocs.isNotEmpty()) {
                checkedDocs.clear()
                checkedDocs.addAll(sortDocs)
                val rText = getString(R.string.pics_list_button) + "(选中${checkedDocs.size})"
                val dText = getString(R.string.list_delete_button) + "(选中${checkedDocs.size})"
                recovery.text = rText
                delete.text = dText
                mainDocs.clear()
                for (child in sortDocs) {
                    mainDocs.add(FileWithType(child.name, child.path, child.size, child.date, child.type, true))
                }
                mAdapter.notifyItemRangeChanged(0, mainDocs.size)
            }
        } else {
            chooseAll = false
            if (sortDocs.isNotEmpty()) {
                checkedDocs.clear()
                recovery.text = getString(R.string.pics_list_button)
                delete.text = getString(R.string.list_delete_button)
                mainDocs.clear()
                for (child in sortDocs) {
                    mainDocs.add(FileWithType(child.name, child.path, child.size, child.date, child.type, false))
                }
                mAdapter.notifyItemRangeChanged(0, mainDocs.size)
            }
        }
    }

    private fun toPayPage() {
        val intent = Intent()
        intent.setClass(this, PayActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        intent.putExtra("title", "微信文档恢复")
        startActivity(intent)
    }

    private fun toSinglePayPage() {
        val intent = Intent()
        intent.setClass(this, SinglePayActivity::class.java)
        intent.putExtra("serviceCode", Constant.DELETE)
        startActivity(intent)
    }

    private fun toDocDetailPage(itemData: FileWithType) {
        val intent = Intent()
        intent.setClass(this, DocDetailActivity::class.java)
        intent.putExtra("file", itemData)
        startActivityForResult(intent, 0x103)
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

        if (requestCode == 0x103 && resultCode == 0x103) {
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
        mainDocs.clear()
        sortDocs.clear()
        checkedDocs.clear()
    }
}