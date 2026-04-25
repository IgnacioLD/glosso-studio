package me.shirobyte42.glosso.util

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.play.core.review.ReviewManagerFactory

fun launchInAppReview(context: Context) {
    try {
        val reviewManager = ReviewManagerFactory.create(context)
        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                (context as? ComponentActivity)?.let {
                    reviewManager.launchReviewFlow(it, request.result)
                }
            }
        }
    } catch (_: Exception) {
    }
}
