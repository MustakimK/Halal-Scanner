package com.example.halalscanner.mainLogic

data class ProductIngredients(
    val code: String,
    val product: ProductIngredientsDetails,
    val status: Int,
    val status_verbose: String
)

data class ProductIngredientsDetails(
    val ingredients: List<Ingredient>
)

data class Ingredient(
    val id: String?,
    val percent_estimate: Double?,
    val percent_max: Double?,
    val percent_min: Double?,
    val text: String,
    val vegan: String?,
    val vegetarian: String?,
    val ciqual_food_code: String?,
    val from_palm_oil: String?
)

