package com.breadcrumbsapp.view.arcore

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.breadcrumbsapp.ARCoreActivity
import com.breadcrumbsapp.R
import com.breadcrumbsapp.databinding.ArImagePostLayoutBinding
import com.breadcrumbsapp.interfaces.APIService
import com.breadcrumbsapp.util.CommonData
import com.breadcrumbsapp.util.SessionHandlerClass
import com.breadcrumbsapp.view.DiscoverScreenActivity
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.ar_challenge_layout.*
import kotlinx.android.synthetic.main.ar_image_post_layout.*
import kotlinx.android.synthetic.main.image_activity.*
import kotlinx.android.synthetic.main.image_activity.imagePostButton
import kotlinx.android.synthetic.main.image_activity.imagePostLayout
import kotlinx.android.synthetic.main.quiz_challenge.*
import kotlinx.android.synthetic.main.selfie_challenge_level_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

class ARImagePostScreen : AppCompatActivity() {
    private lateinit var sharedPreference: SessionHandlerClass
    lateinit var binding: ArImagePostLayoutBinding
    private var selfiePostValue = 50
    private var scoredValue = 0
    private var overallValue = 12000
    private var discoverValue = 1000
    private var interceptor = intercept()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ArImagePostLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreference = SessionHandlerClass(applicationContext)
        val uri = sharedPreference.getSession("arURI")
        println("ImageUri: $uri")
        val imageUri: Uri = Uri.parse(uri)
        binding.capturedImage.setImageURI(imageUri)


        CropImage.activity(imageUri).setAspectRatio(1, 1).setFixAspectRatio(true).start(this)

        arChallengeLevelCloseBtn.setOnClickListener {
         /*   startActivity(
                Intent(
                    this@ARImagePostScreen,
                    DiscoverScreenActivity::class.java
                ).putExtra("isFromLogin", "no")
            )
            overridePendingTransition(
                R.anim.anim_slide_in_left,
                R.anim.anim_slide_out_left
            )
            finish()
*/
            discoverPOI()
        }
        binding.arImagePostBackButton.setOnClickListener(View.OnClickListener {
            //    CropImage.activity(imageUri).setAspectRatio(1, 1).setFixAspectRatio(true).start(this)

            startActivity(Intent(this@ARImagePostScreen, ARCoreActivity::class.java))
        })
        binding.imagePostButton.setOnClickListener(View.OnClickListener {


            if (imagePostButton.text.equals("CONTINUE")) {
                Glide.with(applicationContext).load(sharedPreference.getSession("poi_image"))
                    .into(arChallengeImageView)

                binding.imagePostLayout.visibility = View.GONE
                binding.arSelfieChallengeLevelLayout.visibility = View.VISIBLE
                imagePostLayout.visibility = View.GONE

                /*       selfieChallengeProgressBar.max = overallValue
                       scoredValue = discoverValue + selfiePostValue
                       arSelfiePostMark.text = "+$selfiePostValue XP"

                       ObjectAnimator.ofInt(selfieChallengeProgressBar, "progress", scoredValue)
                           .setDuration(1000)
                           .start()

                       val subtractValue = overallValue - scoredValue
                       arBalanceScoreValue.text = "$subtractValue XP to Level 2"*/

                calculateXPPoints()
            } else {
                binding.didUKnowLayout.visibility = View.VISIBLE
                binding.imagePostLayout.visibility = View.VISIBLE
                binding.arSelfieChallengeLevelLayout.visibility = View.GONE

                binding.arImagePostBackButton.visibility = View.INVISIBLE
                binding.titleText.text = "Photo posted successfully!"
                binding.didYouKnowTxt.visibility = View.VISIBLE
                binding.didYouKnowContent.visibility = View.VISIBLE
                imagePostButton.background = getDrawable(R.drawable.selfie_continue_btn)
                imagePostButton.text = "CONTINUE"
            }

        })

    }

    private fun calculateXPPoints() {
        try {
            arSelfieChallengeProgressBar.max = overallValue
            discoverValue = CommonData.getUserDetails!!.experience.toInt()
            println("discover_value $discoverValue")
            scoredValue = discoverValue + selfiePostValue
            arSelfiePostMark.text = "+$selfiePostValue XP"

            var totalScore =0
            for (i in 0 until CommonData.eventsModelMessage!!.count()) {
                if (CommonData.eventsModelMessage!![i].disc_id != null) {
                    totalScore += CommonData.eventsModelMessage!![i].experience.toInt()
                    println("totalScore :: $totalScore")
                }
            }
            println("totalScore :: $totalScore")
            ObjectAnimator.ofInt(arSelfieChallengeProgressBar, "progress", totalScore)
                .setDuration(1000)
                .start()

            totalScore+=scoredValue
            val subtractValue = overallValue - totalScore
            arBalanceScoreValue.text = "$subtractValue XP to Level 2"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun discoverPOI() {


        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build()
        // Create Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(resources.getString(R.string.staging_url))
            .client(okHttpClient)
            .build()

        // Create Service
        val service = retrofit.create(APIService::class.java)

        // Create JSON using JSONObject
        val jsonObject = JSONObject()
        jsonObject.put("user_id", sharedPreference.getSession("login_id"))
        jsonObject.put("poi_id", sharedPreference.getSession("selectedPOIID"))

        println("Discover_POI Input = $jsonObject")
        // Convert JSONObject to String
        val jsonObjectString = jsonObject.toString()

        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
        val requestBody = jsonObjectString.toRequestBody("application/json".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            val response = service.discoverPOI(requestBody)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {

                    // Convert raw JSON to  JSON using GSON library
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val registerJSON = gson.toJson(
                        JsonParser.parseString(
                            response.body()
                                ?.string()
                        )
                    )
                    val jsonElement: JsonElement? = JsonParser.parseString(registerJSON)
                    val jsonObject: JsonObject? = jsonElement?.asJsonObject

                    val status: Boolean = jsonObject?.get("status")!!.asBoolean
                    println("Discover_POI Status = $jsonElement")

                    if (status) {



                        startActivity(
                            Intent(
                                this@ARImagePostScreen,
                                DiscoverScreenActivity::class.java
                            ).putExtra("isFromLogin","no")
                        )
                        overridePendingTransition(
                            R.anim.anim_slide_in_left,
                            R.anim.anim_slide_out_left
                        )
                        finish()
                    }
                } else {

                    println("Printed JSON ELSE : ${response.code()}")

                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri = result.uri
                binding.capturedImage.setImageURI(resultUri)
                println("Cropped Image : $resultUri")
                imagePostLayout.visibility = View.VISIBLE

                /*   poiID=sharedPreference.getSession("selectedPOIID").toString()
                   dateFormat= SimpleDateFormat(selfieChallengeImagePostActivity.FILENAME, Locale.US).format(System.currentTimeMillis())
                   tempFile="uri:${result.uri}"+",type:'image/jpeg',name:'selfie_challenge_'$dateFormat+_$poiID.jpg"
                   println("tempFile $tempFile")*/
                //    constraintLayout.setBackgroundColor(Color.parseColor("#F8F0DD"))
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
    }
    private fun intercept(): HttpLoggingInterceptor {
        val interceptors = HttpLoggingInterceptor()
        interceptors.level = HttpLoggingInterceptor.Level.BODY
        interceptor = interceptors
        return interceptor
    }

}