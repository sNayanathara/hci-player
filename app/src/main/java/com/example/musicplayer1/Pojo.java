package com.example.musicplayer1;

import com.google.gson.annotations.SerializedName;

public class Pojo {

    @SerializedName("emotion")
    private String emotion;

    public String getEmotion()
    {
        return emotion;
    }

}