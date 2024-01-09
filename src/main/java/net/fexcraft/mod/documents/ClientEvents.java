package net.fexcraft.mod.documents;

import com.mojang.blaze3d.platform.ScreenManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import static net.neoforged.fml.common.Mod.EventBusSubscriber.Bus.*;

@Mod.EventBusSubscriber(modid = "documents", bus = MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event){
        //TODO ScreenManager.register(Documents.DOC_EDITOR.get(), DocEditorScreen::new);
        //TODO ScreenManager.register(Documents.DOC_VIEWER.get(), DocViewerScreen::new);
    }

}
