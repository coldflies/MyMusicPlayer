package com.example.mymusicplayer;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    ImageView nextIv,playIv,lastIv,orderIv;
    TextView singerTv,songTv;
    RecyclerView musicRv;
    SeekBar audio_seekBar;
    private Thread thread;
    //判断当前是否为顺序播放：
    boolean isInOrder = true;
    //判断当前是否为暂停状态
    boolean isStop ;
    List<LocalMusicBean> mDatas;
    private LocalMusicAdapter adapter;
//记录当前正在播放的音乐的序号
    int currnetPosition = -1;
    //记录暂停音乐时进度条的位置
    int currentPausePosition =0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            audio_seekBar.setProgress(msg.what);


        }
    };


    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //隐藏标题栏
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }
        initView();
        mediaPlayer = new MediaPlayer();
        mDatas = new ArrayList<>();
        //创建适配器对象
        adapter = new LocalMusicAdapter(this,mDatas);
       //设置适配器
        musicRv.setAdapter(adapter);
        //为recycleview设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        musicRv.setLayoutManager(layoutManager);

        //加载本地数据源
       loadLocalMusicData();
       setEventListener();


        audio_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    mediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    private void setEventListener() {
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                currnetPosition = position;
                LocalMusicBean musicBean = mDatas.get(position);
                playMusicInPosition(musicBean);


            }

        });
    }

    //根据传入musicBean，播放音乐
    public void playMusicInPosition(LocalMusicBean musicBean) {

        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();
        mediaPlayer.reset();
        try{
            mediaPlayer.setDataSource(musicBean.getPath());
            //audio_seekBar.setMax(mediaPlayer.getDuration());
            //audio_seekBar.setProgress(0);
            playMusic();
        }catch(IOException e){
            e.printStackTrace();
        }
        new Thread(new SeekBarThread()).start();
        audio_seekBar.setMax(mediaPlayer.getDuration());
    }
    private void playMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            if(currentPausePosition == 0){
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    //audio_seekBar.setMax(mediaPlayer.getDuration());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                playIv.setImageResource(R.mipmap.icon_pause);

            }else {
                //从暂停位置开始播放
                mediaPlayer.seekTo(currentPausePosition);
                mediaPlayer.start();
                playIv.setImageResource(R.mipmap.icon_pause);

            }

        }
    }
    private void pauseMusic() {
        if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
            currentPausePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }
    private void stopMusic() {
        if(mediaPlayer!= null ){
            currentPausePosition = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopMusic();
    }

    //加载本地数据源音乐文件函数到集合当中（获取本地文件）
    private void loadLocalMusicData() {
        //获取contentResover
        ContentResolver contentResolver = getContentResolver();
        //获取本地音乐的URI地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //查询地址
        Cursor cursor = contentResolver.query(uri,null,null,null,null);
        int id = 0;
        while (cursor.moveToNext()){
            String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));//歌曲名称
            String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            id++;
            String sid = String.valueOf(id);
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            //将时长转换为人类能识别的格式：
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time = sdf.format(new Date(duration));
            LocalMusicBean localMusicBean = new LocalMusicBean(sid, song, singer, album, time, path);
            mDatas.add(localMusicBean);

        }
        adapter.notifyDataSetChanged();


    }

    //  初始化控件的函数
    private void initView() {
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        lastIv = findViewById(R.id.local_music_bottom_iv_last);
        singerTv = findViewById(R.id.local_music_bottom_tv_singer);
        songTv = findViewById(R.id.local_music_bottom_tv_song);
        musicRv = findViewById(R.id.local_music_rv);
        orderIv = findViewById(R.id.local_music_bottom_iv_order);
        audio_seekBar = findViewById(R.id.seekBar);
        nextIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        orderIv.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.local_music_bottom_iv_last:
                if(isInOrder){//如果现在是顺序播放
                    if (currnetPosition == 0) {
                        Toast.makeText(this,"当前歌曲已经为第一首",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currnetPosition = currnetPosition-1;
                    LocalMusicBean lastbean = mDatas.get(currnetPosition);

                    playMusicInPosition(lastbean);
                    break;

                }else{
                    double b = Math.random() * (mDatas.size()-1);
                    int i =(new Double(b)).intValue();
                    LocalMusicBean musicBean = mDatas.get(i);
                    currnetPosition = i;
                    playMusicInPosition(musicBean);

                }
                break;
            case R.id.local_music_bottom_iv_next:
                if(isInOrder){
                    if (currnetPosition == mDatas.size()-1) {
                        Toast.makeText(this,"当前歌曲已经为最后一区",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currnetPosition = currnetPosition+1;
                    LocalMusicBean nextbean = mDatas.get(currnetPosition);

                    playMusicInPosition(nextbean);
                    break;

                }else{
                    double b = Math.random() * (mDatas.size()-1);
                    int i =(new Double(b)).intValue();
                    LocalMusicBean musicBean = mDatas.get(i);
                    currnetPosition = i;
                    playMusicInPosition(musicBean);

                }

                break;
            case R.id.local_music_bottom_iv_play:
                if (currnetPosition ==-1) {
                    //并没有音乐可供播放
                    return;
                }
                if (mediaPlayer.isPlaying()){
                    pauseMusic();
                }else{
                    //如果现在未播放音乐
                    playMusic();
                }
                break;
            case R.id.local_music_bottom_iv_order:
                if(isInOrder){
                    orderIv.setImageResource(R.mipmap.icon_radom);
                    Toast.makeText(this,"随机播放",Toast.LENGTH_SHORT).show();
                    isInOrder=false;
                }else{
                    orderIv.setImageResource(R.mipmap.icon_inorder);
                    Toast.makeText(this,"顺序播放",Toast.LENGTH_SHORT).show();
                    isInOrder=true;
                }
                break;
        }
    }


    class SeekBarThread implements Runnable {


        @Override
        public void run() {
            while (mediaPlayer != null && isStop == false) {
                // 将SeekBar位置设置到当前播放位置
                handler.sendEmptyMessage(mediaPlayer.getCurrentPosition());
                try {
                    // 每100毫秒更新一次位置
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }







}
