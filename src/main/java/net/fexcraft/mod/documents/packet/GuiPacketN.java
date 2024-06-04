package net.fexcraft.mod.documents.packet;

import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.gui.UiPacketReceiver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GuiPacketN(CompoundTag com) implements CustomPacketPayload {

    @Override
    public void write(FriendlyByteBuf buf){
        buf.writeNbt(com);
    }

    @Override
    public ResourceLocation id(){
        return Documents.UI_PACKET;
    }

	public static GuiPacketN read(FriendlyByteBuf buf){
		return new GuiPacketN(buf.readNbt());
	}

	public void handle_client(IPayloadContext context){
		context.workHandler().submitAsync(() -> {
			((UiPacketReceiver)net.minecraft.client.Minecraft.getInstance().player.containerMenu).onPacket(com, true);
		});
	}

	public void handle_server(IPayloadContext context){
		context.workHandler().submitAsync(() -> {
			((UiPacketReceiver)context.player().get().containerMenu).onPacket(com, false);
		});
	}

}
