package com.wx.tools.view.base

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.baidu.mobads.action.BaiduAction
import com.baidu.mobads.action.PrivacyStatus
import com.wx.tools.config.Constant
import com.wx.tools.utils.LivePermissions
import com.wx.tools.utils.PermissionResult
import com.wx.tools.utils.RomUtil
import com.wx.tools.utils.ToastUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseFragment : Fragment(), CoroutineScope by MainScope(), View.OnClickListener {
    private var mContext: Context? = null
    private var mHandler: Handler? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = initView(inflater, container, savedInstanceState)
        initData()
        return v
    }

    override fun onClick(v: View) {
        click(v)
    }

    protected val handler: Handler
        get() {
            if (mHandler == null) {
                mHandler = Handler(Looper.getMainLooper())
            }
            return mHandler!!
        }

    fun onActivityResume() {}
    protected abstract fun initView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    protected abstract fun initData()
    protected abstract fun click(v: View?)


    protected fun checkPermissions(method: () -> Unit) {
        LivePermissions(this).request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        ).observe(this, {
            when (it) {
                is PermissionResult.Grant -> {
                    //????????????
                    method()
                    if (!RomUtil.isOppo() && Constant.OCPC) {
                        BaiduAction.setPrivacyStatus(PrivacyStatus.AGREE)
                    }
                }

                is PermissionResult.Rationale -> {
                    //????????????
                    ToastUtil.showShort(context, "?????????????????????????????????????????????????????????")
                    it.permissions.forEach { s ->
                        println("Rationale:${s}")//??????????????????
                    }
                    if (!RomUtil.isOppo() && Constant.OCPC) {
                        BaiduAction.setPrivacyStatus(PrivacyStatus.DISAGREE)
                    }
                }

                is PermissionResult.Deny -> {
                    ToastUtil.showShort(context, "?????????????????????????????????????????????????????????")
                    //???????????????????????????????????????
                    it.permissions.forEach { s ->
                        println("deny:${s}")//??????????????????
                    }
                }
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}