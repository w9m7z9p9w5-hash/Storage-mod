package com.istore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class IStoreClient implements ClientModInitializer {
    /**
     * The block that opens the storage screen when you right-click it.
     * Lodestone: nothing in vanilla happens when you right-click it with an empty hand, so it never
     * clashes with anything. To use a different block, change this one line (e.g. Blocks.ENDER_CHEST).
     */
    public static final Block STORAGE_BLOCK = Blocks.LODESTONE;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            // This event also fires on the integrated server in singleplayer; only act for the client player.
            if (!(player instanceof LocalPlayer)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            // Sneaking lets you place blocks against it; a compass still binds to it like normal.
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            if (player.getItemInHand(hand).is(Items.COMPASS)) return InteractionResult.PASS;
            if (!level.getBlockState(hitResult.getBlockPos()).is(STORAGE_BLOCK)) return InteractionResult.PASS;

            StorageMenu.open(Minecraft.getInstance());
            // Anything but PASS stops the click here, so the server never hears about it.
            return InteractionResult.SUCCESS;
        });

        // Swallows the data pack's sync message and the "Executed N commands" notice so chat stays clean.
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> StorageData.allowIncoming(message));
    }
}
