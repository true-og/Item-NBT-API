package de.tr7zw.nbtapi.plugin.tests.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Lists;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.nbtapi.plugin.tests.Test;

public class ItemConversionTest implements Test {

    @Override
    public void test() throws Exception {

        ItemStack item = new ItemStack(Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Lists.newArrayList("Firest Line", "Second Line"));
        item.setItemMeta(meta);
        String nbt = NBTItem.convertItemtoNBT(item).toString();
        if (!nbt.contains("Firest Line") || !nbt.contains("Second Line"))
            throw new NbtApiException("The Item nbt '" + nbt + "' didn't contain the lore");
        ItemStack rebuild = NBTItem.convertNBTtoItem(new NBTContainer(nbt));
        if (!item.isSimilar(rebuild))
            throw new NbtApiException("Rebuilt item did not match the original!");

        NBTContainer cont = new NBTContainer();
        cont.setItemStack("testItem", item);
        if (!item.isSimilar(cont.getItemStack("testItem")))
            throw new NbtApiException("Rebuilt item did not match the original!");

    }

}
