package com.example.musicplayer1;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import static com.example.musicplayer1.PlayerActivity.listSongs;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    IBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    Uri uri;
    int position = -1;
    ActionPlaying actionPlaying;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Bind","Method");
        return mBinder;
    }

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       int myPosition = intent.getIntExtra("servicePosition", -1);
       if (myPosition != -1) {
           playMedia(myPosition);
       }
        return START_STICKY;
    }

    private void playMedia(int StartPosition) {
        musicFiles = listSongs;
        position = StartPosition;
        if (mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            if (musicFiles != null)
            {
                createMediaPlayer(position);
                mediaPlayer.start();
            }
        }
        else  {
            createMediaPlayer(position);
            mediaPlayer.start();
        }
    }

    void start() { //start the mediaPlayer
        mediaPlayer.start();
    }

    boolean isPlaying() {  // to check the media player is playing or not
        return mediaPlayer.isPlaying();
    }

    void stop() {  //to stop the mediaplayer
        mediaPlayer.stop();
    }

    void release() { //to release the mediaplayer instance
        mediaPlayer.release();
    }

    int getDuration() {
        return mediaPlayer.getDuration();
    }

    void seekTo(int position) {  //get the current position of the media player
        mediaPlayer.seekTo(position);
    }

    int getCurrentPosition() {
        return  mediaPlayer.getCurrentPosition();
    }

    void createMediaPlayer(int position) {
        uri = Uri.parse(musicFiles.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);
    }

    void pause() {
        mediaPlayer.pause();
    }

    void OnCompleted() {
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (actionPlaying != null) {
            actionPlaying.nextBtnClicked();
        }
        createMediaPlayer(position);
        start();
        OnCompleted();
    }


}
