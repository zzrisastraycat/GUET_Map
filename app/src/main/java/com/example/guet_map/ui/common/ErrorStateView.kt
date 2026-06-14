package com.example.guet_map.ui.common

import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.guet_map.R

/**
 * 统一的错误状态展示组件
 */
object ErrorStateView {

    /**
     * 显示错误状态的通用方法
     */
    fun show(
        view: View,
        @StringRes messageRes: Int,
        @DrawableRes iconRes: Int = R.drawable.ic_error,
        onRetry: (() -> Unit)? = null
    ) {
        show(view, view.context.getString(messageRes), iconRes, onRetry)
    }

    /**
     * 显示错误状态的通用方法
     */
    fun show(
        view: View,
        message: String,
        @DrawableRes iconRes: Int = R.drawable.ic_error,
        onRetry: (() -> Unit)? = null
    ) {
        val iconView = view.findViewById<View>(R.id.ivErrorIcon)
        val messageView = view.findViewById<TextView>(R.id.tvErrorMessage)
        val retryView = view.findViewById<View>(R.id.btnRetry)

        iconView?.let {
            if (iconRes != 0) {
                (it as? android.widget.ImageView)?.setImageResource(iconRes)
                it.isVisible = true
            } else {
                it.isVisible = false
            }
        }

        messageView?.text = message
        messageView?.isVisible = true

        retryView?.isVisible = onRetry != null
        retryView?.setOnClickListener { onRetry?.invoke() }

        view.isVisible = true
    }

    /**
     * 隐藏错误状态
     */
    fun hide(view: View) {
        view.isVisible = false
    }

    /**
     * 显示网络错误
     */
    fun showNetworkError(view: View, onRetry: (() -> Unit)? = null) {
        show(view, R.string.error_network, R.drawable.ic_error_network, onRetry)
    }

    /**
     * 显示定位错误
     */
    fun showLocationError(view: View, onRetry: (() -> Unit)? = null) {
        show(view, R.string.error_location, R.drawable.ic_error_location, onRetry)
    }

    /**
     * 显示空状态
     */
    fun showEmpty(view: View, @StringRes messageRes: Int) {
        val iconView = view.findViewById<View>(R.id.ivErrorIcon)
        val messageView = view.findViewById<TextView>(R.id.tvErrorMessage)
        val retryView = view.findViewById<View>(R.id.btnRetry)

        iconView?.let {
            if (it is android.widget.ImageView) {
                it.setImageResource(R.drawable.ic_empty)
            }
        }
        messageView?.setText(messageRes)
        messageView?.isVisible = true
        retryView?.isVisible = false

        view.isVisible = true
    }
}

/**
 * 扩展函数：更便捷的错误显示
 */
fun View.showError(message: String, onRetry: (() -> Unit)? = null) {
    ErrorStateView.show(this, message, onRetry = onRetry)
}

fun View.showNetworkError(onRetry: (() -> Unit)? = null) {
    ErrorStateView.showNetworkError(this, onRetry)
}

/**
 * 吐司扩展
 */
fun View.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}

fun View.showToast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, messageRes, duration).show()
}
