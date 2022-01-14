package com.wx.tools.view.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.res.ResourcesCompat
import com.baidu.mobads.action.ActionParam
import com.baidu.mobads.action.ActionType
import com.baidu.mobads.action.BaiduAction
import com.tencent.mmkv.MMKV
import com.wx.tools.R
import com.wx.tools.bean.FileBean
import com.wx.tools.callback.DialogCallback
import com.wx.tools.callback.PayCallback
import com.wx.tools.config.Constant
import com.wx.tools.controller.PayManager
import com.wx.tools.controller.WxManager
import com.wx.tools.http.loader.OrderDetailLoader
import com.wx.tools.http.response.ResponseTransformer
import com.wx.tools.http.schedulers.SchedulerProvider
import com.wx.tools.utils.AppUtil
import com.wx.tools.utils.JLog
import com.wx.tools.utils.RomUtil
import com.wx.tools.utils.ToastUtil
import com.wx.tools.view.base.BaseActivity
import com.wx.tools.view.views.AutoTextView
import com.wx.tools.view.views.PaySuccessDialog
import com.wx.tools.view.views.QuitDialog
import com.zyp.cardview.YcCardView
import kotlinx.android.synthetic.main.heart_small.view.*
import kotlinx.android.synthetic.main.item_steps.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*


class PayActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var pay: Button
    private lateinit var userAgreement: AppCompatCheckBox
    private lateinit var wechatPay: AppCompatCheckBox
    private lateinit var aliPay: AppCompatCheckBox
    private lateinit var titleName: TextView
    private lateinit var menuLayout: FrameLayout
    private lateinit var otherLayout: FrameLayout
    private lateinit var menuCard: YcCardView
    private lateinit var otherCard: YcCardView
    private lateinit var menuPriceView: TextView
    private lateinit var originalMenuPriceView: TextView
    private lateinit var priceView: TextView
    private lateinit var originPriceView: TextView
    private lateinit var discount: TextView
    private lateinit var introduce: TextView
    private lateinit var menuSign: ImageView
    private lateinit var menuBox: ImageView
    private lateinit var functionAll: ImageView
    private lateinit var functionPart: ImageView
    private lateinit var functiontitle: ImageView

    private var serviceId: Int = 0
    private var currentServiceId = 0
    private var firstServiceId = 0
    private var secondServiceId = 0
    private var chooseIndex = 1

    private var title: String? = null
    private var lastClickTime: Long = 0L

    private var mPrice = 0f
    private var originalMenuPrice = 0f
    private var oPrice = 0f
    private var otherPrice = 0f
    private var price = 0f

    private lateinit var counter: TextView
    private lateinit var counterTimer: CountDownTimer
    private lateinit var timer: CountDownTimer
    private lateinit var customerAgreement: TextView
    private lateinit var notice: AutoTextView
    private var remindTime = 15 * 60 * 1000L
    private var kv: MMKV? = MMKV.defaultMMKV()
    private var orderSn = ""
    private var startPay = false

    override fun setLayout(): Int {
        return R.layout.a_recovery_pay
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        back = findViewById(R.id.iv_back)
        pay = findViewById(R.id.do_pay)
        wechatPay = findViewById(R.id.do_wechat_pay)
        aliPay = findViewById(R.id.do_alipay_pay)
        titleName = findViewById(R.id.pay_content)
        menuPriceView = findViewById(R.id.menu_price)
        originalMenuPriceView = findViewById(R.id.original_menu_price)
        priceView = findViewById(R.id.price)
        originPriceView = findViewById(R.id.original_price)
        introduce = findViewById(R.id.introduce)
        counter = findViewById(R.id.counter)
        notice = findViewById(R.id.tv_notice)
        customerAgreement = findViewById(R.id.customer_agreement)
        userAgreement = findViewById(R.id.user_agreement)
        discount = findViewById(R.id.discount)
        menuSign = findViewById(R.id.menu_sign)
        functionAll = findViewById(R.id.function_all)
        functionPart = findViewById(R.id.function_part)
        functiontitle = findViewById(R.id.function_title)
        menuBox = findViewById(R.id.menu_box)

        wechatPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                aliPay.isChecked = false
            }
        }

        aliPay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wechatPay.isChecked = false
            }
        }

        menuLayout = findViewById(R.id.ll_1)
        otherLayout = findViewById(R.id.ll_2)
        menuCard = findViewById(R.id.card_1)
        otherCard = findViewById(R.id.card_2)

        //原价删除线
        originalMenuPriceView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
        originPriceView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG

        back.setOnClickListener { onBackPressed() }
        pay.setOnClickListener { checkPay(this) }
        menuLayout.setOnClickListener { chooseMenu() }
        otherLayout.setOnClickListener { chooseOther() }
        customerAgreement.setOnClickListener { toAgreementPage() }

        chooseMenu()
        kv = MMKV.defaultMMKV()

        initNotice()
        initCounter()
    }

    override fun onResume() {
        super.onResume()
        if (startPay) {
            checkPayResult()
        }
    }

    override fun initData() {
        serviceId = intent.getIntExtra("serviceId", 0)
        title = intent.getStringExtra("title")
        if (title != null) {
            titleName.text = title
        }

        getServicePrice()
    }

    private fun initNotice() {
        timer = object : CountDownTimer(4000 * 1000L, 4000) {
            override fun onFinish() {

            }

            override fun onTick(millisUntilFinished: Long) {
                val str = WxManager.getInstance(this@PayActivity).getRecoveryUser()
                notice.setText(str, Color.GRAY)
            }
        }

        timer.start()
    }

    private fun initCounter() {
        val result = kv?.decodeLong("pay_counter")
        remindTime = if (result == 0L) 15 * 60 * 1000L else result!!

        counterTimer = object : CountDownTimer(remindTime, 100 / 6L) {
            override fun onFinish() {
                val text = AppUtil.timeStamp2Date("0", "mm:ss:SS")
                counter.text = text
                kv?.encode("pay_counter", 15 * 60 * 1000L)
            }

            override fun onTick(millisUntilFinished: Long) {
                val text = AppUtil.timeStamp2Date(millisUntilFinished.toString(), "mm:ss:SS")
                counter.text = text
                remindTime = millisUntilFinished
            }
        }
    }


    private fun chooseMenu() {
        menuLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.background_gradient_stroke, null)
        otherLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_nomal, null)
        menuSign.visibility = View.VISIBLE
        functionAll.visibility = View.VISIBLE
        functionPart.visibility = View.GONE
        chooseIndex = 1
    }

    private fun chooseOther() {
        menuLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.background_gradient, null)
        otherLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.pay_background_stroke, null)
        menuSign.visibility = View.GONE
        functionAll.visibility = View.GONE
        functionPart.visibility = View.VISIBLE
        chooseIndex = 2
    }

    @SuppressLint("SetTextI18n")
    private fun getServicePrice() {
        var type = Constant.COM
        if (serviceId == 1 || serviceId == 2) {
            type = Constant.REC
        }

        PayManager.getInstance().getPayStatus(this, type) {
            discount.text = "支付立减${it.discountFee}"

            val packDetails = it.packDetail

            //单项补价套餐
            if (packDetails.size == 1) {
                firstServiceId = packDetails[0].id
                originalMenuPrice = packDetails[0].server_price.toFloat()
                mPrice = packDetails[0].sale_price.toFloat()
                otherCard.visibility = View.GONE
                menuSign.visibility = View.GONE
                menuBox.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_pay_repl, null))
                functiontitle.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_remind_title, null))
                functionAll.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_remind_func, null))
            }

            //多项套餐或者补价套餐
            if (packDetails.size == 2) {
                for (child in packDetails) {
                    if (child.server_code == Constant.REC) {
                        firstServiceId = child.id
                        originalMenuPrice = child.server_price.toFloat()
                        mPrice = child.sale_price.toFloat()
                    }

                    if (child.server_code == Constant.COM) {
                        secondServiceId = child.id
                        oPrice = child.sale_price.toFloat()
                        otherPrice = child.server_price.toFloat()
                        introduce.text = child.desc
                    }
                }
            }
//            if (packDetails.size == 2) {
//                for (child in packDetails) {
//                    if (child.expire_type == "2") {
//                        firstServiceId = child.id
//                        originalMenuPrice = child.server_price.toFloat()
//                        mPrice = child.sale_price.toFloat()
//                    } else {
//                        secondServiceId = child.id
//                        oPrice = child.sale_price.toFloat()
//                        otherPrice = child.server_price.toFloat()
//                        introduce.text = child.desc
//                    }
//                }
//            }

            //刷新价格
            changeDescription(packDetails.size)
        }
    }


    private fun changeDescription(index: Int) {
        pay.visibility = View.VISIBLE
        when (index) {
            0 -> {
                menuLayout.visibility = View.GONE
                otherLayout.visibility = View.GONE
            }

            1 -> {
                menuLayout.visibility = View.VISIBLE
                menuPriceView.text = String.format("%.0f", mPrice)
                originalMenuPriceView.text = String.format("%.0f", originalMenuPrice)
                counterTimer.start()
            }

            2 -> {
                menuLayout.visibility = View.VISIBLE
                otherLayout.visibility = View.VISIBLE
                menuPriceView.text = String.format("%.0f", mPrice)
                originalMenuPriceView.text = String.format("%.0f", originalMenuPrice)
                priceView.text = String.format("%.0f", oPrice)
                originPriceView.text = String.format("%.0f", otherPrice)
                counterTimer.start()
            }

            else -> {
                menuLayout.visibility = View.GONE
                otherLayout.visibility = View.GONE
            }
        }
    }


    private fun checkPay(c: Activity) {
        if (!userAgreement.isChecked) {
            ToastUtil.show(this, "请阅读并勾选《会员须知》")
            return
        }

        if (!wechatPay.isChecked && !aliPay.isChecked) {
            ToastUtil.show(this, "请选择付款方式")
            return
        }

        if (lastClickTime == 0L) {
            lastClickTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - lastClickTime < 2 * 1000) {
            ToastUtil.showShort(c, "请不要频繁发起支付")
            return
        }

        lastClickTime = System.currentTimeMillis()

        when(chooseIndex){
            1 ->{
                currentServiceId = firstServiceId
                price = mPrice
            }
            2 ->{
                currentServiceId = secondServiceId
                price = oPrice
            }
        }


        if (wechatPay.isChecked) {
            doPay(c, 0)
        } else {
            doPay(c, 1)
        }
    }

    /**
     *  index = 0快速支付 1支付宝支付
     */
    private fun doPay(c: Activity, index: Int) {
        when (index) {
            0 ->{
                startPay = true
                PayManager.getInstance().doFastPay(c, currentServiceId, object : PayCallback {
                    override fun success() {
                    }

                    override fun progress(orderId: String) {
                        orderSn = orderId
                    }

                    override fun failed(msg: String) {
                        launch(Dispatchers.Main) {
                            ToastUtil.showShort(c, msg)
                        }
                    }
                })
            }

            1 -> PayManager.getInstance().doAliPay(c, currentServiceId, object : PayCallback {
                override fun success() {
                    launch(Dispatchers.Main) {

                        //pay upload
                        if (!RomUtil.isOppo() && Constant.OCPC) {
                            val actionParam = JSONObject()
                            actionParam.put(ActionParam.Key.PURCHASE_MONEY, price * 100)
                            BaiduAction.logAction(ActionType.PURCHASE, actionParam)
                        }

                        //支付成功
                        ToastUtil.showShort(c, "支付成功")


                        //根据套餐判断是否跳转到补价页面
                        if (currentServiceId == secondServiceId) {
                            toPaySuccessPage()
                        } else {
                            openPaySuccessDialog()
                        }

                    }
                }

                override fun progress(orderId: String) {
                    orderSn = orderId
                }

                override fun failed(msg: String) {
                    launch(Dispatchers.Main) {
                        ToastUtil.showShort(c, msg)
                    }
                }
            })
        }
    }


    private fun checkPayResult() {
        if (orderSn == "") return
        launch(Dispatchers.IO) {
            OrderDetailLoader.getOrderStatus(orderSn)
                .compose(ResponseTransformer.handleResult())
                .compose(SchedulerProvider.getInstance().applySchedulers())
                .subscribe({
                    JLog.i("order_sn = ${it.order_sn}")
                    if (it.order_sn != orderSn) {
                        return@subscribe
                    }

                    when (it.status) {
                        "1" -> {
                            //pay upload
                            if (!RomUtil.isOppo() && Constant.OCPC) {
                                val actionParam = JSONObject()
                                actionParam.put(ActionParam.Key.PURCHASE_MONEY, price * 100)
                                BaiduAction.logAction(ActionType.PURCHASE, actionParam)
                            }

                            if (currentServiceId == secondServiceId) {
                                toPaySuccessPage()
                            } else {
                                openPaySuccessDialog()
                            }

                        }

                        else -> {
                            ToastUtil.show(this@PayActivity, "未支付")
                        }
                    }

                }, {
                    ToastUtil.show(this@PayActivity, "查询支付结果失败")
                })
        }

    }

    private fun toAgreementPage() {
        val intent = Intent()
        intent.setClass(this, AgreementActivity::class.java)
        startActivity(intent)
    }

    private fun toPaySuccessPage() {
        val intent = Intent(this, PaySuccessActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        startActivity(intent)
        finish()
    }

    private fun openPaySuccessDialog() {
        PaySuccessDialog(this@PayActivity, object : DialogCallback {
            override fun onSuccess(file: FileBean) {
                setResult(0x100)
                finish()
            }

            override fun onCancel() {
            }
        }).show()
    }


    override fun onBackPressed() {
        QuitDialog(this, getString(R.string.quite_title), object : DialogCallback {
            override fun onSuccess(file: FileBean) {
                finish()
            }

            override fun onCancel() {
            }
        }).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        timer.cancel()
        counterTimer.cancel()

        if (kv != null && remindTime != 0L) {
            kv?.encode("pay_counter", remindTime)
        }
    }


}