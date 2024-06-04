package net.fexcraft.mod.documents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fexcraft.mod.documents.data.Document;
import net.fexcraft.mod.documents.data.DocumentItem;
import net.fexcraft.mod.documents.gui.DocEditorContainer;
import net.fexcraft.mod.documents.gui.DocViewerContainer;
import net.fexcraft.mod.documents.packet.GuiPacketN;
import net.fexcraft.mod.documents.packet.SyncPacketN;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.File;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Documents.MODID)
public class Documents {

	public static final String MODID = "documents";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
	public static final DeferredItem<Item> DOCITEM = ITEMS.registerItem("document", func -> new DocumentItem());
	public static final ResourceLocation SYNC_PACKET = new ResourceLocation(MODID, "sync");
	public static final ResourceLocation UI_PACKET = new ResourceLocation(MODID, "ui");
	public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<DocEditorContainer>> DOC_EDITOR = CONTAINERS.register("editor", () -> IMenuTypeExtension.create(DocEditorContainer::new));
	public static final DeferredHolder<MenuType<?>, MenuType<DocViewerContainer>> DOC_VIEWER = CONTAINERS.register("viewer", () -> IMenuTypeExtension.create(DocViewerContainer::new));

	public Documents(IEventBus eventbus){
		eventbus.addListener(this::commonSetup);
		ITEMS.register(eventbus);
		CONTAINERS.register(eventbus);
		NeoForge.EVENT_BUS.register(this);
		eventbus.addListener(this::addCreative);
	}

	public static void sendSyncTo(ServerPlayer player){
		PacketDistributor.PLAYER.with(player).send(new SyncPacketN(DocRegistry.confmap));
	}

	public static MinecraftServer getCurrentServer(){
		return ServerLifecycleHooks.getCurrentServer();
	}

	public static void send(boolean toclient, CompoundTag compound, Player player){
		if(toclient){
			PacketDistributor.PLAYER.with((ServerPlayer)player).send(new GuiPacketN(compound));
		}
		else{
			PacketDistributor.SERVER.noArg().send(new GuiPacketN(compound));
		}
	}

	public static File getConfigDir(){
		return FMLPaths.CONFIGDIR.get().toFile();
	}

	public static void openViewer(Player player, int page){
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName(){
				return Component.literal("Document Viewer");
			}

			@Override
			public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player){
				return new DocViewerContainer(i, inventory);
			}
		}, buf -> buf.writeInt(page));
	}

	public static void openViewerOrEditor(Player player, CompoundTag com){
		player.openMenu(new MenuProvider() {
			@Override
			public Component getDisplayName(){
				return Component.literal(com.contains("document:issued") ? "Document Viewer" : "Document Editor");
			}

			@Override
			public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player){
				return com.contains("document:issued") ? new DocViewerContainer(i, inventory) : new DocEditorContainer(i, inventory);
			}
		}, buf -> buf.writeInt(0));
	}

	private void commonSetup(final FMLCommonSetupEvent event){
		DocRegistry.init();
		DocPerms.loadperms();
	}

	private void addCreative(BuildCreativeModeTabContentsEvent event){
		if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(DOCITEM);
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event){
		//
	}

	@Mod.EventBusSubscriber(modid = "documents", bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {

		@SubscribeEvent
		public static void register(final RegisterPayloadHandlerEvent event){
			final IPayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0").optional();
			registrar.common(SYNC_PACKET, SyncPacketN::read, handler -> handler.client(SyncPacketN::handle));
			registrar.common(UI_PACKET, GuiPacketN::read, handler -> {
				handler.server(GuiPacketN::handle_server);
				handler.client(GuiPacketN::handle_client);
			});
		}

	}

	@Mod.EventBusSubscriber(modid = "documents")
	public static class Events {

		@SubscribeEvent
		public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
			if(event.getEntity().level().isClientSide) return;
			DocRegistry.opj(event.getEntity());
			PacketDistributor.PLAYER.with((ServerPlayer)event.getEntity()).send(new SyncPacketN(DocRegistry.confmap));
		}

		@SubscribeEvent
		public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event){
			if(event.getEntity().level().isClientSide) return;
			DocRegistry.opl(event.getEntity());
		}

		@SubscribeEvent
		public static void onCmdReg(RegisterCommandsEvent event){
			event.getDispatcher().register(DocumentsCommand.get());
		}

	}

}
