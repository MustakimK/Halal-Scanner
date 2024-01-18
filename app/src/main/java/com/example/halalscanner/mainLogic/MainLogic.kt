package com.example.halalscanner.mainLogic

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.Room
import com.example.halalscanner.history.DatabaseManager
import com.example.halalscanner.history.HistoryActivity
import com.example.halalscanner.history.HistoryData
import com.example.halalscanner.history.HistoryDatabase
import com.google.android.gms.tasks.Tasks
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainLogic(private val context: Context) {

    // Retrofit instance for making network requests
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Service instance for accessing the OpenFoodFactsAPI
    private val service = retrofit.create(OpenFoodFactsAPI::class.java)

    // Function to check if the device is connected to the internet
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Function to get product name using the provided barcode
    suspend fun getProductName(barcode: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Check if the device is connected to the internet
                if (!isNetworkAvailable()) {
                    Log.i("NO_INTERNET", "No internet connection")
                    return@withContext "No internet connection"
                }

                // Make the network request
                val response = service.getProductName(barcode).execute()

                // Check if the request was successful
                when {
                    //Product was not found
                    response.code() == 404 -> {
                        Log.i("PRODUCT_NOT_FOUND", "Product not found")
                        return@withContext "Product not found"
                    }
                    response.isSuccessful -> {
                        // Return the product name if it exists, otherwise return an empty string
                        response.body()?.product?.product_name ?: ""
                    }
                    else -> {
                        Log.i("API_DOWN", "API is down")
                        return@withContext "API is down"
                    }
                }
            } catch (e: Exception) {
                Log.i("ERROR", "Failed to get product name: ${e.message}")
                return@withContext "Failed to get product name"
            }
        }
    }

    // This function is a coroutine that returns a list of product ingredients
    suspend fun getProductIngredients(barcode: String): List<String>? {
        // withContext switches the coroutine context to Dispatchers.IO for network operations
        return withContext(Dispatchers.IO) {
            try {
                // Check if the device is connected to the internet
                if (!isNetworkAvailable()) {
                    // Log an error message and return a list containing "No internet connection"
                    Log.i("NO_INTERNET", "No internet connection")
                    return@withContext listOf("No internet connection")
                }

                // Execute the network request
                val response = service.getProductIngredients(barcode).execute()

                // Check if the request was successful
                if (response.isSuccessful) {
                    // Get the list of ingredients from the response
                    val ingredients = response.body()?.product?.ingredients

                    // If the ingredients are null or empty, log an error message and return a list containing "Ingredients not found"
                    if (ingredients.isNullOrEmpty()) {
                        Log.i("INGREDIENTS_NOT_FOUND", "Ingredients not found")
                        return@withContext listOf("Ingredients not found")
                    } else {
                        // Map the list of ingredients to their text, replacing certain characters and words
                        val ingredientTexts = ingredients.map {
                            it.text.replace("_", "")
                                .replace("-", " ")
                                .replace(" or", " ")
                                .replace(" or ", " ")
                                .replace(" and", " ")
                                .replace(" and ", " ")
                        }

                        // Join the ingredient texts into a single string
                        val ingredientString = ingredientTexts.joinToString(", ")

                        // Get a language identifier client
                        val languageIdentifier = LanguageIdentification.getClient()

                        // Identify the language of the ingredient string
                        val identifiedLanguage = Tasks.await(languageIdentifier.identifyLanguage(ingredientString))

                        // If the identified language is English, return the ingredient texts
                        if (identifiedLanguage == "en") {
                            ingredientTexts
                        } else {
                            // Otherwise, build translator options for translating from the identified language to English
                            val options = TranslatorOptions.Builder()
                                .setSourceLanguage(identifiedLanguage)
                                .setTargetLanguage("en")
                                .build()

                            // Get a translation client with the translator options
                            val translator = Translation.getClient(options)

                            // Download the translation model if needed
                            Tasks.await(translator.downloadModelIfNeeded())

                            // Translate the ingredient string to English
                            val translatedText = Tasks.await(translator.translate(ingredientString))

                            // Split the translated text into a list of ingredients
                            val translatedIngredients = translatedText.split(", ")

                            // Map the list of translated ingredients, replacing certain words
                            val cleanedIngredients = translatedIngredients.map {
                                it.replace(" or", " ")
                                    .replace(" or ", " ")
                                    .replace(" and", " ")
                                    .replace(" and ", " ")
                            }

                            // Return the list of cleaned, translated ingredients
                            cleanedIngredients
                        }
                    }
                } else {
                    // If the request was not successful, log an error message and return a list containing "API is down"
                    Log.i("API_DOWN", "API is down")
                    listOf("API is down")
                }
            } catch (e: Exception) {
                // If an exception occurs, log an error message and return a list containing "Failed to get ingredients"
                Log.i("ERROR", "Failed to get ingredients: ${e.message}")
                listOf("Failed to get ingredients")
            }
        }
    }



    //function to get the image url
    fun getProductIcon(barcode: String): String {
        return try {
            // Make the network request
            val response = service.getProductImage(barcode).execute()

            // If the request was successful, return the product icon
            // Otherwise, return an empty string
            if (response.isSuccessful) {
                response.body()?.product?.selected_images?.front?.thumb?.get("en") ?: ""
            } else {
                Log.i("API_DOWN", "API is down")
                ""
            }
        } catch (e: Exception) {
            // Log an error message if the network request fails
            Log.i("ERROR", "Failed to get product icon: ${e.message}")
            ""
        }
    }


    //List of items that are 100% haram
    private val haramList: List<String> = listOf("l cystine", "l cysteine", "cochineal", "carminic acid", "gelatine", "gelatine emulsifier",
        "gelatin", "shellac", "essentiale calcium phosphate", "bone phosphate", "tribasic")

    //List of items that may come from haram sources
    private val maybeList: List<String> = listOf("wax esters", "magnesium stearate", "calcium stearate", "stearic acid", "stearic acid fatty acid",
        "sorbitan monostearate", "sorbitan tristearate", "sorbitan monolaurate", "sorbitan monooleate", "sorbitan monopalminate", "stearyl tartrate",
        "lactylated fatty acids esters of glycerol and propane", "glyceryl monostearate", "glyceryl distearate", "sucrose of fatty acids", "polyglycerol esters of fatty acids",
        "propylene gycol esters of fatty acids", "glycerol esters of wood rosins", "ammonium phosphatides", "polyoxyethene 8 stearate", "polyoxyethene 40 stearate",
        "polyoxyethene 20 stearate", "dodecyl gallate", "sodium erythorbate", "sodium erythorbin", "lecithin", "sodium lactate", "potassium lactate",
        "potassium lactate antioxidant", "calcium lactate", "magnesium lactate", "fatty acid of esters of ascorbic acid", "lactic acid", "lactic acid preservative",
        "potassium nitrate", "black 7984", "orange ggn", "citrus red 2", "ponceau sx", "scarlet gn", "ponceau 6r", "indanthrene blue rs", "", "emulsifiers", "emulsifier",
        "edible bone phosphate", "glycerol", "glycerin", "glycerine","curcumin", "turmeric", "riboflavin", "vitamin b2", "ammonium phosphates", "monoammonium phosphate",
        "diammonium phosphate", "lecitin citrate", "magnesium citrate", "ammonium malate", "calcium glycerylphosphate", "isopropyl citrate","disodium ethylene diamine",
        "oxystearin, thiodipropionic acid", "dilauryl thiodipropionate", "distearyl thiodipropionate","phytic acid", "extracts of rosemary", "calcium lactobionate",
        "bakers yeast", "arabinogalactan", "oat gum thickener", "gum ghatti thickener", "curdlan", "dioctyl sodium sulphosuccinate", "stearyl citrate",
        "sodium stearoyl fumarate", "calcium stearoyl fumarate", "sodium laurylsulphate", "ethoxylated monoglycerides", "ethoxylated diglycerides",
        "methyl glucoside coconut oil", "sorbitan trioleate", "polyoxypropylene polyoxyethylene", "partial polyglycerol esters of polycondensed fatty acids of castor oil",
        "ferrous carbonate", "ferrous hexacyanomanganate", "sodium thiosulphate", "dicalcium diphosphate acidity regulator", "sepiolite", "sepiolitic clay", "natrolite phonolite",
        "magnesium gluconate", "4 hexylresorcinol", "synthetic calcium aluminates", "perlite", "inosinic acid", "disodium inosinate", "dipotassium inosinate",
        "calcium inosinate", "calcium 5 ribonucleotides", "disodium 5 ribonucleotides", "maltol", "ethyl maltol", "glycine and its sodium salt", "l leucine",
        "lysine hydrochloride", "zinc acetate", "bacitracin", "penicillin g benzathyne", "spiramycins", "virginiamicins", "flavophospholipol", "tylosin",
        "tetracyclines", "chlortetracycline", "oxytetracycline", "oleandomycin", "penicillin g potassium", "penicillin g sodium", "penicillin g procaine",
        "inosinic acid", "disodium inosinate", "dipotassium inosinate", "calcium inosinate", "calcium 5 ribonucleotides", "disodium 5 ribonucleotides", "maltol", "ethyl maltol",
        "glycine and its sodium salt", "l leucine", "lysine hydrochloride", "zinc acetate", "bacitracin", "penicillin g benzathyne", "spiramycins", "virginiamicins",
        "flavophospholipol", "tylosin", "tetracyclines", "chlortetracycline", "oxytetracycline", "oleandomycin", "penicillin g potassium", "penicillin g sodium", "penicillin g procaine",
        "monensin", "avoparcin", "salinomycin", "avilamycin", "gum benzoic", "rice bran wax", "spermaceti wax", "methyl esters of fatty acids", "oxidized polyethylene wax",
        "calcium iodate", "potassium iodate", "nitrogen oxides", "nitrosyl chloride", "potassium persulphate", "ammonium persulphate", "potassium bromate", "acetone peroxide",
        "monensin", "avoparcin", "salinomycin", "avilamycin", "gum benzoic", "rice bran wax", "spermaceti wax", "methyl esters of fatty acids", "oxidized polyethylene wax",
        "calcium iodate", "potassium iodate", "nitrogen oxides", "nitrosyl chloride", "potassium persulphate", "ammonium persulphate", "potassium bromate", "acetone peroxide",
        "dichlorodifluoromethane", "propane", "chloropentafluoroethane", "octafluorocyclobutane", "sucralose", "alitame", "glycyrrhizin sweetener", "stevioside",
        "neotame", "aspartame acesulfame salt", "erythritol","amylase", "protease", "papain", "bromelian", "ficin", "glucose oxidase", "lipases",
         "polyvinyl alcohol", "pullulan", "enzyme treated starch", "distarch glycerol", "acetylated distarch glycerol",
         "fast green fcf", "fd&c green 3", "distarch glycerine", "hydroxy propyl distarch glycerol", "acetylated oxidised starch", "starch aluminum octenyl",
         "ethanol", "glyceryl monoacetate", "benzyl alcohol", "polythylene glycol 8000", "hydroxyetheyl cellulose", "saffron", "sandalwood red", "tannin",
         "orcein", "orchil", "heptyl p hydroxybenzoate", "dehydroacetic acid", "sodium dehydroacetate", "ascorbyl stearate", "erythorbin acid",
         "tert butylhydroquinone tbhq", "tert butylhydroquinone", "tbhq", "anoxomer", "ethoxyquin", "ammonium lactate", "ammonium adipate", "ammonium fumarate",
         "soybean hemicellulose", "cassia gum", "peptones", "polyoxyethene 8 stearate", "polyoxyethene 40 stearate", "polyoxyethene 20 stearate",
         "brominated", "succistearin", "beta cyclodexterin", "crosslinked sodium", "zinc silicate", "potassium silicate", "vermiculite", "sepiolite"
    )

    // This function checks if a product is halal based on its ingredients
    fun isHalal(context: Context, ingredients: List<String>, barcode: String? = null, name: String = "") {
        // Store the product name
        val productName: String = name
        var localIcon: String = ""

        // Launch a coroutine in the IO context for network and database operations
        CoroutineScope(Dispatchers.IO).launch {
            // If a barcode is provided, fetch the product icon
            if (barcode != null) {
                localIcon = withContext(Dispatchers.IO) {
                    getProductIcon(barcode).toString()
                }
            }

            // Assume the product is halal until proven otherwise
            var isHalal = true
            // Keep track of whether any unknown ingredients are found
            var unknownFound = false

            // Iterate over the ingredients
            for (item in ingredients) {
                // Clean the ingredient name
                val cleanedItem = item.lowercase().replace("[()]".toRegex(), "")
                // If the ingredient is in the haram list
                if (cleanedItem in haramList) {
                    // Update the database with the product information
                    val historyData = HistoryData(image = localIcon, name = productName, status = "Haram")
                    updateDB(context, historyData)

                    // Launch the HaramActivity
                    val intent = Intent(context, HaramActivity::class.java)
                    context.startActivity(intent)

                    // Set isHalal to false and break from the loop
                    isHalal = false
                    break
                }
                // If the ingredient is in the maybe list
                else if (cleanedItem in maybeList) {
                    // Set unknownFound to true
                    unknownFound = true
                }
            }

            // If the product is still considered halal after checking all ingredients
            if (isHalal) {
                // If any unknown ingredients were found
                if (unknownFound) {
                    // Update the database with the product information
                    val historyData = HistoryData(image = localIcon, name = productName, status = "Unknown")
                    updateDB(context, historyData)

                    // Launch the UnknownActivity
                    val intent = Intent(context, UnknownActivity::class.java)
                    context.startActivity(intent)
                }
                // If no unknown or haram ingredients were found
                else {
                    // Update the database with the product information
                    val historyData = HistoryData(image = localIcon, name = productName, status = "Halal")
                    updateDB(context, historyData)

                    // Launch the HalalActivity
                    val intent = Intent(context, HalalActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
    }

    // This function updates the database with the provided history data
    private suspend fun updateDB(context: Context, historyData: HistoryData) {
        // Use withContext to switch to the IO dispatcher for database operations
        withContext(Dispatchers.IO) {
            try {
                // Get an instance of the database
                val db = DatabaseManager.getInstance(context).database

                // If the product name is empty, create a new name using the ID
                // Otherwise, use the provided history data
                val updatedHistoryData = if (historyData.name == "") {
                    HistoryData(image = historyData.image, name = "Scanned Product #${db.historyDao().getNewestItemId() + 1}", status = historyData.status)
                } else {
                    historyData
                }

                // Insert the updated history data into the database
                db.historyDao().insertAll(updatedHistoryData)

                // Get the count of items in the database
                val count = db.historyDao().getCount()

                // If the count exceeds 100, delete the oldest items to maintain a maximum size of 100
                if (count > 100) {
                    db.historyDao().deleteOldest(count - 100)
                }else{
                    // Do nothing here because thats how kotlin works
                }
            } catch (e: Exception) {
                // Log an error message if an exception occurs
                Log.i("ERROR", "Failed to update database: ${e.message}")
            }
        }
    }

}


