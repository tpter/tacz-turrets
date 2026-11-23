package com.taczturrets.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item that documents how the turret base is used, so the rules are visible without a wiki. */
public class TurretBlockItem extends BlockItem {
    public TurretBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        String base = this.getBlock().getDescriptionId();
        for (int line = 1; line <= 4; line++) {
            String key = base + ".tooltip" + line;
            Component text = Component.translatable(key);
            if (!text.getString().equals(key)) {
                tooltip.add(text.copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
