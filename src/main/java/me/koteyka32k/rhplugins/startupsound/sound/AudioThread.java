package me.koteyka32k.rhplugins.startupsound.sound;


import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * The thread that plays the audio file. There is always one instance of this so
 * that we don't play multiple files at same time, as that causes more threads to spawn.
 * I'll write a proper solution later lol.
 *
 * @author Koteyka32k
 * @since 1.0
 */
public final class AudioThread extends Thread {
    private static AudioThread singleton = null;
    private final CountDownLatch prepLatch = new CountDownLatch(1);
    private final File audioFile;

    private AudioThread(final File audioFile) {
        this.audioFile = audioFile;
        this.setName("Audio - " + audioFile.getName());
        this.setDaemon(true);
    }

    static void play(Sound sound) {
        if (singleton != null) {
            singleton.interrupt();
            try {
                singleton.join();
            } catch (InterruptedException ignored) {}
        }

        singleton = new AudioThread(sound.file);
        singleton.start();
        try {
            // We need to wait for AudioTrack#play() to commence before letting
            // anything else happen in the current thread.
            singleton.prepLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("all")
    public void run() {
        try (AudioTrack audio = new AudioTrack(audioFile)) {
            audio.play();
            prepLatch.countDown();
            while (audio.update()) {
                if (interrupted())
                    break;
            }
        }
    }
}