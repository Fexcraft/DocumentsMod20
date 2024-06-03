package net.fexcraft.mod.documents.packet;

import net.fexcraft.app.json.JsonHandler;
import net.fexcraft.app.json.JsonMap;
import net.fexcraft.mod.documents.data.DocRegistry;
import net.fexcraft.mod.documents.Documents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPacket(JsonMap map) implements CustomPacketPayload {

	@Override
	public void write(FriendlyByteBuf buf){
		String string = JsonHandler.toString(map, JsonHandler.PrintOption.FLAT);
		buf.writeInt(string.length());
		buf.writeUtf(string);
	}

	@Override
	public ResourceLocation id(){
		return Documents.SYNC_PACKET;
	}

	public static SyncPacket read(FriendlyByteBuf buf){
		return new SyncPacket(JsonHandler.parse(buf.readUtf(buf.readInt()), true).asMap());
	}

	public void handle(IPayloadContext context){
		context.workHandler().submitAsync(() -> {
			Documents.LOGGER.info(map.toString());
			DocRegistry.load(map);
			DocRegistry.DOCS.values().forEach(doc -> doc.linktextures());
		});
	}

}
