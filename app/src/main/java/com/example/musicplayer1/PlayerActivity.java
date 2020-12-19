package com.example.musicplayer1;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static com.example.musicplayer1.AlbumDetailsAdapter.albumFiles;
import static com.example.musicplayer1.ApplicationClass.ACTION_NEXT;
import static com.example.musicplayer1.ApplicationClass.ACTION_PLAY;
import static com.example.musicplayer1.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicplayer1.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicplayer1.MainActivity.musicFiles;
import static com.example.musicplayer1.MainActivity.repeatBoolean;
import static com.example.musicplayer1.MainActivity.shuffleBoolean;
import static com.example.musicplayer1.MusicAdapter.mFiles;

///////////////////////////////////////////
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
//////////////////////////////////////////
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection, PictureCapturingListener, ActivityCompat.OnRequestPermissionsResultCallback {

    //initialize all the views used in our activity_player
    TextView user_mood, song_name, artist_name, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    int position = -1;
    public static ArrayList<MusicFiles> listSongs = new ArrayList<>();
    static Uri uri;
    private Handler handler = new Handler();
    private Thread playThread, prevThread, nextThread;
    MusicService musicService;
    //private static Pojo pojo = new Pojo();

    // The image selected to detect.
    private Bitmap mBitmap;
    private String finalMood = "";
    private ProgressDialog detectionProgressDialog;
    Face[] facesDetected;

    private FaceServiceClient faceServiceClient = new FaceServiceRestClient("https://mooddetector.cognitiveservices.azure.com/face/v1.0/", "0d71073c12d748df8ae9d4257e1bd7ac");

    /////////////////////

    //cam
    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;

    //The capture service
    private APictureCapturingService pictureService;

    //The mood recognition service
    //private MoodRecognitionService moodRecognitionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();
        initViews();
        getIntenMethod();
        checkPermissions();
        pictureService = PictureCapturingServiceImpl.getInstance(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser)
                {
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });  //seekBar acts according to the song
        PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
            @Override
            public void run() {
                if (musicService != null)
                {
                    int mCurrentPosition = musicService.getCurrentPosition() /1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000); //1 sec
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shuffleBoolean)
                {
                    shuffleBoolean = false;
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_off);
                }
                else
                {
                    shuffleBoolean = true;
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_on);
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatBoolean)
                {
                    repeatBoolean = false;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_off);
                }
                else
                {
                    repeatBoolean = true;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_on);
                }
            }
        });
    }

    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    private void uploadImage() {
        File file = new File(Environment.getExternalStorageDirectory() + "/1_pic.jpg"); //the file we want to upload
//        mBitmap = BitmapFactory.decodeFile(String.valueOf(file));
        mBitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/1_pic.jpg");
        mBitmap = RotateBitmap(mBitmap, 180);
        getMood(mBitmap);
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private  void getMood(final Bitmap imageBitmap) {
        Log.e("myTag ", "inside getMood");
//        File file = new File(Environment.getExternalStorageDirectory() + "/1_pic.jpg"); //the file we want to upload
//        Log.e("myTag ", "File : "+file);
//        // mImageUri = Uri.fromFile(file);
//
//        mBitmap = BitmapFactory.decodeFile(String.valueOf(file));
//        Log.e("myTag ", "bitmap : "+mBitmap);
//        //Log.e(TAG, "detectAndFrame"  );
        Log.e("myTag", ""+imageBitmap);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        @SuppressLint("StaticFieldLeak") AsyncTask<InputStream, String, Face[]> detectTask =

                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            Log.e("myTag", "doInBackground try: " );
                            publishProgress("Detecting...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    // returnFaceAttributes:
                                    new FaceServiceClient.FaceAttributeType[] {
                                            FaceServiceClient.FaceAttributeType.Emotion,
                                            FaceServiceClient.FaceAttributeType.Gender }

                            );
                            Log.e("myTag", "after result");
                            Log.e("myTag", ""+result.length);


                            for (int i=0;i<result.length;i++) {
                                Log.e("myTag", "inside for loop " );
//                                jsonObject.put("happiness" , result[i].faceAttributes.emotion.happiness);
//                                jsonObject.put("sadness" , result[i].faceAttributes.emotion.sadness);
//                                jsonObject.put("surprise" , result[i].faceAttributes.emotion.surprise);
//                                jsonObject.put("neutral"  , result[i].faceAttributes.emotion.neutral);
//                                jsonObject.put("anger" , result[i].faceAttributes.emotion.anger);
//                                jsonObject.put("contempt" , result[i].faceAttributes.emotion.contempt);
//                                jsonObject.put("disgust" , result[i].faceAttributes.emotion.disgust);
//                                jsonObject.put("fear" , result[i].faceAttributes.emotion.fear);
//                                Log.e(TAG, "doInBackground: "+jsonObject.toString()  );
//                                Log.e("myTag", "doInBackground: "+jsonObject.toString()  );
//                                Log.e("myTag", "doInBackground-ob1-1: "+jsonObject1.toString()  );
//                                jsonObject1.put(  (String.valueOf(i)),jsonObject);
//                                Log.e("myTag", "doInBackground-ob1-2: "+jsonObject1.toString()  );

                                List<Double> list = new ArrayList<>();

                                list.add(result[i].faceAttributes.emotion.happiness);
                                list.add(result[i].faceAttributes.emotion.sadness);
                                list.add(result[i].faceAttributes.emotion.surprise);
                                list.add(result[i].faceAttributes.emotion.neutral);
                                list.add(result[i].faceAttributes.emotion.anger);
                                list.add(result[i].faceAttributes.emotion.contempt);
                                list.add(result[i].faceAttributes.emotion.disgust);
                                list.add(result[i].faceAttributes.emotion.fear);

                                Collections.sort(list);
                                double max = list.get(list.size() - 1);
                                if(max == result[i].faceAttributes.emotion.happiness) {
                                    finalMood = "Happy";
                                } else if(max == result[i].faceAttributes.emotion.sadness) {
                                    finalMood = "Sad";
                                } else if(max == result[i].faceAttributes.emotion.surprise) {
                                    finalMood = "Surprise";
                                } else if(max == result[i].faceAttributes.emotion.neutral) {
                                    finalMood = "Neutral";
                                } else if(max == result[i].faceAttributes.emotion.anger) {
                                    finalMood = "Angry";
                                } else if(max == result[i].faceAttributes.emotion.contempt) {
                                    finalMood = "Contempt";
                                } else if(max == result[i].faceAttributes.emotion.disgust) {
                                    finalMood = "Disgust";
                                } else if(max == result[i].faceAttributes.emotion.fear) {
                                    finalMood = "Fear";
                                }

                                Log.e("myTag", "Mood : "+finalMood  );

                            }
//
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(PlayerActivity.this,"DATA"+finalMood,Toast.LENGTH_LONG).show();
                                }});

                            if (result == null) {
                                publishProgress(
                                        "Detection Finished. Nothing detected");
                                return null;
                            }
                            Log.e("TAG", "doInBackground: "+"   "+result.length );
                            Log.e("myTag", "doInBackground-resultLength: "+"   "+result.length );
                            publishProgress(String.format(
                                    "Detection Finished. %d face(s) detected",
                                    result.length));
                            Log.e("myTag", "doInBackground-before return result: ");
                            Log.e("myTag", "doInBackground-before return-the-result: "+" "+result);
                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        //TODO: show progress dialog
                       // detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(String... progress) {
                        //TODO: update progress
                       // detectionProgressDialog.setMessage(progress[0]);
                    }

                    @Override
                    protected void onPostExecute(Face[] result) {
                        //TODO: update face frames
                       // detectionProgressDialog.dismiss();

                        facesDetected = result;

                        if (!exceptionMessage.equals("")) {
                            if (facesDetected == null) {
//                                showError(exceptionMessage + "\nNo faces detected.");
                            } else {
//                                showError(exceptionMessage);
                            }
                        }
                        if (result == null) {
                            if (facesDetected == null) {
//                                showError("No faces detected");
                            }
                        }
                        Log.e("TAG", "onPostExecute: "+facesDetected );
                        Log.e("myTag", "onPostExecute: "+facesDetected );
                        Log.e("myTag", "doInBackground: "+"   "+result.length );

//                        ImageView imageView = findViewById(R.id.imageView);
//                        imageView.setImageBitmap(
//                                drawFaceRectanglesOnBitmap(imageBitmap, result));
                        mBitmap.recycle();
//                        Toast.makeText(getApplicationContext(), "Now you can identify the person by pressing the \"Identify\" Button", Toast.LENGTH_LONG).show();
//                        takePicture = true;
                    }
                };

        detectTask.execute(inputStream);
    }
//    private void uploadImage() {
//        Log.e("myTag" , "Inside upload image");
//        File file = new File(Environment.getExternalStorageDirectory() + "/1_pic.jpg"); //the file we want to upload
//        Retrofit retrofit = NetworkClient.getRetrofit();
//        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
//        MultipartBody.Part part = MultipartBody.Part.createFormData("newimage", file.getName(), requestBody);
//        UploadApis uploadApis = retrofit.create(UploadApis.class);
//        Call call = uploadApis.uploadImage(part);
//        call.enqueue(new Callback() {
//            @Override
//            public void onResponse(Call call, Response response) {
//                Log.e("myTag" , "response received : " + response);
//                pojo = (Pojo) response.body();
//                Log.e("myTag", pojo.getEmotion());
//            }
//
//            @Override
//            public void onFailure(Call call, Throwable t) {
//                Log.e("myTag" , "" + t);
//
//            }
//        });
//    }


    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        prevThreadBtn();
        nextThreadBtn();
        playThreadBtn();
        
        super.onResume();
    }

    @Override
    protected void onPause() {  //this method unbounds the service while the onresume
        super.onPause();        //method binds the service
        unbindService(this);
    }

    private void playThreadBtn() {
        playThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if (musicService.isPlaying())
        {
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow);
            musicService.showNotification(R.drawable.ic_baseline_play_arrow);
            musicService.pause();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
        }
        else
        {
            musicService.showNotification(R.drawable.ic_baseline_pause);
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause);
            musicService.start();
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        takePicture();
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if (musicService.isPlaying())
        {
            musicService.stop();
            musicService.release();
            pictureService.startCapturing(this);
//            final Handler handler = new Handler(Looper.getMainLooper());
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//           // moodRecognitionService.detectAndFrame();
//            uploadImage();
//                }
//            }, 2000);

            uploadImage(); ///////////////////////////////////

            //original code
            if (shuffleBoolean && !repeatBoolean)
            {
                position = getRandom(listSongs.size() - 1); //position assigne randomly
            }
            else if (!shuffleBoolean && !repeatBoolean)
            {
                position = ((position + 1) % listSongs.size()); // increment position by 1
            }

            //else position will be position
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            //user_mood.setText(pojo.getEmotion()); //****************************UPDATE*************
            user_mood.setText(finalMood); //****************************UPDATE*************
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_baseline_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_pause);
            musicService.start();
        }
        else
        {
            musicService.stop();
            musicService.release();
            pictureService.startCapturing(this);
///         final Handler handler = new Handler(Looper.getMainLooper());
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//           // moodRecognitionService.detectAndFrame();
//            //uploadImage();
//                }
//            }, 2000);
           uploadImage(); ////////////////////////////////////////////////////////////////

            if (shuffleBoolean && !repeatBoolean)
            {
                position = getRandom(listSongs.size() - 1); //position assigne randomly
            }
            else if (!shuffleBoolean && !repeatBoolean)
            {
                position = ((position + 1) % listSongs.size()); // increment position by 1
            }
//            if (shuffleBoolean && !repeatBoolean)
//            {
//                position = getRandom(positions.size() - 1); //position assigne randomly
//            }
//            else if (!shuffleBoolean && !repeatBoolean)
//            {
//                position = ((position + 1) % positions.size()); // increment position by 1
//            }

            //else position will be position
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            //user_mood.setText(pojo.getEmotion());
            user_mood.setText(finalMood); //****************************UPDATE*************
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_baseline_play_arrow);
            //stopBackgroundThread();
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1); //generates a random between 0 and listSond.size() - 1
    }

    private void prevThreadBtn() {
        prevThread = new Thread()
        {
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if (musicService.isPlaying())
        {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean)
            {
                position = getRandom(listSongs.size() - 1); //position assigne randomly
            }
            else if (!shuffleBoolean && !repeatBoolean)
            {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1)); // decrement position by 1
            }

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            //user_mood.setText(pojo.getEmotion());
            user_mood.setText(finalMood); //****************************UPDATE*************
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_baseline_pause);
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_pause);
            musicService.start();
        }
        else
        {
            musicService.stop();
            musicService.release();
            if (shuffleBoolean && !repeatBoolean)
            {
                position = getRandom(listSongs.size() - 1); //position assigne randomly
            }
            else if (!shuffleBoolean && !repeatBoolean)
            {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1)); // decrement position by 1
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            //user_mood.setText(pojo.getEmotion());
            user_mood.setText(finalMood); //****************************UPDATE*************
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {    //time moves accordingly
                @Override
                public void run() {
                    if (musicService != null)
                    {
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000); //1 sec
                }
            });
            musicService.OnCompleted();
            musicService.showNotification(R.drawable.ic_baseline_play_arrow);
            playPauseBtn.setBackgroundResource(R.drawable.ic_baseline_play_arrow);
        }
    }

    private String formattedTime(int mCurrentPosition) {

        String totalout = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalout = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;
        if (seconds.length() == 1)
        {
            return totalNew;
        }
        else
        {
            return totalout;
        }
    }

    private void getIntenMethod() {
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");
        if (sender != null && sender.equals("albumDetails"))
        {
            listSongs = albumFiles;
        }
        else
        {
            listSongs = mFiles;
        }

        if (listSongs != null)
        {
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause);
            uri = Uri.parse(listSongs.get(position).getPath());
        }
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);
    }

    private void initViews() {
        user_mood = findViewById(R.id.user_mood);
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationPlayed);
        cover_art = findViewById(R.id.cover_art);
        duration_total = findViewById(R.id.durationTotal);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void  metaData(Uri uri)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));
        byte[] art = retriever.getEmbeddedPicture();
        Bitmap bitmap = null;
        if (art != null)
        {

            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
            ImageAnimation(this, cover_art, bitmap);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null)
                    {
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);
                        gradient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), 0x00000000});
                        gradient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), swatch.getRgb()});
                        mContainer.setBackground(gradientDrawableBg);
                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                    }
                    else
                    {
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);
                        gradient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0x00000000});
                        gradient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0xff000000});
                        mContainer.setBackground(gradientDrawableBg);
                        song_name.setTextColor(Color.WHITE);
                        artist_name.setTextColor(Color.DKGRAY);
                    }
                }
            });
        }
        else
        {
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.image2) /////////////////////////////
                    .into(cover_art);
            ImageView gradient = findViewById(R.id.imageViewGradient);
            RelativeLayout mContainer = findViewById(R.id.mContainer);
            gradient.setBackgroundResource(R.drawable.gradient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);
            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
        }
    }

    public void ImageAnimation(final Context context, final ImageView imageView, final Bitmap bitmap)
    {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        final Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder)service;
        musicService = myBinder.getService();
        musicService.setCallBack(this);
        Toast.makeText(this, "Connected" + musicService,
                Toast.LENGTH_SHORT).show();
        seekBar.setMax(musicService.getDuration() / 1000); //divide by 1000 to get as seconds
        //since duration gives time in milliseconds
        metaData(uri);
        //user_mood.setText(pojo.getEmotion());
        user_mood.setText(finalMood); //****************************UPDATE*************
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.OnCompleted();
        musicService.showNotification(R.drawable.ic_baseline_pause);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                if (pictureUrl.contains("0_pic.jpg")) {
                    //uploadBackPhoto.setImageBitmap(scaled);
                    Log.e("myTag", "Backphoto" );
                } else if (pictureUrl.contains("1_pic.jpg")) {
                    //uploadFrontPhoto.setImageBitmap(scaled);
                    Log.e("myTag", "Frontphoto ");
                }
            });
            Log.e("myTag", "Picture saved to " + pictureUrl);
        }

    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            //showToast("Done capturing all photos!");
            Log.e("myTag", "Done capturing all photos!");
            return;
        }
        // showToast("No camera detected!");
        Log.e("myTag", "No camera detected");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }
}