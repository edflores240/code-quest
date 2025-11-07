package io.github.code_quest.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * Manages a single shared looping background music instance so that screens can
 * hand off playback without audible gaps.
 */
public final class BackgroundMusicManager {
    private static Music sharedMusic;
    private static String currentPath;
    private static int usageCount;
    private static float lastVolume = 1f;

    private BackgroundMusicManager() {}

    public static synchronized void playLoop(String path, float volume) {
        if (sharedMusic == null) {
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.error("BackgroundMusicManager", "Missing music: " + path);
                return;
            }
            sharedMusic = Gdx.audio.newMusic(Gdx.files.internal(path));
            sharedMusic.setLooping(true);
            currentPath = path;
        } else if (!path.equals(currentPath)) {
            swapTrack(path);
        }

        usageCount++;
        setVolume(volume);
        if (!sharedMusic.isPlaying()) {
            sharedMusic.play();
        }
    }

    public static synchronized void ensurePlaying(String path, float volume) {
        if (sharedMusic == null) {
            playLoop(path, volume);
            return;
        }
        if (!path.equals(currentPath)) {
            swapTrack(path);
        }
        setVolume(volume);
        if (!sharedMusic.isPlaying()) {
            sharedMusic.play();
        }
    }

    public static synchronized void release() {
        if (usageCount > 0) {
            usageCount--;
            if (usageCount == 0) {
                disposeMusic();
            }
        }
    }

    private static void swapTrack(String path) {
        disposeMusic();
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("BackgroundMusicManager", "Missing music: " + path);
            return;
        }
        sharedMusic = Gdx.audio.newMusic(Gdx.files.internal(path));
        sharedMusic.setLooping(true);
        currentPath = path;
        usageCount = 0; // reset count; caller will increment afterwards
    }

    private static void setVolume(float volume) {
        lastVolume = volume;
        if (sharedMusic != null) {
            sharedMusic.setVolume(volume);
        }
    }

    private static void disposeMusic() {
        if (sharedMusic != null) {
            if (sharedMusic.isPlaying()) {
                sharedMusic.stop();
            }
            sharedMusic.dispose();
            sharedMusic = null;
            currentPath = null;
            usageCount = 0;
            lastVolume = 1f;
        }
    }
}
