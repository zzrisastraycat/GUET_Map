package com.example.guet_map.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView

/**
 * 动画工具类
 * 提供通用的动画效果
 */
object AnimationUtils {

    /**
     * 简单的淡入动画
     */
    fun fadeIn(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        view.isVisible = true
        ViewCompat.animate(view)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    onEnd?.invoke()
                }
            })
            .start()
    }

    /**
     * 简单的淡出动画
     */
    fun fadeOut(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        ViewCompat.animate(view)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    view.isVisible = false
                    view.alpha = 1f // 重置透明度
                    onEnd?.invoke()
                }
            })
            .start()
    }

    /**
     * 缩放弹入动画（用于卡片出现）
     */
    fun scaleIn(view: View, duration: Long = 300) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.isVisible = true

        ViewCompat.animate(view)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    /**
     * 缩放弹出动画（用于卡片消失）
     */
    fun scaleOut(view: View, duration: Long = 200, onEnd: (() -> Unit)? = null) {
        ViewCompat.animate(view)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    view.isVisible = false
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.alpha = 1f
                    onEnd?.invoke()
                }
            })
            .start()
    }

    /**
     * 从底部滑入动画
     */
    fun slideInFromBottom(view: View, duration: Long = 300) {
        view.translationY = view.height.toFloat()
        view.isVisible = true

        ViewCompat.animate(view)
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * 向底部滑出动画
     */
    fun slideOutToBottom(view: View, duration: Long = 300, onEnd: (() -> Unit)? = null) {
        ViewCompat.animate(view)
            .translationY(view.height.toFloat())
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    view.isVisible = false
                    view.translationY = 0f
                    onEnd?.invoke()
                }
            })
            .start()
    }

    /**
     * 脉冲动画（用于提示）
     */
    fun pulse(view: View, scale: Float = 1.1f, duration: Long = 150) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, scale, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, scale, 1f)

        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            this.duration = duration * 2
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 摇晃动画（用于错误提示）
     */
    fun shake(view: View, intensity: Float = 10f) {
        val animator = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            0f,
            intensity, -intensity, intensity, -intensity, intensity / 2, -intensity / 2, 0f
        )
        animator.duration = 400
        animator.start()
    }

    /**
     * MaterialCardView 抬起效果
     */
    fun elevateCard(card: MaterialCardView, elevation: Float = 8f, duration: Long = 150) {
        card.animate()
            .translationZ(elevation)
            .setDuration(duration)
            .start()
    }

    /**
     * MaterialCardView 放下效果
     */
    fun flattenCard(card: MaterialCardView, duration: Long = 150) {
        card.animate()
            .translationZ(0f)
            .setDuration(duration)
            .start()
    }

    /**
     * 地图标记弹跳动画
     */
    fun markerBounce(view: View) {
        val animator = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_Y,
            0f, -30f, 0f, -15f, 0f
        )
        animator.duration = 600
        animator.start()
    }

    /**
     * 搜索栏聚焦动画
     */
    fun searchBarFocus(view: View, expanded: Boolean, targetWidth: Int) {
        val width = if (expanded) targetWidth else view.width
        val targetScale = if (expanded) 1.05f else 1f

        ViewCompat.animate(view)
            .scaleX(targetScale)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    /**
     * FAB 显示/隐藏动画
     */
    fun fabShow(view: View, marginBottom: Int = 0) {
        view.isVisible = true
        view.translationY = 100f
        view.animate()
            .translationY(marginBottom.toFloat())
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    fun fabHide(view: View) {
        view.animate()
            .translationY(100f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.isVisible = false
                view.translationY = 0f
            }
            .start()
    }
}
