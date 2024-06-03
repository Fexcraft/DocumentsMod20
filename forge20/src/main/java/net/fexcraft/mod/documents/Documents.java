package net.fexcraft.mod.documents;

import com.mojang.logging.LogUtils;
import net.fexcraft.mod.documents.data.DocumentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.fexcraft.mod.documents.gui.DocEditorContainer;
import net.fexcraft.mod.documents.gui.DocViewerContainer;
import org.slf4j.Logger;

@Mod(Documents.MODID)
public class Documents {

	public static final String MODID = "examplemod";
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
	public static final RegistryObject<Item> DOCITEM = ITEMS.register("document", () -> new DocumentItem());
	public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
	public static final RegistryObject<MenuType<DocEditorContainer>> DOC_EDITOR = CONTAINERS.register("editor", () -> IForgeMenuType.create(DocEditorContainer::new));
	public static final RegistryObject<MenuType<DocViewerContainer>> DOC_VIEWER = CONTAINERS.register("viewer", () -> IForgeMenuType.create(DocViewerContainer::new));

	public Documents(){
		IEventBus eventbus = FMLJavaModLoadingContext.get().getModEventBus();
		eventbus.addListener(this::commonSetup);
		ITEMS.register(eventbus);
		CONTAINERS.register(eventbus);
		MinecraftForge.EVENT_BUS.register(this);
		eventbus.addListener(this::addCreative);
	}

	private void commonSetup(final FMLCommonSetupEvent event){
		DocRegistry.init();
		DocPerms.loadperms();
	}

	private void addCreative(BuildCreativeModeTabContentsEvent event){
		if(event.getTabKey() == CreativeModeTabs.INGREDIENTS)
			event.accept(DOCITEM);
	}

	@Mod.EventBusSubscriber(modid = "documents")
	public static class Events {

		@SubscribeEvent
		public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
			if(event.getEntity().level().isClientSide) return;
			DocRegistry.opj(event.getEntity());
			//TODO PacketDistributor.PLAYER.with((ServerPlayer)event.getEntity()).send(new SyncPacket(DocRegistry.confmap));
		}

		@SubscribeEvent
		public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event){
			if(event.getEntity().level().isClientSide) return;
			DocRegistry.opl(event.getEntity());
		}

	}

}
