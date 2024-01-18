package com.example.halalscanner.mainLogic

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsAPI {
    @GET("api/v2/product/{barcode}?fields=product_name")
    fun getProductName(@Path("barcode") barcode: String): Call<ProductName>

    @GET("api/v2/product/{barcode}?fields=ingredients")
    fun getProductIngredients(@Path("barcode") barcode: String): Call<ProductIngredients>

    @GET("api/v2/product/{barcode}?fields=selected_images")
    fun getProductImage(@Path("barcode") barcode: String): Call<ProductIcon>
}