package com.danila.gunsofwonders;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(GunsOfWonders.ID)
public final class GunsOfWonders {
    public static final String ID = "gunsofwonders";
    public static final String[] GUN_IDS = {"brees_glock", "brick_vintovka", "creeper", "dragon_vintovka", "dripstone_vintovka", "fantom_vintovka", "ghast_vintovka", "glock_ender", "glock_dirt", "glock_wither", "glock_spider", "glock_bedrock", "glock_iron", "glock_diamond", "glock_amethyst", "gold_vintovka", "herobrine_vintovka", "ming", "ming_lava", "obsidian_vintovka", "puli_pig", "puli_razorl", "skulk_vintovka", "skulkroket", "snow_vintovka", "vither_vintovka", "witch_glock"};
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final Map<String, RegistryObject<Item>> GUNS = new LinkedHashMap<>();
    static {
        for (String id : GUN_IDS) GUNS.put(id, ITEMS.register(id, () -> new GunItem(id, new Item.Properties().setId(ITEMS.key(id)).stacksTo(1))));
        TABS.register("guns", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.gunsofwonders"))
            .withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> GUNS.get("glock_amethyst").get().getDefaultInstance())
            .displayItems((parameters, output) -> GUNS.values().forEach(gun -> output.accept(gun.get()))).build());
    }
    public GunsOfWonders(FMLJavaModLoadingContext context) {
        ITEMS.register(context.getModBusGroup());
        TABS.register(context.getModBusGroup());
    }
    public static final class GunItem extends Item {
        public final String model;
        public GunItem(String model, Properties properties) { super(properties); this.model = model; }
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            consumer.accept(new com.danila.gunsofwonders.client.GunArmPose());
        }
    }
}
