package com.wx.tools.view.activity

import android.content.Intent
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wx.tools.R
import com.wx.tools.adapter.DataAdapter
import com.wx.tools.bean.Resource
import com.wx.tools.view.base.BaseActivity
import kotlinx.android.synthetic.main.item_function.view.*

class PaySuccessActivity : BaseActivity() {
    private lateinit var back: ImageView
    private lateinit var pay: Button
    private lateinit var cancel: Button
    private var serviceId = 0

    override fun setLayout(): Int {
        return R.layout.a_pay_success
    }


    override fun initView() {
        back = findViewById(R.id.iv_back)
        pay = findViewById(R.id.pay_btn)
        cancel = findViewById(R.id.cancel_btn)

        back.setOnClickListener { finish() }
        pay.setOnClickListener { toPay() }
        cancel.setOnClickListener { finish() }

    }

    override fun initData() {
        serviceId = intent.getIntExtra("serviceId", 0)
    }

    private fun toPay() {
        val intent = Intent(this, PayActivity::class.java)
        intent.putExtra("serviceId", serviceId)
        startActivity(intent)
        finish()
    }

}