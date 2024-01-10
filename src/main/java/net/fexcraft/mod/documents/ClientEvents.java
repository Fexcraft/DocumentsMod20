package net.fexcraft.mod.documents;

import com.mojang.blaze3d.platform.ScreenManager;
import net.fexcraft.mod.documents.gui.DocEditorScreen;
import net.fexcraft.mod.documents.gui.DocViewerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import static net.neoforged.fml.common.Mod.EventBusSubscriber.Bus.*;

@Mod.EventBusSubscriber(modid = "documents", bus = MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event){
        MenuScreens.register((MenuType)Documents.DOC_EDITOR.get(), DocEditorScreen::new);
        MenuScreens.register((MenuType)Documents.DOC_VIEWER.get(), DocViewerScreen::new);
    }

}
