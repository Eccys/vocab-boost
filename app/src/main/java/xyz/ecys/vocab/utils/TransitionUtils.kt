package xyz.ecys.vocab.utils

import android.app.Activity

/**
 * Utility class for managing activity transitions
 */
object TransitionUtils {

    /**
     * Standard transition for all activities - combines zoom and fade effects
     * Use this for all activity transitions for consistent user experience
     */
    fun applyStandardTransition(activity: Activity) {
        // Using the zoom transition for all activities
        activity.overridePendingTransition(
            activity.resources.getIdentifier("zoom_in", "anim", activity.packageName),
            activity.resources.getIdentifier("fade_out", "anim", activity.packageName)
        )
    }

    /**
     * Standard transition for finishing activities - combines zoom and fade effects
     * Use this for all activity finish transitions for consistent user experience
     */
    fun applyStandardTransitionOnFinish(activity: Activity) {
        // Using the zoom transition for all finish transitions
        activity.overridePendingTransition(
            activity.resources.getIdentifier("fade_in", "anim", activity.packageName),
            activity.resources.getIdentifier("zoom_out", "anim", activity.packageName)
        )
    }

    // Keeping legacy transition methods for compatibility, but these should be replaced with standard transitions

    /**
     * Applies a slide transition when starting an activity
     * (current activity slides out to the left, new activity slides in from the right)
     * @deprecated Use applyStandardTransition instead
     */
    fun applySlideTransition(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransition(activity)
    }

    /**
     * Applies a slide transition when finishing an activity
     * (current activity slides out to the right, previous activity slides in from the left)
     * @deprecated Use applyStandardTransitionOnFinish instead
     */
    fun applySlideTransitionOnFinish(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransitionOnFinish(activity)
    }

    /**
     * Applies a zoom transition when starting an activity
     * (current activity zooms out, new activity zooms in)
     * @deprecated Use applyStandardTransition instead
     */
    fun applyZoomTransition(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransition(activity)
    }

    /**
     * Applies a zoom transition when finishing an activity
     * (current activity zooms out, previous activity zooms in)
     * @deprecated Use applyStandardTransitionOnFinish instead
     */
    fun applyZoomTransitionOnFinish(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransitionOnFinish(activity)
    }

    /**
     * Applies a fade transition when starting an activity
     * (current activity fades out, new activity fades in)
     * @deprecated Use applyStandardTransition instead
     */
    fun applyFadeTransition(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransition(activity)
    }

    /**
     * Applies a fade transition when finishing an activity
     * (current activity fades out, previous activity fades in)
     * @deprecated Use applyStandardTransitionOnFinish instead
     */
    fun applyFadeTransitionOnFinish(activity: Activity) {
        // For backwards compatibility, simply call standard transition
        applyStandardTransitionOnFinish(activity)
    }
} 