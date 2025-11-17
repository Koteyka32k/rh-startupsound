package me.koteyka32k.rhplugins.startupsound;

import me.koteyka32k.rhplugins.startupsound.module.StartupSoundModule;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Plugin entry point.
 *
 * @author Koteyka32k
 * @since 1.0
 */
public final class StartupSoundPlugin extends Plugin {
    public static StartupSoundPlugin instance;

    @Override
    public void onLoad() {
        RusherHackAPI.getModuleManager().registerFeature(new StartupSoundModule());
        StartupSoundManager.init();
        instance = this;
    }

    @Override
    public void onUnload() {

    }
}
