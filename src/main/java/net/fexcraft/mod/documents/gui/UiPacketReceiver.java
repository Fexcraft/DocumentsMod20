package net.fexcraft.mod.documents.gui;

import net.fexcraft.mod.documents.Documents;
import net.fexcraft.mod.documents.packet.GuiPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public interface UiPacketReceiver {

	public void onPacket(CompoundTag com, boolean client);

	public default void send(boolean toclient, CompoundTag compound, Player player){
		if(toclient){
			PacketDistributor.PLAYER.with((ServerPlayer)player).send(new GuiPacket(compound));
		}
		else{
			PacketDistributor.SERVER.noArg().send(new GuiPacket(compound));
		}
	}

}
