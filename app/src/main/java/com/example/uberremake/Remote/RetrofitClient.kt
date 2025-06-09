package com.example.uberremake.Remote

import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    val instance: Retrofit? = null
        get() = if (field == null)
            Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        else field
}