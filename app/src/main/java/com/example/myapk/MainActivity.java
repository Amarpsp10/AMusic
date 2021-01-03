package com.example.myapk;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;


import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.database.Cursor;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.w3c.dom.Text;

import java.io.FileDescriptor;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import static com.example.myapk.R.id.text;
import static com.example.myapk.R.id.thumbnail;
import static com.example.myapk.R.id.visible;

public class MainActivity extends AppCompatActivity {

    //fragment part , it shows thumbnail and title of song in fragment
    ImageButton thumb;
    Fragment fragment;
    FragmentManager fm ;
    FragmentTransaction fragmentTransaction;

    //to run time schedule task to update current and total duration of song
    Handler handler;
    Runnable runnable;
    TextView currTime;
    TextView totTime;
    SeekBar seekBar;

    //recycler view
    public Context context;
    RecyclerView recyclerView;

    //media player and status of song = if 0 means player is pause else 1 means song in running
    MediaPlayer player;
    public int status = 0;
    public ListView audioView;

    Drawable draw;
    // pp = play pause button
    ImageView pp;
    // song position is public show that index of song position in listView
    public int songPosition=0;

    //Audio list save the title of songs
    ArrayList<String> audioList;

    //files save the data(storage location) of songs in a sequence same as audio list
    String[] files;
    // it save Uris of songs .. because we use Uri to play song and files[] use to get thumbnail by media retriever
    Uri[] uris;

    //saves all bitmaps that is retrieve by files[]
    Bitmap[] bitmaps ;

    ImageButton playPauseB;
    ImageButton nextSong;
    ImageButton preSong;
    ImageButton forSongButton;

    public int i = 0;


    @SuppressLint("CutPasteId")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioView = (ListView) findViewById(R.id.listView1);
        playPauseB = findViewById(R.id.playPause);
        nextSong = findViewById(R.id.forSong);
        forSongButton = findViewById(R.id.forSong);
        preSong = findViewById(R.id.backSong);

        audioList = new ArrayList<String>();
        pp = findViewById(R.id.playPause);
        seekBar = findViewById(R.id.seekBar);
        currTime = findViewById(R.id.currTime);
        totTime  = findViewById(R.id.totalTime);
        forSongButton = findViewById(R.id.forSong);
        thumb = findViewById(R.id.thumbButton);
        context = getApplicationContext();


        fm = getSupportFragmentManager() ;







           // to update the song current progress in every 300 milli seconds if song will complete then it will play next song


           handler = new Handler();
          runnable = new Runnable() {
            @Override
            public void run() {
                if(player!=null) {
                    currTime.setText(time(player.getCurrentPosition()));

                 if(player.getCurrentPosition()>player.getDuration()-400){
                     forSong(forSongButton);
                 }
                }
               handler.postDelayed(this,300);
            }
        };
        handler.post(runnable);

       // seek bar change listener

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // player.seekTo(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                     player.seekTo(seekBar.getProgress());
            }
        });


        // new timer schedule to update seek bar progress in every 100 milli seconds and it will run the seek bar with progress of song
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               try {
                   seekBar.setProgress(player.getCurrentPosition());

               }catch (NullPointerException ignored){
               }

            }
        },0,100);


        // recycler View used for lower navigation menu

         LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,true);
        linearLayoutManager.setReverseLayout(false);

         recyclerView = findViewById(R.id.recyclerView);
         recyclerView.setLayoutManager(linearLayoutManager);



         // Media cursor to get all details about Files

        String[] projection = { MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.DATA};// Can include more data for more details and check i;

        Cursor audioCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null , null, null);

         // here Cursor .get Count () - give the total number of desired files

        files = new String[audioCursor.getCount()];
        uris = new Uri[audioCursor.getCount()];
        bitmaps = new Bitmap[audioCursor.getCount()];


        if(audioCursor != null){
            if(audioCursor.moveToFirst()){
                do{


                    int audioIndex = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    int idColumn = audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    long id = audioCursor.getLong(idColumn);

                    audioList.add(audioCursor.getString(audioIndex).replace(".mp3",""));



                    files[i] = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));



                    uris[i] =ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                    i = i+1;

      }while(audioCursor.moveToNext());
            }
        }


        // to retrieve the thumbnail of all song files by cover picture function it will decode the bytes into bitmap and take the parameter location of file
        // if thumbnail is not found then it saves null on that poistion
        for (int i = 0; i<files.length; i++){
            bitmaps[i] = coverPicture(files[i]);
        }


        // our Song custom adapter show the the song files into listView-audioView
        SongsCustomAdapter songscustomAdapter = new SongsCustomAdapter();
        audioView.setAdapter(songscustomAdapter);

        // on click listener for list view

        audioView.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

               // view.setSelected(true);
                // it update the position of song in list view
                songPosition = position;
                // it play selected song
                mediaPlay(uris[position]);
                // fragment will show thumbnail and title of that song
                fragmentSetter();


            }
        });


        RecyclerViewAdapter recyclerViewAdapter = new RecyclerViewAdapter(MainActivity.this,slideList,audioView);
        recyclerView.setAdapter(recyclerViewAdapter);


    }

// it every time makes new fragement and delete old one
    public void fragmentSetter(){
         fm.popBackStack();
        fragment = new thumbnailFragment(files[songPosition],audioList.get(songPosition));
        fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.fragment,fragment);
        fragmentTransaction.setReorderingAllowed(true);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

    }




// play song with uri

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void mediaPlay(Uri uri){

       seekBar.setProgress(0);
        try {
            player.reset();
        }catch (NullPointerException ignored){

        }
        player = MediaPlayer.create(this,uri);


        thumb.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_down_float));

        draw = getResources().getDrawable(R.drawable.new_pause_button);
        playPauseB.setImageDrawable(draw);
        player.start();
        status = 1;
        //thumb.setImageBitmap(bitmaps[songPosition]);

        seekBar.setMax(player.getDuration());
        totTime.setText(time(player.getDuration()));

          fragmentSetter();
        //transaction.replace(R.id.fragment, albums  , null);
        //transaction.commit();


    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void playPause(View view){
        if(status==1){
            draw = getResources().getDrawable(R.drawable.new_play_button_error);
            playPauseB.setImageDrawable(draw);
            if(player!=null){
            player.pause();}
            status = 0;

        }

        else if(status == 0){
            draw = getResources().getDrawable(R.drawable.new_pause_button);
            status =1;

            playPauseB.setImageDrawable(draw);
           if(player!=null){ player.start();}
           else mediaPlay(uris[0]);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void forSong(View v){

        if(songPosition+1<uris.length){
            songPosition+=1;
        mediaPlay(uris[songPosition]);

        }
        else {
            songPosition =0;
            mediaPlay(uris[0]);

        }

    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void backSong(View v){
        if(songPosition>0){
            songPosition-=1;
        mediaPlay(uris[songPosition]);
        }
        else{
            songPosition=uris.length-1;
            mediaPlay(uris[uris.length-1]);

        }

    }

// it retrieve bitmap of song by path of that song in storage

    public  Bitmap coverPicture(String path) {

        MediaMetadataRetriever mr = new MediaMetadataRetriever();

        mr.setDataSource(path);

        byte[] byte1 = mr.getEmbeddedPicture();
        mr.release();
        if(byte1 != null)
            return BitmapFactory.decodeByteArray(byte1, 0, byte1.length);
        else
            return  null;

    }



// our custom adapter

    class SongsCustomAdapter extends BaseAdapter{

    @Override
    public int getCount() {
        return audioList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = getLayoutInflater().inflate(R.layout.item,null);
        ImageView imageView = view.findViewById(R.id.thumbnail);
        TextView textView = view.findViewById(R.id.text1);
        if(bitmaps[position]!=null){
        imageView.setImageBitmap(bitmaps[position]);}
        else  imageView.setImageDrawable(getResources().getDrawable(R.drawable.spotify));
        textView.setText(audioList.get(position));
        return view;
    }
}



// lower navigation bar element names

ArrayList<String> slideList = getSlideList();

    public ArrayList<String> getSlideList() {
        ArrayList<String> slideList = new ArrayList<>();

        slideList.add("SONGS");
        slideList.add("SEARCH");
        slideList.add("SETTINGS");
        slideList.add("PROFILE");

        return slideList;
    }

    public String time(int milli){
        int sec = (milli / 1000);
        int min = sec/60;
        int seconds = sec%60;
        if(seconds<10){
            return String.valueOf(min)+":0"+String.valueOf(seconds);
        }

        return String.valueOf(min) +":" + String.valueOf(seconds);
    }

// fragment show and hide by click on button
    public void fragmentView(View v){

        if(fragment!=null){
            if(fragment.isVisible()){
                fm.popBackStack();
                thumb.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_up_float));
            }
            else {fragmentSetter();

                thumb.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_down_float));
            }
        }

        else Toast.makeText(this,"No Song Choose",Toast.LENGTH_SHORT).show();
     }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.reset();
    }

    @Override
    public void onBackPressed(){
        thumb.setImageDrawable(getResources().getDrawable(android.R.drawable.arrow_up_float));
        super.onBackPressed();

    }


}









