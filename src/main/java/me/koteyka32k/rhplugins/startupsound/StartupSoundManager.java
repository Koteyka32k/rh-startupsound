package me.koteyka32k.rhplugins.startupsound;

import me.koteyka32k.rhplugins.startupsound.module.StartupSoundModule;
import me.koteyka32k.rhplugins.startupsound.sound.Sound;
import net.minecraft.client.Minecraft;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.StringSetting;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Startup Sound Manager. This chooses the audio to play
 * and also makes the audio actually play via OpenAL and STB Vorbis.
 *
 * @author Koteyka32k
 * @since 1.0
 */
 public final class StartupSoundManager {
    /**
     * State of the sound manager.
     */
    private static boolean initialized;

    /**
     * A list of available startup sounds.
     */
    private static final List<Sound> sounds = new ArrayList<>();

    /**
     * A setting that decides whether to make the startup sound random or not.
     */
    private static final BooleanSetting useRandom = new BooleanSetting("UseRandom",
            "Whether to use a random audio file during startup.", false);

    /**
     * A setting that decides what audio file to play at startup.
     * Only visible if !useRandom.
     */
    private static final StringSetting startupSound = new StringSetting("Sound", "What sound to play during startup.", null)
            .setVisibility(() -> !useRandom.getValue());

    /**
     * A (hacky) button that opens the folder in case.
     */
    private static final BooleanSetting openFolder = createOpenFolderButton();

    /**
     * Initializes the startup sound manager instance.
     */
    public static void init() {
        // check if dir exists
        final File startupSoundsDir = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath() + "/rusherhack/startupsounds/");
        if (!startupSoundsDir.exists()) {
            if (!startupSoundsDir.mkdir()) {
                throw new RuntimeException("Failed to create \"rusherhack/startupsounds/\" folder!");
            }
        }

        // null is used as a flag here
        startupSound.setDefaultValue(null);

        for (File file : Objects.requireNonNull(startupSoundsDir.listFiles())) {
            if (!file.isFile() || !file.canRead() || !file.getName().endsWith(".ogg"))
                continue;

            Sound sound = new Sound(file);
            sounds.add(sound);
            startupSound.addOptions(sound.getDisplayName());

            if (startupSound.getDefaultValue() == null && startupSound.getValue() == null) {
                startupSound.setDefaultValue(sound.getDisplayName());
                startupSound.setValue(sound.getDisplayName());
            }
        }

        if (sounds.isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no startup sounds! " +
                            "Please put startup sound files (*.ogg) into the opened directory (it will appear after clicking OK). Then, please restart the game.");
            openFolder();
        }

        // openFolder & useRandom are fine without the songs
        // startupSound *needs* the songs so we don't register
        // it if we don't have songs

        StartupSoundModule.getInstance().registerSettings(openFolder);
        if (!sounds.isEmpty())  StartupSoundModule.getInstance().registerSettings(startupSound);
        StartupSoundModule.getInstance().registerSettings(useRandom);

        initialized = true;
    }

    public static void playStartupSound() {
        if (!initialized || sounds.isEmpty()) return;

        Sound sound = chooseSound();
        if (sound != null) {
            sound.play();
        } else {
            throw new RuntimeException("Failed to find a sound!");
        }
    }

    private static Sound chooseSound() {
        if (useRandom.getValue()) {
            return sounds.get(new SecureRandom().nextInt(0, sounds.size()));
        } else {
            for (Sound sound : sounds) {
                if (sound.getDisplayName().equals(startupSound.getValue()))
                    return sound;
            }
        }

        return null;
    }

    private static BooleanSetting createOpenFolderButton() {
        return new BooleanSetting("OpenFolder", "Opens the folder containing the startup sounds.", true) {
            @Override
            public void setValue(Boolean value) {
                if (initialized && value != getDefaultValue()) {
                    openFolder();
                } else {
                    super.setValue(value);
                }
            }
        };
    }

    private static void openFolder() {
        try {
            Desktop.getDesktop().open(new File(Minecraft.getInstance().gameDirectory.getAbsolutePath() + "/rusherhack/startupsounds/"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open folder!");
        }
    }
}
