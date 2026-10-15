package com.ryzer.ryzergen.client.handbook;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

/** The mod's links, opened in the browser after the game's usual "open this link?" check. */
public final class Links {
    /** The invite kept for in-game links, so the server's invite list counts joins from the game. */
    public static final String DISCORD = "https://discord.gg/cdbc8CjYgn";
    public static final String BUGS = "https://github.com/MultiRyzer/ryzer-gen/issues";

    private Links() {}

    public static void open(Screen from, String url) {
        ConfirmLinkScreen.confirmLinkNow(from, url, true);
    }
}
