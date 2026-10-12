package com.example.utilmod;

import com.example.utilmod.config.Config;
import com.example.utilmod.feature.BaseFinder;
import com.example.utilmod.feature.Xray;
import com.example.utilmod.gui.ClickGuiScreen;
import com.example.utilmod.render.EspRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint. Nothing here touches the network layer: everything is local rendering + local world reads. */
public class UtilModClient implements ClientModInitializer {
    public static final String MOD_ID = "utilmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping openGui;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(id("main"));
        openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.utilmod.open_gui",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT, // rebindable in Options > Controls
                category));

        EspRenderer.init();

        // Resolve block-id filters once the registries/world exist, then (re)apply xray.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            BaseFinder.refreshFilters();
            Xray.apply(false);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                if (client.level != null && client.gui.screen() == null) {
                    client.gui.setScreen(new ClickGuiScreen());
                }
            }
            BaseFinder.tick(client);
        });

        // Pressing the same key again closes the GUI (unless you're typing in a text box).
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ClickGuiScreen)) return;
            ScreenKeyboardEvents.beforeKeyPress(screen).register((s, keyEvent) -> {
                if (s.getFocused() instanceof EditBox) return;
                if (openGui.matches(keyEvent)) client.gui.setScreen(null);
            });
        });
    }
}
