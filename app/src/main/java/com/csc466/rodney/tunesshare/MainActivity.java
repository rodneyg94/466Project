package com.csc466.rodney.tunesshare;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import android.net.Uri;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import com.csc466.rodney.tunesshare.MusicService.MusicBinder;

public class MainActivity extends ActionBarActivity implements MediaPlayerControl{

    // Store songs in 'songList' and display them in the ListView 'songView'
    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;
    private boolean paused = false, playbackPaused = false;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<>();
        getSongList();
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        setController();

        initializeRegistrationListener();
        registerService(9000);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Retrieves song info and creates a list
    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME;
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Get columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            // Add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song (thisId, thisTitle, thisArtist));
            } while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                // Shuffle
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    // Controller stuff
    private void setController() {
        // Set the controller up
        controller = new MusicController(this);
        // Determine what will happen when the user presses the previous/next buttons
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    public void start() {
        musicSrv.go();
    }

    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    private void playNext(){
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    public boolean isPlaying() {
        return (musicSrv != null && musicBound && musicSrv.isPng());
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPng()) return musicSrv.getPosn();
        return 0;
    }

    public int getBufferPercentage() {
        return 0;
    }

    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPng()) return musicSrv.getDur();
        return 0;
    }

    // Register service on network
    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it.
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.setServiceName("TunesShare");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);

        mNsdManager = Context.getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                String mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

}
