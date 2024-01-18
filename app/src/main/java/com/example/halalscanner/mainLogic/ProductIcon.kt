package com.example.halalscanner.mainLogic

data class ProductIcon(
    val code: String,
    val product: Product
)

data class Product(
    val selected_images: SelectedImages
)

data class SelectedImages(
    val front: ImageData
)

data class ImageData(
    val thumb: Map<String, String>
)