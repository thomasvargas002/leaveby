package com.example.leaveby.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

private fun calcInSampleSize(srcW: Int, srcH: Int, maxDim: Int): Int {
    var inSampleSize = 1
    if (srcH > maxDim || srcW > maxDim) {
        var halfH = srcH / 2
        var halfW = srcW / 2
        while ((halfH / inSampleSize) >= maxDim && (halfW / inSampleSize) >= maxDim) {
            inSampleSize *= 2
        }
    }
    return max(1, inSampleSize)
}

private fun rotateIfNeeded(bmp: Bitmap, exif: ExifInterface?): Bitmap {
    val orientation = exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    ) ?: ExifInterface.ORIENTATION_NORMAL

    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_TRANSPOSE  -> { m.postRotate(90f);  m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL   -> m.postScale(1f, -1f)
        else -> return bmp
    }

    return try {
        val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (out != bmp && !bmp.isRecycled) bmp.recycle()
        out
    } catch (_: Throwable) {
        bmp
    }
}

/** Decode a photo picker image to a safe size (<= maxDim) using RGB_565 to save RAM. */
fun loadScaledBitmap(ctx: Context, uri: Uri, maxDim: Int = 1600): Bitmap {
    // Read EXIF first (orientation)
    val exif = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
    }.getOrNull()

    // 1) Bounds pass
    val opts1 = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts1) }
    val (srcW, srcH) = opts1.outWidth to opts1.outHeight
    val sample = calcInSampleSize(srcW, srcH, maxDim)

    // 2) Decode pass
    val opts2 = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565 // ~½ memory vs ARGB_8888
    }
    val decoded = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts2)
    } ?: throw IllegalStateException("Failed to decode image")

    // 3) Fix orientation
    return rotateIfNeeded(decoded, exif)
}
