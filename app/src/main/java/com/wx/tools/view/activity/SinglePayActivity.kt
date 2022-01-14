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
import com.baidu.mobads.action.ActionParam
import com.baidu.mobads.action.ActionType
import com.baidu.mobads.action.BaiduAction
import com.tencent.mmkv.MMKV
import com.wx.tools.R
import com.wx.tools.bean.FileBean
import com.wx.tools.bean.Price
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
import com.wx.tools.view.views.QuitDialog
import kotlinx.android.synthetic.main.item_steps.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*


class SinglePayActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var pay: Button
    private lateinit var userAgreement: AppCompatCheckBox
    private lateinit var wechatPay: AppCompatCheckBox
    private lateinit var aliPay: AppCompatCheckBox
    private lateinit var titleName: TextView
    private lateinit var otherLayout: FrameLayout
    private lateinit var introduce: TextView
    private lateinit var guide: TextView
    private lateinit var priceView: TextView
    private lateinit var originPriceView: TextView
    private lateinit var discount: TextView
    private lateinit var billIntroduce: ImageView
    private lateinit var deleteIntroduce: ImageView
    private lateinit var counterTimer: CountDownTimer
    private lateinit var timer: CountDownTimer
    private lateinit var counter: TextView
    private var serviceId: Int = 0
    private var serviceCode: String? = null
    private var title: String? = null
    private var lastClickTime: Long = 0L
    private var oPrice = 0f
    private lateinit var customerAgreement: TextView
    private var kv: MMKV? = MMKV.defaultMMKV()
    private var orderSn = ""
    private var startPay = false
    private var mPrice = 0f
    private var remindTime = 15 * 60 * 1000L
    private lateinit var notice: AutoTextView

    override fun setLayout(): Int {
        return R.layout.a_bill_pay
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        back = findViewById(R.id.iv_back)
        pay = findViewById(R.id.do_pay)
        wechatPay = findViewById(R.id.do_wechat_pay)
        aliPay = findViewById(R.id.do_alipay_pay)
        titleName = findViewById(R.id.pay_content)
        priceView = findViewById(R.id.price)
        originPriceView = findViewById(R.id.original_price)
        introduce = findViewById(R.id.introduce)
        guide = findViewById(R.id.guide)
        customerAgreement = findViewById(R.id.customer_agreement)
        userAgreement = findViewById(R.id.user_agreement)
        otherLayout = findViewById(R.id.ll_2)
        counter = findViewById(R.id.counter)
        discount = findViewById(R.id.discount)
        billIntroduce = findViewById(R.id.iv_bill_introduce)
        deleteIntroduce = findViewById(R.id.iv_delete_introduce)
        notice = findViewById(R.id.tv_notice)

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

        //原价删除线
        originPriceView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG

        back.setOnClickListener { onBackPressed() }
        pay.setOnClickListener { checkPay(this) }
        customerAgreement.setOnClickListener { toAgreementPage() }

        kv = MMKV.defaultMMKV()

        initCounter()
        initNotice()
    }

    override fun onResume() {
        super.onResume()
        if (startPay) {
            checkPayResult()
        }
    }

    override fun initData() {
        serviceCode = intent.getStringExtra("serviceCode")
        title = intent.getStringExtra("title")
        if (title != null) {
            titleName.text = title
        }

        when (serviceCode) {
            Constant.BILL -> {
                introduce.text = getString(R.string.bill_pay_description)
                guide.text = getString(R.string.bill_pay_guide)
                deleteIntroduce.visibility = View.GONE
            }

            Constant.DELETE -> {
                introduce.text = getString(R.string.delete_pay_description)
                guide.text = getString(R.string.delete_pay_guide)
                billIntroduce.visibility = View.GONE
            }
        }

        val service = kv?.decodeParcelable(serviceCode, Price::class.java)
        if (service != null) {
            serviceId = service.id
            JLog.i("serviceId = $serviceId")
        }

        getServicePrice()

    }

    private fun initNotice() {
        timer = object : CountDownTimer(4000L * 1000, 4000) {
            override fun onFinish() {

            }

            override fun onTick(millisUntilFinished: Long) {
                val str = WxManager.getInstance(this@SinglePayActivity).getRecoveryUser()
                notice.setText(str, Color.GRAY)
            }
        }

        timer.start()
    }

    private fun initCounter() {
        val result = kv?.decodeLong("single_pay_counter")
        remindTime = if (result == 0L) 15 * 60 * 1000L else result!!

        counterTimer = object : CountDownTimer(remindTime, 100L / 6) {
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

        counterTimer.start()
    }


    private fun toAgreementPage() {
        val intent = Intent()
        intent.setClass(this, AgreementActivity::class.java)
        startActivity(intent)
    }

    private fun getServicePrice() {
        PayManager.getInstance().getSinglePayStatus(this, serviceId) {
            val text = "支付立减${it.discountFee}"
            discount.text = text

            val packDetails = it.packDetail
            if (packDetails.isNotEmpty()) {
                mPrice = packDetails[0].sale_price.toFloat()
                oPrice = packDetails[0].server_price.toFloat()
            }

            //刷新价格
            changeDescription()
        }
    }


    private fun changeDescription() {
        pay.visibility = View.VISIBLE
        otherLayout.visibility = View.VISIBLE
        priceView.text = String.format("%.0f", mPrice)
        originPriceView.text = String.format("%.0f", oPrice)
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
            0 -> {
                startPay = true
                PayManager.getInstance().doFastPay(c, serviceId, object : PayCallback {
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

            1 -> PayManager.getInstance().doAliPay(c, serviceId, object : PayCallback {
                override fun success() {
                    launch(Dispatchers.Main) {

                        //pay upload
                        if (!RomUtil.isOppo()&& Constant.OCPC) {
                            val actionParam = JSONObject()
                            actionParam.put(ActionParam.Key.PURCHASE_MONEY, mPrice * 100)
                            BaiduAction.logAction(ActionType.PURCHASE, actionParam)
                        }

                        when (serviceCode) {
                            Constant.BILL -> {
                                kv?.encode("bill", true)
                                startActivity(Intent(c, WechatBillRecoveryActivity::class.java))
                            }

                            Constant.DELETE -> {
                                kv?.encode("deletePay", true)
                                startActivity(Intent(c, WechatDeleteActivity::class.java))
                            }
                        }

                        //返回支付结果
                        ToastUtil.showShort(c, "支付成功")
                        setResult(0x100)
                        finish()
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

                            when (serviceCode) {
                                Constant.BILL -> {
                                    kv?.encode("bill", true)
                                    startActivity(Intent(this@SinglePayActivity, WechatBillRecoveryActivity::class.java))
                                }

                                Constant.DELETE -> {
                                    kv?.encode("deletePay", true)
                                    startActivity(Intent(this@SinglePayActivity, WechatDeleteActivity::class.java))
                                }
                            }

                            //pay upload
                            if (!RomUtil.isOppo() && Constant.OCPC) {
                                val actionParam = JSONObject()
                                actionParam.put(ActionParam.Key.PURCHASE_MONEY, mPrice * 100)
                                BaiduAction.logAction(ActionType.PURCHASE, actionParam)
                            }

                            //返回支付结果
                            ToastUtil.showShort(this@SinglePayActivity, "支付成功")
                            setResult(0x100)
                            finish()
                        }

                        else -> {
                            ToastUtil.show(this@SinglePayActivity, "未支付")
                        }
                    }

                }, {
                    ToastUtil.show(this@SinglePayActivity, "查询支付结果失败")
                })
        }

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
            kv?.encode("single_pay_counter", remindTime)
        }
    }


}