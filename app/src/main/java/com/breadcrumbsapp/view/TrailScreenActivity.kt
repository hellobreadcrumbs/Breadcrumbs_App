package com.breadcrumbsapp.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.breadcrumbsapp.R
import com.breadcrumbsapp.adapter.FeedPostAdapter
import com.breadcrumbsapp.adapter.TrailsListScreenAdapter
import com.breadcrumbsapp.databinding.TrailsScreenLayoutBinding
import com.breadcrumbsapp.interfaces.APIService
import com.breadcrumbsapp.util.CommonData
import com.breadcrumbsapp.util.SessionHandlerClass
import kotlinx.android.synthetic.main.feed_layout.*
import kotlinx.android.synthetic.main.trails_screen_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class TrailScreenActivity:AppCompatActivity()
{
    private lateinit var sessionHandlerClass: SessionHandlerClass
    private var interceptor = intercept()
    private lateinit var binding:TrailsScreenLayoutBinding
    private lateinit var trailsListScreenAdapter:TrailsListScreenAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= TrailsScreenLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionHandlerClass= SessionHandlerClass(applicationContext)
        trailsScreenRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        getFeedPostData()
       // trailsListScreenAdapter = TrailsListScreenAdapter()
        //trailsScreenRecyclerView.adapter = trailsListScreenAdapter

        trails_screen_back_button.setOnClickListener {
            finish()
        }
    }



    private fun getFeedPostData() {
        try {

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
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            // Create JSON using JSONObject

            val jsonObject = JSONObject()
            jsonObject.put("user_id", sessionHandlerClass.getSession("login_id"))




            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)



            CoroutineScope(Dispatchers.IO).launch {

                // Create Service
                val service = retrofit.create(APIService::class.java)

                val response = service.getTrailsList(
                    resources.getString(R.string.api_access_token),
                    requestBody
                )

                if (response.isSuccessful) {
                    if (response.body()!!.status) {

                        CommonData.getTrailsData = response.body()?.message

                        runOnUiThread {

                            if (CommonData.getTrailsData != null) {
                               // feedListAdapter = FeedPostAdapter(CommonData.getFeedData!!)
                                //feedList.adapter = feedListAdapter

                                trailsListScreenAdapter = TrailsListScreenAdapter(CommonData.getTrailsData!!)
                                trailsScreenRecyclerView.adapter = trailsListScreenAdapter

                            }

                        }


                    }

                }


            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun intercept(): HttpLoggingInterceptor {
        val interceptors = HttpLoggingInterceptor()
        interceptors.level = HttpLoggingInterceptor.Level.BODY
        interceptor = interceptors
        return interceptor
    }
}