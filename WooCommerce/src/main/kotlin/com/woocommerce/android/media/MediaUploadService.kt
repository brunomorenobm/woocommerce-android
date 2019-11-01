package com.woocommerce.android.media

import android.content.Intent
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

/**
 * service which changes a product's image via a two-step process:
 *    1. uploads a device photo to the WP media library
 *    2. when upload completes, assigns the uploaded media to the product
 */
class MediaUploadService : JobIntentService() {
    companion object {
        const val KEY_PRODUCT_ID = "key_product_id"
        const val KEY_LOCAL_MEDIA_URI = "key_media_uri"
        const val STRIP_LOCATION = true

        // array of remoteProductId / localImageUri
        private val currentUploads = LongSparseArray<Uri>()

        class OnProductMediaUploadEvent(
            var remoteProductId: Long,
            val isError: Boolean
        )

        fun isUploadingForProduct(remoteProductId: Long) = currentUploads.containsKey(remoteProductId)

        fun isBusy() = !currentUploads.isEmpty
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap

    private val doneSignal = CountDownLatch(1)

    override fun onCreate() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > created")
        AndroidInjection.inject(this)
        dispatcher.register(this)
        super.onCreate()
    }

    override fun onDestroy() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > destroyed")
        dispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        WooLog.i(WooLog.T.MEDIA, "media upload service > onHandleWork")

        val remoteProductId = intent.getLongExtra(KEY_PRODUCT_ID, 0L)
        val localMediaUri = intent.getParcelableExtra<Uri>(KEY_LOCAL_MEDIA_URI)

        if (localMediaUri == null) {
            WooLog.w(WooLog.T.MEDIA, "media upload service > null localMediaUri")
            handleFailure(remoteProductId)
            return
        }

        val media = MediaUploadUtils.mediaModelFromLocalUri(
                this,
                selectedSite.get().id,
                localMediaUri,
                mediaStore
        )

        media?.let {
            it.postId = remoteProductId
            it.setUploadState(MediaModel.MediaUploadState.UPLOADING)
            currentUploads.put(remoteProductId, localMediaUri)
            dispatchUploadAction(it)
            return
        }

        WooLog.w(WooLog.T.MEDIA, "media upload service > null media")
        handleFailure(remoteProductId)
    }

    override fun onStopCurrentWork(): Boolean {
        super.onStopCurrentWork()
        WooLog.i(WooLog.T.MEDIA, "media upload service > onStopCurrentWork")
        return true
    }

    /**
     * Dispatch the request to upload device image to the WP media library and wait for it to complete
     */
    private fun dispatchUploadAction(media: MediaModel) {
        val site = siteStore.getSiteByLocalId(media.localSiteId)
        val payload = UploadMediaPayload(site, media, STRIP_LOCATION)
        dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
        doneSignal.await()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        val remoteProductId = event.media?.postId ?: 0L
        when {
            event.isError -> {
                WooLog.w(
                        WooLog.T.MEDIA,
                        "MediaUploadService > error uploading media: ${event.error.type}, ${event.error.message}"
                )
                handleFailure(remoteProductId)
            }
            event.canceled -> {
                WooLog.w(WooLog.T.MEDIA, "MediaUploadService > upload media cancelled")
                handleFailure(remoteProductId)
            }
            event.completed -> {
                dispatchEditProductAction(event.media)
                WooLog.i(WooLog.T.MEDIA, "MediaUploadService > uploaded media ${event.media?.id}")
            }
        }
    }

    /**
     * Called after device media has been uploaded to dispatch a request to assign the uploaded media
     * to the product
     */
    private fun dispatchEditProductAction(media: MediaModel) {
        val product = productStore.getProductByRemoteId(selectedSite.get(), media.postId)
        if (product == null) {
            WooLog.w(WooLog.T.MEDIA, "MediaUploadService > product is null")
            handleFailure(media.postId)
        } else {
            val imageList = ArrayList<WCProductImageModel>().also {
                it.add(WCProductImageModel.fromMediaModel(media))
            }

            // make sure we're only replacing the first image
            with(product.getImages()) {
                if (this.size > 1) {
                    this.removeAt(0)
                    imageList.addAll(this)
                }
            }

            val site = siteStore.getSiteByLocalId(media.localSiteId)
            val payload = UpdateProductImagesPayload(site, media.postId, imageList)
            dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        if (event.isError) {
            WooLog.w(
                    WooLog.T.MEDIA,
                    "MediaUploadService > error changing product images: ${event.error.type}, ${event.error.message}"
            )
            handleFailure(event.remoteProductId)
        } else {
            WooLog.i(WooLog.T.MEDIA, "MediaUploadService > product images changed")
            handleSuccess(event.remoteProductId)
        }
    }

    private fun handleSuccess(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductMediaUploadEvent(remoteProductId, isError = false))
        doneSignal.countDown()
        currentUploads.remove(remoteProductId)
        productImageMap.update(remoteProductId)
    }

    private fun handleFailure(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductMediaUploadEvent(remoteProductId, isError = true))
        doneSignal.countDown()
        currentUploads.remove(remoteProductId)
    }
}
