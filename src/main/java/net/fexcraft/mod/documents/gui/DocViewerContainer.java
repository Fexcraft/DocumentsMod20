package net.fexcraft.mod.documents.gui;

import net.fexcraft.mod.documents.DocRegistry;
import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.data.Document;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DocViewerContainer extends AbstractContainerMenu implements UiPacketReceiver {

    protected ItemStack stack;
    protected Document doc;
    protected DocViewerScreen screen;
    protected Player player;
    protected int page = 0;

    public DocViewerContainer(int id, Inventory inv){
        super(Documents.DOC_VIEWER.get(), id);
        stack = inv.player.getItemInHand(InteractionHand.MAIN_HAND);
        player = inv.player;
        doc = DocRegistry.get(stack);
    }

    public DocViewerContainer(int id, Inventory inv, FriendlyByteBuf buffer){
        this(id, inv);
        page = buffer.readInt();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i){
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player){
        return true;
    }

    @Override
    public void onPacket(CompoundTag com, boolean client){
        Documents.LOGGER.info(client + " " + com);
        if(com.contains("open_page")){
            if(client) return;
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName(){
                    return Component.literal("Document Viewer");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player){
                    return new DocViewerContainer(i, inventory);
                }
            }, buf -> buf.writeInt(com.getInt("open_page")));
            return;
        }
    }

    public String getValue(String str){
        return stack.getTag().getString("document:" + str);
    }

}
