package com.example.musicplayer1;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

//to have a single instance of retrofit all over the app
public class NetworkClient {
    private static Retrofit retrofit;
    private static String Base_URL = "http://52.15.85.163:8080";

    public static Retrofit getRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        if (retrofit == null) {
            //if null initialize
            retrofit = new Retrofit.Builder().baseUrl(Base_URL).
                    addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build();
        }
        return retrofit;
    }
}
