package net.fexcraft.mod.documents;

import net.fexcraft.mod.documents.gui.DocEditorScreen;
import net.fexcraft.mod.documents.gui.DocViewerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import static net.neoforged.fml.common.Mod.EventBusSubscriber.Bus.*;

@Mod.EventBusSubscriber(modid = "documents", bus = MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void clientInit(RegisterMenuScreensEvent event){
        event.register(Documents.DOC_EDITOR.get(), DocEditorScreen::new);
        event.register(Documents.DOC_VIEWER.get(), DocViewerScreen::new);
    }

}
