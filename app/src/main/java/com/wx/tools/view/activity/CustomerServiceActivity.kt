package com.wx.tools.view.activity

import android.content.Intent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bytedance.sdk.openadsdk.*
import com.wx.tools.R
import com.wx.tools.adapter.DataAdapter
import com.wx.tools.bean.Resource
import com.wx.tools.config.Constant
import com.wx.tools.controller.IMManager
import com.wx.tools.controller.PayManager
import com.wx.tools.utils.ToastUtil
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.JLog
import kotlinx.android.synthetic.main.item_customer.view.*

class CustomerServiceActivity : BaseActivity() {
    private lateinit var customer: RecyclerView
    private lateinit var back: ImageView
    private lateinit var ad: FrameLayout
    private var payed = false
    private var descrption = ""

    override fun setLayout(): Int {
        return R.layout.a_customer_service
    }

    override fun initView() {
        back = findViewById(R.id.iv_back)
        customer = findViewById(R.id.customer_service)
        ad = findViewById(R.id.layout_ad)
        back.setOnClickListener { finish() }
    }

    override fun initData() {
        loadCustomerService()
        checkPay()
        loadExpressAd()
    }

    private fun loadCustomerService() {
        val list = arrayListOf<Resource>()
        list.add(Resource("wechat", R.drawable.customer_tel, "在线客服"))
        list.add(Resource("doc", R.drawable.customer_gd, "提交反馈"))
        val mAdapter = DataAdapter.Builder<Resource>()
            .setData(list)
            .setLayoutId(R.layout.item_customer)
            .addBindView { itemView, itemData, position ->
                Glide.with(this).load(itemData.icon).into(itemView.service_icon)
                itemView.service_name.text = itemData.name
                when (position) {
                    0 -> {
                        val text = "在线客服(10:00-22:00)"
                        itemView.tv_service_title.text = text
                        itemView.tv_service_descrition.text = getString(R.string.vip_service_des)
                    }

                    1 -> {
                        itemView.tv_service_title.text = "投诉与退款"
                        itemView.tv_service_descrition.text = getString(R.string.visitor_service_des)
                    }
                }

                itemView.setOnClickListener {
                    when (position) {
                        0 -> {
                            if (payed) {
                                checkUserStatus()
                            } else {
                                ToastUtil.showShort(this, "成为会员即可发起会话")
                            }
                        }


                        1 -> {
                            if (payed) {
                                val intent = Intent()
                                intent.setClass(this, FeedbackActivity::class.java)
                                startActivity(intent)
                            } else {
                                ToastUtil.showShort(this, "成为会员即可投诉与退款")
                            }
                        }
                    }

                }
            }
            .create()

        customer.layoutManager = LinearLayoutManager(this)
        customer.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
    }

    /**
     * 检查顾客是否开通了Vip
     */
    private fun checkPay() {
        PayManager.getInstance().getPayStatus(this, Constant.REC) {
            when (it.serverExpire) {
                0 -> {
                    val pack = it.packDetail
                    if (pack.isEmpty()) {
                        payed = true
                        descrption = "高级套餐"
                        return@getPayStatus
                    }

                    if (pack.size == 2) {
                        if (pack[0].server_code == Constant.REPL) {
                            payed = true
                            descrption = "普通套餐"
                            return@getPayStatus
                        }
                    }
                }

                else -> {
                    payed = false
                }
            }
        }
    }

    private fun loadExpressAd() {
        val width = AppUtil.getScreenWidth(this)
        val mTTAdNative = TTAdSdk.getAdManager().createAdNative(this);
        val adSlot = AdSlot.Builder()
            .setCodeId("946909788")
            .setSupportDeepLink(true)
            .setAdCount(1) //请求广告数量为1到3条
            .setExpressViewAcceptedSize(width * 1.0f, 0f)
            .setAdLoadType(TTAdLoadType.LOAD)
            .build()

        mTTAdNative.loadNativeExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
            override fun onError(p0: Int, p1: String?) {
                JLog.i("load ad error code = $p0")
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
                            JLog.i("ad view is null")
                        }
                    }
                }
            }
        })
    }

    private fun checkUserStatus() {
        if (Constant.USER_NAME.isEmpty()) return
        IMManager.register(Constant.USER_NAME, { startConversation() }, {})
    }

    private fun startConversation() {
        //防止进入会话发送消息还在回调
        IMManager.removeMessageListener()
        IMManager.startConversation(this, Constant.USER_NAME, descrption)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}