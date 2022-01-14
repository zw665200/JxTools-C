package com.wx.tools.view.fragment

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baidu.mobads.action.ActionParam
import com.baidu.mobads.action.ActionType
import com.baidu.mobads.action.BaiduAction
import com.bumptech.glide.Glide
import com.bytedance.sdk.openadsdk.*
import com.tencent.mmkv.MMKV
import com.wx.tools.R
import com.wx.tools.adapter.DataAdapter
import com.wx.tools.bean.Resource
import com.wx.tools.bean.UserInfo
import com.wx.tools.config.Constant
import com.wx.tools.controller.IMManager
import com.wx.tools.controller.PayManager
import com.wx.tools.http.loader.TokenLoader
import com.wx.tools.http.loader.UserInfoLoader
import com.wx.tools.http.response.ResponseTransformer
import com.wx.tools.http.schedulers.SchedulerProvider
import com.wx.tools.utils.*
import com.wx.tools.view.activity.*
import com.wx.tools.view.base.BaseFragment
import com.wx.tools.view.views.ClockView
import com.zyp.cardview.YcCardView
import kotlinx.android.synthetic.main.item_heart.view.*
import kotlinx.android.synthetic.main.item_recommend.view.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*

open class FHome : BaseFragment() {
    private lateinit var rv: RecyclerView
    private lateinit var otherRv: RecyclerView
    private lateinit var recommendRv: RecyclerView
    private lateinit var ad: FrameLayout
    private lateinit var mainAdapter: DataAdapter<Resource>
    private lateinit var otherAdapter: DataAdapter<Resource>
    private lateinit var recommendAdapter: DataAdapter<String>
    private var mainPics = mutableListOf<Resource>()
    private var otherPics = mutableListOf<Resource>()
    private var recommendList = mutableListOf<String>()
    private lateinit var mine: ImageView
    private lateinit var cardView: YcCardView
    private lateinit var clockView: ClockView
    private lateinit var robot: FrameLayout
    private lateinit var percent: TextView
    private lateinit var used: TextView
    private lateinit var unused: TextView
    private lateinit var model: TextView
    private lateinit var customerService: LinearLayout
    private lateinit var customerFeedback: LinearLayout
    private var servicePermission = false
    private var descrption = ""
    private var lastClickTime = 0L
    private var mmkv = MMKV.defaultMMKV()

    override fun initView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.f_home, container, false)
        rv = rootView.findViewById(R.id.ry_billboard)
        otherRv = rootView.findViewById(R.id.ry_other_func)
        recommendRv = rootView.findViewById(R.id.recommend)
        mine = rootView.findViewById(R.id.mine)
        model = rootView.findViewById(R.id.model)
        percent = rootView.findViewById(R.id.percent)
        used = rootView.findViewById(R.id.used)
        unused = rootView.findViewById(R.id.unused)
        robot = rootView.findViewById(R.id.robot_bg)
        cardView = rootView.findViewById(R.id.cardview)
        customerService = rootView.findViewById(R.id.customer_service)
        customerFeedback = rootView.findViewById(R.id.customer_feedback)
        clockView = rootView.findViewById(R.id.clock_view)
        ad = rootView.findViewById(R.id.ad)

        mine.setOnClickListener { toMinePage() }
        customerService.setOnClickListener { toCustomerServicePage() }
        customerFeedback.setOnClickListener { toFeedbackPage() }

        return rootView
    }

    override fun initData() {

        mainPics.clear()
        mainPics.add(Resource("wechat", R.drawable.wechat, "聊天恢复"))
        mainPics.add(Resource("friends", R.drawable.friends, "好友恢复"))
        mainPics.add(Resource("doc", R.drawable.doc, "文档恢复"))
        mainPics.add(Resource("video", R.drawable.video, "视频恢复"))
        mainPics.add(Resource("pic", R.drawable.pic, "图片恢复"))
        mainPics.add(Resource("audio", R.drawable.voice, "语音恢复"))

        otherPics.clear()
        otherPics.add(Resource("bill", R.drawable.bill, "账单记录恢复"))
        otherPics.add(Resource("delete", R.drawable.delete, "彻底删除记录"))

        initMainRecycleView()
        initOtherRecycleView()
        initRecommendRecycleView()
        loadExpressAd()

        checkPermission()

    }

    override fun onResume() {
        super.onResume()
        initProgressBar()
        initCurrentStatus()
        initCustomerService()
        checkPay(activity!!)
    }


    override fun click(v: View?) {
    }

    private fun initCustomerService() {
        IMManager.setMessageListener {
            AppUtil.sendNotification(activity, Constant.Notification_title, Constant.Notification_content)
        }
    }

    private fun initProgressBar() {
        //动态调整进度条的大小
        val width = AppUtil.getScreenWidth(activity)
        val layout = robot.layoutParams
        layout.width = width / 2
        layout.height = width / 2
        robot.layoutParams = layout

        val layoutParam = clockView.layoutParams
        layoutParam.width = width / 2
        clockView.layoutParams = layoutParam

        val total = DeviceUtil.getTotalExternalMemorySize()
        val free = DeviceUtil.getFreeSpace()
        val per = 100 * (total - free) / total
        val totalGB = String.format("%.2f", total.toFloat() / 1024 / 1024 / 1024)
        val usedGB = String.format("%.2f", (total - free).toFloat() / 1024 / 1024 / 1024)
        percent.text = "${per}%"
        used.text = "已使用：${usedGB}GB"
        unused.text = "总空间：${totalGB}GB"

        if (Build.BRAND == "HUAWEI" || Build.BRAND == "HONOR") {
            val name = Dict.getHUAWEIName(Build.MODEL)
            if (name.isNullOrEmpty()) {
                val b = "手机型号: ${Build.BRAND} ${Build.MODEL}"
                model.text = b
            } else {
                val b = "手机型号: $name"
                model.text = b
            }
        } else {
            val b = "手机型号: ${Build.BRAND} ${Build.MODEL}"
            model.text = b
        }

        clockView.setCompleteDegree(per * 1.0f)
    }

    private fun initCurrentStatus() {
        Constant.CURRENT_BACKUP_TIME = mmkv?.decodeLong("backup_time")
        Constant.CURRENT_BACKUP_PATH = mmkv?.decodeString("backup_path")
    }

    private fun initMainRecycleView() {
        mainAdapter = DataAdapter.Builder<Resource>()
            .setData(mainPics)
            .setLayoutId(R.layout.item_heart)
            .addBindView { itemView, itemData ->
                Glide.with(this).load(itemData.icon).into(itemView.iv_icon)
                itemView.tv_name.text = itemData.name
                itemView.setOnClickListener {
                    if (lastClickTime == 0L) {
                        lastClickTime = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastClickTime < 1000) return@setOnClickListener
                    }

                    lastClickTime = System.currentTimeMillis()

                    checkPermissions {
                        when (itemData.type) {
                            "wechat" -> checkPay(1, itemData.type)
                            "friends" -> checkPay(2, itemData.type)
                            "doc" -> goDocRecovery()
                            "video" -> goVideoRecovery()
                            "pic" -> goPicRecovery()
                            "audio" -> goAudioRecovery()
                            "bill" -> checkSinglePay(Constant.BILL)
                            "delete" -> checkSinglePay(Constant.DELETE)
                        }
                    }

                }
            }
            .create()

        rv.adapter = mainAdapter
        rv.layoutManager = GridLayoutManager(activity, 3)
        mainAdapter.notifyItemRangeChanged(0, mainPics.size)
    }

    private fun initOtherRecycleView() {
        otherAdapter = DataAdapter.Builder<Resource>()
            .setData(otherPics)
            .setLayoutId(R.layout.item_heart)
            .addBindView { itemView, itemData ->
                Glide.with(this).load(itemData.icon).into(itemView.iv_icon)
                itemView.tv_name.text = itemData.name
                itemView.setOnClickListener {
                    if (lastClickTime == 0L) {
                        lastClickTime = System.currentTimeMillis()
                    } else {
                        if (System.currentTimeMillis() - lastClickTime < 1000) return@setOnClickListener
                    }

                    lastClickTime = System.currentTimeMillis()

                    checkPermissions {
                        when (itemData.type) {
                            "wechat" -> checkPay(1, itemData.type)
                            "friends" -> checkPay(2, itemData.type)
                            "doc" -> goDocRecovery()
                            "video" -> goVideoRecovery()
                            "pic" -> goPicRecovery()
                            "audio" -> goAudioRecovery()
                            "bill" -> checkSinglePay(Constant.BILL)
                            "delete" -> checkSinglePay(Constant.DELETE)
                        }
                    }

                }
            }
            .create()

        otherRv.adapter = otherAdapter
        otherRv.layoutManager = GridLayoutManager(activity, 3)
        otherAdapter.notifyItemRangeChanged(0, otherPics.size)
    }

    private fun initRecommendRecycleView() {
        recommendList = Dict.getRecommendList()
        recommendAdapter = DataAdapter.Builder<String>()
            .setData(recommendList)
            .setLayoutId(R.layout.item_recommend)
            .addBindView { itemView, itemData ->
                val text = "用户QL*****${Random().nextInt(9999)} 评论："
                itemView.recommend_title.text = text
                itemView.recommend_content.text = itemData
            }
            .create()

        recommendRv.layoutManager = LinearLayoutManager(activity)
        recommendRv.adapter = recommendAdapter
        recommendAdapter.notifyItemRangeChanged(0, recommendList.size)
    }

    private fun checkPermission() {
        if (Constant.CLIENT_TOKEN == "") {
            checkPermissions {
                getAccessToken()
            }
        } else {
            checkPay(activity!!)
        }
    }

    private fun checkPay(context: Context) {
        PayManager.getInstance().getPayStatus(context, Constant.COM) {
            when (it.serverExpire) {
                0 -> {
                    val pack = it.packDetail
                    if (pack.isEmpty()) {
                        mmkv?.encode("recovery", 110)
                        descrption = "高级套餐"
                        servicePermission = true
                        return@getPayStatus
                    }

                    if (pack.size == 1) {
                        if (pack[0].server_code == Constant.REPL) {
                            mmkv?.encode("recovery", 111)
                            descrption = "普通套餐"
                            servicePermission = true
                            return@getPayStatus
                        }
                    }

                    if (pack.size == 2) {
                        if (pack[0].server_code == Constant.REPL) {
                            mmkv?.encode("recovery", 111)
                            descrption = "普通套餐"
                            servicePermission = true
                            return@getPayStatus
                        } else {
                            mmkv?.encode("recovery", 0)
                            servicePermission = false
                        }
                    }
                }

                else -> {
                    mmkv?.encode("recovery", 0)
                    servicePermission = false
                }
            }
        }
    }

    private fun getAccessToken() {
        launch(Dispatchers.IO) {
            TokenLoader.getToken(activity!!)
                .compose(ResponseTransformer.handleResult())
                .compose(SchedulerProvider.getInstance().applySchedulers())
                .subscribe({
                    getUserInfo(it.questToken)
                }, {
                    ToastUtil.show(activity!!, "请求失败，请检查网络")
                })
        }
    }

    private fun getUserInfo(token: String) {
        launch(Dispatchers.IO) {
            UserInfoLoader.getUser(token)
                .compose(ResponseTransformer.handleResult())
                .compose(SchedulerProvider.getInstance().applySchedulers())
                .subscribe({
                    if (it.isNotEmpty()) {
                        Constant.CLIENT_TOKEN = it[0].client_token
                        Constant.USER_NAME = it[0].nickname

                        val userInfo = UserInfo(
                            it[0].id,
                            it[0].nickname,
                            it[0].user_type,
                            it[0].addtime,
                            it[0].last_logintime,
                            it[0].login_ip,
                            it[0].popularize_id,
                            it[0].pop_name,
                            it[0].client_token,
                            it[0].city
                        )

                        mmkv?.encode("userInfo", userInfo)

                        //active upload
                        if (!RomUtil.isOppo() && Constant.OCPC) {
                            BaiduAction.logAction(ActionType.REGISTER)

//                            val actionParam = JSONObject()
//                            actionParam.put(ActionParam.Key.PURCHASE_MONEY, 100)
//                            BaiduAction.logAction(ActionType.PURCHASE, actionParam)
                        }

                        checkPay(activity!!)

                        //IM register
                        IMManager.register(it[0].nickname, {}, {})
                    }

                }, {
                    JLog.i("error = ${it.message}")
                })
        }
    }

    private fun checkPay(serviceId: Int, type: String) {
        PayManager.getInstance().checkRecoveryPay(activity!!, serviceId) {
            if (it) {
                JLog.i("it = $it")
                when (type) {
                    "wechat" -> goWechatRecovery()
                    "friends" -> goContactRecovery()
                }
            } else {
                toPayPage(serviceId)
            }
        }
    }

    private fun checkSinglePay(serviceCode: String) {
        when (serviceCode) {
            Constant.BILL -> {
                PayManager.getInstance().checkBillPay(activity!!) {
                    if (it) {
                        goBillRecovery()
                    } else {
                        toSinglePayPage(serviceCode)
                    }
                }
            }

            Constant.DELETE -> {
                PayManager.getInstance().checkDeletePay(activity!!) {
                    if (it) {
                        goDeleteRecovery()
                    } else {
                        toSinglePayPage(serviceCode)
                    }
                }
            }
        }
    }

    private fun toPayPage(serviceId: Int) {
        val intent = Intent()
        intent.setClass(activity!!, PayActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        startActivity(intent)
    }

    private fun toSinglePayPage(serviceCode: String) {
        val intent = Intent()
        intent.setClass(activity!!, SinglePayActivity::class.java)
        intent.putExtra("serviceCode", serviceCode)
        startActivity(intent)
    }

    private fun toCustomerServicePage() {
        if (servicePermission) {
            if (Constant.USER_NAME.isEmpty()) return
            IMManager.register(Constant.USER_NAME, {
                //防止进入会话发送消息还在回调
                IMManager.removeMessageListener()
                IMManager.startConversation(activity!!, Constant.USER_NAME, descrption)
            }, {})
        } else {
            ToastUtil.showShort(activity!!, "成为会员即可发起会话")
        }
    }

    private fun toFeedbackPage() {
        if (servicePermission) {
            val intent = Intent()
            intent.setClass(activity!!, FeedbackActivity::class.java)
            startActivity(intent)
        } else {
            ToastUtil.showShort(activity!!, "成为会员即可投诉与退款")
        }
    }

    private fun toMinePage() {
        val intent = Intent()
        intent.setClass(activity!!, MineActivity::class.java)
        startActivity(intent)
    }

    private fun goWechatRecovery() {
        val intent = Intent()
        intent.setClass(context!!, RecoveryTipsActivity::class.java)
        intent.putExtra("serviceId", 1)
        activity?.startActivity(intent)
    }

    private fun goContactRecovery() {
        val intent = Intent()
        intent.setClass(context!!, RecoveryTipsActivity::class.java)
        intent.putExtra("serviceId", 2)
        activity?.startActivity(intent)
    }

    private fun goDocRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WeChatDocRecoveryActivity::class.java)
        intent.putExtra("serviceId", 3)
        activity?.startActivity(intent)
    }

    private fun goVideoRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WeChatVideoRecoveryActivity::class.java)
        intent.putExtra("serviceId", 4)
        activity?.startActivity(intent)
    }

    private fun goPicRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WeChatPicsRecoveryActivity::class.java)
        intent.putExtra("serviceId", 5)
        activity?.startActivity(intent)
    }

    private fun goAudioRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WeChatVoiceRecoveryActivity::class.java)
        intent.putExtra("serviceId", 6)
        activity?.startActivity(intent)
    }

    private fun goBillRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WechatBillRecoveryActivity::class.java)
        intent.putExtra("serviceId", 13)
        activity?.startActivity(intent)
    }

    private fun goDeleteRecovery() {
        val intent = Intent()
        intent.setClass(context!!, WechatDeleteActivity::class.java)
        intent.putExtra("serviceId", 14)
        activity?.startActivity(intent)
    }

    private fun loadExpressAd() {
        val width = AppUtil.getScreenWidth(activity)
        val dp = AppUtil.px2dip(activity, width * 1.0f)
        val mTTAdNative = TTAdSdk.getAdManager().createAdNative(activity)
        val adSlot = AdSlot.Builder()
            .setCodeId("946909788")
            .setSupportDeepLink(true)
            .setAdCount(1)
            .setExpressViewAcceptedSize((dp - 30) * 1.0f, 0f)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()

        mTTAdNative.loadNativeExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(p0: Int, p1: String?) {
                JLog.i("load ad error code = $p0")
                ad.visibility = View.GONE
            }

            override fun onNativeExpressAdLoad(p0: MutableList<TTNativeExpressAd>?) {
                if (p0 != null) {
                    JLog.i("ad count is ${p0.size}")
                    if (p0.size > 0) {
                        p0[0].render()
                        val view = p0[0].expressAdView
                        if (view != null) {
                            ad.addView(view)
                        } else {
                            ad.visibility = View.GONE
                            JLog.i("ad view is null")
                        }
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        IMManager.removeMessageListener()
        IMManager.logout()
    }
}