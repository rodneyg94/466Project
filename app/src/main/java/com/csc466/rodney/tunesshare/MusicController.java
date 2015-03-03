package com.csc466.rodney.tunesshare;

/**
 * This class presents a standard widget with play/pause, rewind, fast-forward, and skip buttons in
 * it. It also contains a seek bar, which updates as the song plays and contains text indicating the
 * duration of the song and the player's current position
 */

import android.content.Context;
import android.widget.MediaController;

public class MusicController extends MediaController {

    // Constructor
    public MusicController(Context c){
        super(c);
    }

    // Overrides the hide method from MediaController
    public void hide(){}

}
