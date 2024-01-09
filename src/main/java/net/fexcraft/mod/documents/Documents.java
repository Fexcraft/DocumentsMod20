package net.fexcraft.mod.documents;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fexcraft.mod.documents.data.Document;
import net.fexcraft.mod.documents.data.DocumentItem;
import net.fexcraft.mod.documents.packet.GuiPacket;
import net.fexcraft.mod.documents.packet.SyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Documents.MODID)
public class Documents {

    public static final String MODID = "documents";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> DOCITEM = ITEMS.registerItem("document", func -> new DocumentItem());
    public static final ResourceLocation SYNC_PACKET = new ResourceLocation(MODID, "sync");
    public static final ResourceLocation UI_PACKET = new ResourceLocation(MODID, "ui");

    public Documents(IEventBus eventbus){
        eventbus.addListener(this::commonSetup);
        ITEMS.register(eventbus);
        NeoForge.EVENT_BUS.register(this);
        eventbus.addListener(this::addCreative);
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

    @Mod.EventBusSubscriber(modid = "documents")
    public static class Events {

        @SubscribeEvent
        public static void register(final RegisterPayloadHandlerEvent event) {
            final IPayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0").optional();
            registrar.common(SYNC_PACKET, SyncPacket::read, handler -> handler.client(SyncPacket::handle));
            registrar.common(UI_PACKET, GuiPacket::read, handler -> {
                handler.server(GuiPacket::handle_server);
                handler.client(GuiPacket::handle_client);
            });
        }

        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event){
            if(event.getEntity().level().isClientSide) return;
            DocRegistry.opj(event.getEntity());
            PacketDistributor.PLAYER.with((ServerPlayer)event.getEntity()).send(new SyncPacket(DocRegistry.confmap));
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event){
            if(event.getEntity().level().isClientSide) return;
            DocRegistry.opl(event.getEntity());
        }

        @SubscribeEvent
        public static void onCmdReg(RegisterCommandsEvent event){
            event.getDispatcher().register(Commands.literal("documents")
                .then(Commands.literal("list").executes(cmd -> {
                    cmd.getSource().sendSystemMessage(Component.literal("\u00A77============"));
                    for(String str : DocRegistry.DOCS.keySet()){
                        cmd.getSource().sendSystemMessage(Component.literal(str));
                    }
                    return 0;
                }))
                .then(Commands.literal("uuid").executes(cmd -> {
                    LOGGER.info(cmd.getSource().getPlayerOrException().toString());
                    cmd.getSource().sendSystemMessage(Component.literal(cmd.getSource().getPlayerOrException().getGameProfile().getId().toString()));
                    return 0;
                }))
                .then(Commands.literal("reload-perms").executes(cmd -> {
                    Player entity = cmd.getSource().getPlayerOrException();
                    if(!DocPerms.hasPerm(entity, "command.reload-perms") && !entity.hasPermissions(4)){
                        cmd.getSource().sendFailure(Component.translatable("documents.cmd.no_permission"));
                        return 1;
                    }
                    DocPerms.loadperms();
                    cmd.getSource().sendSystemMessage(Component.translatable("documents.cmd.perms_reloaded"));
                    return 0;
                }))
                .then(Commands.literal("reload-docs").executes(cmd -> {
                    Player entity = cmd.getSource().getPlayerOrException();
                    if(!DocPerms.hasPerm(entity, "command.reload-docs") && !entity.hasPermissions(4)){
                        cmd.getSource().sendFailure(Component.translatable("documents.cmd.no_permission"));
                        return 1;
                    }
                    DocRegistry.init();
                    entity.getServer().getPlayerList().getPlayers().forEach(player -> {
            PacketDistributor.PLAYER.with(player).send(new SyncPacket(DocRegistry.confmap));
                    });
                    cmd.getSource().sendSystemMessage(Component.translatable("documents.cmd.docs_reloaded"));
                    return 0;
                }))
                .then(Commands.literal("get").then(Commands.argument("id", StringArgumentType.word()).executes(cmd -> {
                    Document doc = DocRegistry.DOCS.get(cmd.getArgument("id", String.class));
                    if(doc == null){
                        cmd.getSource().sendFailure(Component.translatable("documents.cmd.doc_not_found"));
                        return -1;
                    }
                    else{
                        if(!DocPerms.hasPerm(cmd.getSource().getPlayerOrException(), "command.get", doc.id)){
                            cmd.getSource().sendSystemMessage(Component.translatable("documents.cmd.no_permission"));
                            return -1;
                        }
                        ItemStack stack = new ItemStack(DocumentItem.INSTANCE);
                        CompoundTag com = stack.hasTag() ? stack.getTag() : new CompoundTag();
                        com.putString(DocumentItem.NBTKEY, doc.id);
                        stack.setTag(com);
                        cmd.getSource().getPlayerOrException().addItem(stack);
                        cmd.getSource().sendSystemMessage(Component.translatable("documents.cmd.added"));
                        LOGGER.info(com.toString());
                    }
                    return 0;
                })))
                .executes(cmd -> {
                    cmd.getSource().sendSystemMessage(Component.literal("\u00A77============"));
                    cmd.getSource().sendSystemMessage(Component.literal("/documents list"));
                    cmd.getSource().sendSystemMessage(Component.literal("/documents get"));
                    cmd.getSource().sendSystemMessage(Component.literal("/documents uuid"));
                    cmd.getSource().sendSystemMessage(Component.literal("/documents reload-perms"));
                    cmd.getSource().sendSystemMessage(Component.literal("/documents reload-docs"));
                    cmd.getSource().sendSystemMessage(Component.literal("\u00A77============"));
                    return 0;
                })
            );
        }

    }

}
