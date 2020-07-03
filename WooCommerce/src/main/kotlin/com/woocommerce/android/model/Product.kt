package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Product(
    val remoteId: Long,
    val name: String,
    val description: String,
    val shortDescription: String,
    val slug: String,
    val type: ProductType,
    val status: ProductStatus?,
    val catalogVisibility: ProductCatalogVisibility?,
    val isFeatured: Boolean,
    val stockStatus: ProductStockStatus,
    val backorderStatus: ProductBackorderStatus,
    val dateCreated: Date,
    val firstImageUrl: String?,
    val totalSales: Int,
    val reviewsAllowed: Boolean,
    val isVirtual: Boolean,
    val ratingCount: Int,
    val averageRating: Float,
    val permalink: String,
    val externalUrl: String,
    val buttonText: String,
    val salePrice: BigDecimal?,
    val regularPrice: BigDecimal?,
    val taxClass: String,
    val manageStock: Boolean,
    val stockQuantity: Int,
    val sku: String,
    val length: Float,
    val width: Float,
    val height: Float,
    val weight: Float,
    val shippingClass: String,
    val shippingClassId: Long,
    val isDownloadable: Boolean,
    val fileCount: Int,
    val downloadLimit: Int,
    val downloadExpiry: Int,
    val purchaseNote: String,
    val numVariations: Int,
    val images: List<Image>,
    val attributes: List<Attribute>,
    val saleEndDateGmt: Date?,
    val saleStartDateGmt: Date?,
    val isOnSale: Boolean,
    val soldIndividually: Boolean,
    val taxStatus: ProductTaxStatus,
    val isSaleScheduled: Boolean,
    val menuOrder: Int,
    val categories: List<ProductCategory>,
    val tags: List<ProductTag>
) : Parcelable {
    companion object {
        const val TAX_CLASS_DEFAULT = "standard"
    }

    @Parcelize
    data class Image(
        val id: Long,
        val name: String,
        val source: String,
        val dateCreated: Date
    ) : Parcelable

    @Parcelize
    data class Attribute(
        val id: Long,
        val name: String,
        val options: List<String>,
        val isVisible: Boolean
    ) : Parcelable

    fun hasCategories() = categories.isNotEmpty()
    fun hasTags() = tags.isNotEmpty()
    fun hasShortDescription() = shortDescription.isNotEmpty()
    fun hasShipping(): Boolean {
        return weight > 0 ||
            length > 0 || width > 0 || height > 0 ||
            shippingClass.isNotEmpty()
    }

    fun isSameProduct(product: Product): Boolean {
        return remoteId == product.remoteId &&
                stockQuantity == product.stockQuantity &&
                stockStatus == product.stockStatus &&
                status == product.status &&
                manageStock == product.manageStock &&
                backorderStatus == product.backorderStatus &&
                soldIndividually == product.soldIndividually &&
                reviewsAllowed == product.reviewsAllowed &&
                sku == product.sku &&
                slug == product.slug &&
                type == product.type &&
                numVariations == product.numVariations &&
                name.fastStripHtml() == product.name.fastStripHtml() &&
                description == product.description &&
                shortDescription == product.shortDescription &&
                taxClass == product.taxClass &&
                taxStatus == product.taxStatus &&
                isSaleScheduled == product.isSaleScheduled &&
                saleEndDateGmt == product.saleEndDateGmt &&
                saleStartDateGmt == product.saleStartDateGmt &&
                isSamePrice(regularPrice, product.regularPrice) &&
                isSamePrice(salePrice, product.salePrice) &&
                weight == product.weight &&
                length == product.length &&
                height == product.height &&
                width == product.width &&
                shippingClass == product.shippingClass &&
                shippingClassId == product.shippingClassId &&
                catalogVisibility == product.catalogVisibility &&
                isFeatured == product.isFeatured &&
                purchaseNote == product.purchaseNote &&
                externalUrl == product.externalUrl &&
                buttonText == product.buttonText &&
                menuOrder == product.menuOrder &&
                isSameImages(product.images) &&
                isSameCategories(product.categories)
    }

    private fun isSamePrice(first: BigDecimal?, second: BigDecimal?): Boolean {
        val val1 = first ?: BigDecimal.ZERO
        val val2 = second ?: BigDecimal.ZERO
        return val1.isEqualTo(val2)
    }

    /**
     * Verifies if there are any changes made to the inventory fields
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasInventoryChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            sku != it.sku ||
                    manageStock != it.manageStock ||
                    stockStatus != it.stockStatus ||
                    stockQuantity != it.stockQuantity ||
                    backorderStatus != it.backorderStatus ||
                    soldIndividually != it.soldIndividually
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the pricing fields
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasPricingChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            regularPrice.isNotEqualTo(it.regularPrice) ||
                    salePrice.isNotEqualTo(it.salePrice) ||
                    saleStartDateGmt != it.saleStartDateGmt ||
                    saleEndDateGmt != it.saleEndDateGmt ||
                    isOnSale != it.isOnSale ||
                    taxClass != it.taxClass ||
                    taxStatus != it.taxStatus
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the shipping fields
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasShippingChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            weight != it.weight ||
                    length != it.length ||
                    width != it.width ||
                    height != it.height ||
                    shippingClass != it.shippingClass
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product images
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasImageChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            !isSameImages(it.images)
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the external link settings
     */
    fun hasExternalLinkChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            externalUrl != it.externalUrl ||
                    buttonText != it.buttonText
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product settings
     */
    fun hasSettingsChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            status != it.status ||
                    catalogVisibility != it.catalogVisibility ||
                    isFeatured != it.isFeatured ||
                    slug != it.slug ||
                    reviewsAllowed != it.reviewsAllowed ||
                    purchaseNote != it.purchaseNote ||
                    menuOrder != it.menuOrder
        } ?: false
    }

    /**
     * Verifies if there are any changes made to the product categories
     * by comparing the updated product model ([updatedProduct]) with the product model stored
     * in the local db and returns a [Boolean] flag
     */
    fun hasCategoryChanges(updatedProduct: Product?): Boolean {
        return updatedProduct?.let {
            !isSameCategories(it.categories)
        } ?: false
    }

    /**
     * Compares this product's images with the passed list, returns true only if both lists contain
     * the same images in the same order
     */
    private fun isSameImages(updatedImages: List<Image>): Boolean {
        if (this.images.size != updatedImages.size) {
            return false
        }
        for (i in images.indices) {
            if (images[i].id != updatedImages[i].id) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this product's categories with the passed list, returns true only if both lists contain
     * the same categories
     */
    private fun isSameCategories(updatedCategories: List<ProductCategory>): Boolean {
        if (this.categories.size != updatedCategories.size) {
            return false
        }

        categories.forEach {
            if (!updatedCategories.containsCategory(it)) {
                return false
            }
        }
        return true
    }

    /**
     * Method merges the updated product fields edited by the user with the locally cached
     * [Product] model and returns the updated [Product] model.
     *
     * [newProduct] includes the updated product fields edited by the user.
     * if [newProduct] is not null, a copy of the stored [Product] model is created
     * and product fields edited by the user and added to this model before returning it
     *
     */
    fun mergeProduct(newProduct: Product?): Product {
        return newProduct?.let { updatedProduct ->
            this.copy(
                    description = updatedProduct.description,
                    shortDescription = updatedProduct.shortDescription,
                    name = updatedProduct.name,
                    sku = updatedProduct.sku,
                    slug = updatedProduct.slug,
                    status = updatedProduct.status,
                    catalogVisibility = updatedProduct.catalogVisibility,
                    isFeatured = updatedProduct.isFeatured,
                    manageStock = updatedProduct.manageStock,
                    stockStatus = updatedProduct.stockStatus,
                    stockQuantity = updatedProduct.stockQuantity,
                    backorderStatus = updatedProduct.backorderStatus,
                    soldIndividually = updatedProduct.soldIndividually,
                    regularPrice = updatedProduct.regularPrice,
                    salePrice = updatedProduct.salePrice,
                    isOnSale = updatedProduct.isOnSale,
                    isSaleScheduled = updatedProduct.isSaleScheduled,
                    saleStartDateGmt = updatedProduct.saleStartDateGmt,
                    saleEndDateGmt = updatedProduct.saleEndDateGmt,
                    taxStatus = updatedProduct.taxStatus,
                    taxClass = updatedProduct.taxClass,
                    length = updatedProduct.length,
                    width = updatedProduct.width,
                    height = updatedProduct.height,
                    weight = updatedProduct.weight,
                    shippingClass = updatedProduct.shippingClass,
                    images = updatedProduct.images,
                    shippingClassId = updatedProduct.shippingClassId,
                    reviewsAllowed = updatedProduct.reviewsAllowed,
                    purchaseNote = updatedProduct.purchaseNote,
                    externalUrl = updatedProduct.externalUrl,
                    buttonText = updatedProduct.buttonText,
                    menuOrder = updatedProduct.menuOrder,
                    categories = updatedProduct.categories
            )
        } ?: this.copy()
    }

    /**
     * Formats the [Product] weight with the given [weightUnit]
     * for display purposes.
     * Eg: 12oz
     */
    fun getWeightWithUnits(weightUnit: String?): String {
        return if (weight > 0) {
            "${weight.formatToString()}${weightUnit ?: ""}"
        } else ""
    }

    /**
     * Formats the [Product] size (length, width, height) with the given [dimensionUnit]
     * if all the dimensions are available.
     * Eg: 12 x 15 x 13 in
     */
    fun getSizeWithUnits(dimensionUnit: String?): String {
        val hasLength = length > 0
        val hasWidth = width > 0
        val hasHeight = height > 0
        val unit = dimensionUnit ?: ""
        return if (hasLength && hasWidth && hasHeight) {
            "${length.formatToString()} " +
                    "x ${width.formatToString()} " +
                    "x ${height.formatToString()} $unit"
        } else if (hasWidth && hasHeight) {
            "${width.formatToString()} x ${height.formatToString()} $unit"
        } else {
            ""
        }.trim()
    }
}

fun Product.toDataModel(storedProductModel: WCProductModel?): WCProductModel {
    fun imagesToJson(): String {
        val jsonArray = JsonArray()
        for (image in images) {
            jsonArray.add(JsonObject().also { json ->
                json.addProperty("id", image.id)
                json.addProperty("name", image.name)
                json.addProperty("source", image.source)
            })
        }
        return jsonArray.toString()
    }

    fun categoriesToJson(): String {
        val jsonArray = JsonArray()
        for (category in categories) {
            jsonArray.add(JsonObject().also { json ->
                json.addProperty("id", category.remoteCategoryId)
                json.addProperty("name", category.name)
                json.addProperty("slug", category.slug)
            })
        }
        return jsonArray.toString()
    }

    return (storedProductModel ?: WCProductModel()).also {
        it.remoteProductId = remoteId
        it.description = description
        it.shortDescription = shortDescription
        it.name = name
        it.sku = sku
        it.slug = slug
        it.status = status.toString()
        it.catalogVisibility = catalogVisibility.toString()
        it.featured = isFeatured
        it.manageStock = manageStock
        it.stockStatus = ProductStockStatus.fromStockStatus(stockStatus)
        it.stockQuantity = stockQuantity
        it.soldIndividually = soldIndividually
        it.backorders = ProductBackorderStatus.fromBackorderStatus(backorderStatus)
        it.regularPrice = if (regularPrice isEqualTo BigDecimal.ZERO) "" else regularPrice.toString()
        it.salePrice = if (salePrice isEqualTo BigDecimal.ZERO) "" else salePrice.toString()
        it.onSale = isOnSale
        it.length = if (length == 0f) "" else length.formatToString()
        it.width = if (width == 0f) "" else width.formatToString()
        it.weight = if (weight == 0f) "" else weight.formatToString()
        it.height = if (height == 0f) "" else height.formatToString()
        it.shippingClass = shippingClass
        it.taxStatus = ProductTaxStatus.fromTaxStatus(taxStatus)
        it.taxClass = taxClass
        it.images = imagesToJson()
        it.reviewsAllowed = reviewsAllowed
        if (isSaleScheduled) {
            saleStartDateGmt?.let { dateOnSaleFrom ->
                it.dateOnSaleFromGmt = dateOnSaleFrom.formatToYYYYmmDDhhmmss()
            }
            it.dateOnSaleToGmt = saleEndDateGmt?.formatToYYYYmmDDhhmmss() ?: ""
        } else {
            it.dateOnSaleFromGmt = ""
            it.dateOnSaleToGmt = ""
        }
        it.purchaseNote = purchaseNote
        it.externalUrl = externalUrl
        it.buttonText = buttonText
        it.menuOrder = menuOrder
        it.categories = categoriesToJson()
    }
}

fun WCProductModel.toAppModel(): Product {
    return Product(
        remoteId = this.remoteProductId,
        name = this.name,
        description = this.description,
        shortDescription = this.shortDescription,
        type = ProductType.fromString(this.type),
        status = ProductStatus.fromString(this.status),
        catalogVisibility = ProductCatalogVisibility.fromString(this.catalogVisibility),
        isFeatured = this.featured,
        stockStatus = ProductStockStatus.fromString(this.stockStatus),
        backorderStatus = ProductBackorderStatus.fromString(this.backorders),
        dateCreated = DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date(),
        firstImageUrl = this.getFirstImageUrl(),
        totalSales = this.totalSales,
        reviewsAllowed = this.reviewsAllowed,
        isVirtual = this.virtual,
        ratingCount = this.ratingCount,
        averageRating = this.averageRating.toFloatOrNull() ?: 0f,
        permalink = this.permalink,
        externalUrl = this.externalUrl,
        buttonText = this.buttonText,
        salePrice = this.salePrice.toBigDecimalOrNull()?.roundError(),
        regularPrice = this.regularPrice.toBigDecimalOrNull()?.roundError(),
            // In Core, if a tax class is empty it is considered as standard and we are following the same
            // procedure here
        taxClass = if (this.taxClass.isEmpty()) Product.TAX_CLASS_DEFAULT else this.taxClass,
        manageStock = this.manageStock,
        stockQuantity = this.stockQuantity,
        sku = this.sku,
        slug = this.slug,
        length = this.length.toFloatOrNull() ?: 0f,
        width = this.width.toFloatOrNull() ?: 0f,
        height = this.height.toFloatOrNull() ?: 0f,
        weight = this.weight.toFloatOrNull() ?: 0f,
        shippingClass = this.shippingClass,
        shippingClassId = this.shippingClassId.toLong(),
        isDownloadable = this.downloadable,
        fileCount = this.getDownloadableFiles().size,
        downloadLimit = this.downloadLimit,
        downloadExpiry = this.downloadExpiry,
        purchaseNote = this.purchaseNote,
        numVariations = this.getNumVariations(),
        images = this.getImages().map {
            Product.Image(
                    it.id,
                    it.name,
                    it.src,
                    DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date()
            )
        },
        attributes = this.getAttributes().map {
            Product.Attribute(
                    it.id,
                    it.name,
                    it.options,
                    it.visible
            )
        },
        saleEndDateGmt = this.dateOnSaleToGmt.formatDateToISO8601Format(),
        saleStartDateGmt = this.dateOnSaleFromGmt.formatDateToISO8601Format(),
        isOnSale = this.onSale,
        soldIndividually = this.soldIndividually,
        taxStatus = ProductTaxStatus.fromString(this.taxStatus),
        isSaleScheduled = this.dateOnSaleFromGmt.isNotEmpty() || this.dateOnSaleToGmt.isNotEmpty(),
        menuOrder = this.menuOrder,
        categories = this.getCategories().map {
            ProductCategory(
                it.id,
                it.name,
                it.slug
            )
        },
        tags = this.getTags().map {
            ProductTag(
                it.id,
                it.name,
                it.slug
            )
        }
    )
}

fun MediaModel.toAppModel(): Product.Image {
    return Product.Image(
            id = this.mediaId,
            name = this.fileName,
            source = this.url,
            dateCreated = DateTimeUtils.dateFromIso8601(this.uploadDate)
    )
}

/**
 * Returns the product as a [ProductReviewProduct] for use with the product reviews feature.
 */
fun WCProductModel.toProductReviewProductModel() =
        ProductReviewProduct(this.remoteProductId, this.name, this.permalink)
