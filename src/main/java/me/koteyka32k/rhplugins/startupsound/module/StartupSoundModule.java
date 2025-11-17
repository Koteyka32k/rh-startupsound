package me.koteyka32k.rhplugins.startupsound.module;

import me.koteyka32k.rhplugins.startupsound.StartupSoundManager;
import org.rusherhack.client.api.feature.module.Module;
import org.rusherhack.client.api.feature.module.ModuleCategory;

/**
 * Literally the whole purpose of this it to serve
 * as a container for the settings.
 *
 * @author Koteyka32k
 * @since 1.0
 */
public final class StartupSoundModule extends Module {
    private static StartupSoundModule instance;

    public static StartupSoundModule getInstance() {
        return instance;
    }

    public StartupSoundModule() {
        super("StartupSound", "Customize startup sound things.", ModuleCategory.CLIENT);
        instance = this;
    }
}
