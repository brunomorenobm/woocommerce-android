package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCategories
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductShortDescriptionEditor
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.viewmodel.ResourceProvider

class ProductDetailBottomSheetBuilder(
    private val viewModel: ProductDetailViewModel,
    private val resources: ResourceProvider
) {
    enum class ProductDetailBottomSheetType(
        @StringRes val titleResource: Int,
        @StringRes val descResource: Int
    ) {
        PRODUCT_SHIPPING(string.product_shipping, string.product_shipping_desc),
        PRODUCT_CATEGORIES(string.product_categories, string.product_categories_desc),
        PRODUCT_TAGS(string.product_tags, string.product_tags_desc),
        SHORT_DESCRIPTION(string.product_short_description, string.product_short_description_desc)
    }

    data class ProductDetailBottomSheetUiItem(
        val type: ProductDetailBottomSheetType,
        val clickEvent: ProductNavigationTarget,
        val stat: Stat? = null
    )

    fun buildBottomSheetList(product: Product): List<ProductDetailBottomSheetUiItem> {
        return when (product.type) {
            SIMPLE -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription()
                )
            }
            EXTERNAL -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription()
                )
            }
            GROUPED -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription()
                )
            }
            VARIABLE -> {
                listOfNotNull(
                    product.getShipping(),
                    product.getCategories(),
                    product.getTags(),
                    product.getShortDescription()
                )
            }
        }
    }

    private fun Product.getShipping(): ProductDetailBottomSheetUiItem? {
        return if (!hasShipping()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_SHIPPING,
                ViewProductShipping(remoteId),
                Stat.PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getCategories(): ProductDetailBottomSheetUiItem? {
        return if (!hasCategories()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_CATEGORIES,
                ViewProductCategories(remoteId),
                Stat.PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED
            )
        } else {
            null
        }
    }

    private fun Product.getTags(): ProductDetailBottomSheetUiItem? {
        return if (!hasTags()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.PRODUCT_TAGS,
                ViewProductCategories(remoteId)
            )
        } else {
            null
        }
    }

    private fun Product.getShortDescription(): ProductDetailBottomSheetUiItem? {
        return if (!hasShortDescription()) {
            ProductDetailBottomSheetUiItem(
                ProductDetailBottomSheetType.SHORT_DESCRIPTION,
                ViewProductShortDescriptionEditor(
                    shortDescription,
                    resources.getString(string.product_short_description)
                ),
                Stat.PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED
            )
        } else {
            null
        }
    }
}
