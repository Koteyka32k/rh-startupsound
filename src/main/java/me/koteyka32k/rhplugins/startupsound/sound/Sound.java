package me.koteyka32k.rhplugins.startupsound.sound;

import java.io.File;

/**
 * Sound class, this is an abstraction object describing
 * a playable media of audio.
 *
 * @author Koteyka32k
 * @since 1.0
 */
public final class Sound {
    final File file;

    public Sound(File file) {
        this.file = file;
    }

    public String getDisplayName() {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }

    public void play() {
        AudioThread.play(this);
    }
}
