package com.keshtoim.forge.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class PhotoPrepTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun file(name: String, write: (File) -> Unit) = File(context.cacheDir, name).also(write).let(Uri::fromFile)

    @Test
    fun `a large photo is scaled down to the long side limit as a jpeg`() {
        val uri = file("big.png") { f -> f.outputStream().use { Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) } }

        val bytes = runBlocking { preparePhoto(context.contentResolver, uri) }!!

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        assertEquals(PHOTO_MAX_SIDE to 853, bounds.outWidth to bounds.outHeight)
        assertEquals("image/jpeg", bounds.outMimeType)
    }

    @Test
    fun `a small photo keeps its size and a non-image is rejected`() {
        val small = file("small.png") { f -> f.outputStream().use { Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) } }
        val bytes = runBlocking { preparePhoto(context.contentResolver, small) }!!
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        assertEquals(400 to 300, bounds.outWidth to bounds.outHeight)

        val text = file("note.txt") { it.writeText("not a photo") }
        assertNull(runBlocking { preparePhoto(context.contentResolver, text) })
    }

    @Test
    fun `a camera shot marked as rotated comes out upright`() {
        val uri = file("camera.jpg") { f ->
            f.outputStream().use { Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it) }
            ExifInterface(f.path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()) }.saveAttributes()
        }
        val bytes = runBlocking { preparePhoto(context.contentResolver, uri) }!!
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        assertEquals(300 to 400, bounds.outWidth to bounds.outHeight)
    }
}
