package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * What a cable carries, as far as its panel cares: the menu type, the lang keys and how a rate is
 * shown. Energy is FE per tick averaged over a second; items move in bursts, so they are counted
 * per second.
 */
public enum CableKind {
    ENERGY("cable", false, 0xFFFF4D3D, () -> ModMenus.ENERGY_CABLE.get()),
    ITEMS("item_pipe", true, 0xFFFF8A1E, () -> ModMenus.ITEM_PIPE.get()),
    FLUID("fluid_pipe", false, 0xFF35C8F5, () -> ModMenus.FLUID_PIPE.get()),
    GAS("gas_pipe", false, 0xFFE1E6EB, () -> ModMenus.GAS_PIPE.get());

    private final String langKey;
    private final boolean perSecond;
    private final int colour;
    private final Supplier<MenuType<CableMenu>> menuType;

    CableKind(String langKey, boolean perSecond, int colour, Supplier<MenuType<CableMenu>> menuType) {
        this.langKey = langKey;
        this.perSecond = perSecond;
        this.colour = colour;
        this.menuType = menuType;
    }

    /** The panel's lang keys start with {@code gui.ryzergen.<this>.}. */
    public String langKey() {
        return langKey;
    }

    /** True: the panel sums a second of samples. False: it averages them per tick. */
    public boolean perSecond() {
        return perSecond;
    }

    /** The rate bar's colour, as on the ports: red energy, orange items, blue liquids, white gases. */
    public int colour() {
        return colour;
    }

    public MenuType<CableMenu> menuType() {
        return menuType.get();
    }
}
