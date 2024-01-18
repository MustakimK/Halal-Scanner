package com.example.halalscanner.mainLogic

data class ProductName(
    val code: String,
    val product: ProductNameDetails,
    val status: Int,
    val status_verbose: String
)

data class ProductNameDetails(
    val product_name: String
)