package com.bgsoftware.wildtools.nms;

import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockBeetroot;
import net.minecraft.server.v1_11_R1.BlockCarrots;
import net.minecraft.server.v1_11_R1.BlockCocoa;
import net.minecraft.server.v1_11_R1.BlockCrops;
import net.minecraft.server.v1_11_R1.BlockNetherWart;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.BlockPotatoes;
import net.minecraft.server.v1_11_R1.EnchantmentManager;
import net.minecraft.server.v1_11_R1.Enchantments;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.EnumColor;
import net.minecraft.server.v1_11_R1.Item;
import net.minecraft.server.v1_11_R1.ItemStack;
import net.minecraft.server.v1_11_R1.Items;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.PlayerInventory;
import net.minecraft.server.v1_11_R1.World;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;

import org.bukkit.CropState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;

import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({"unused", "deprecation", "ConstantConditions"})
public final class NMSAdapter_v1_11_R1 implements NMSAdapter {

    @Override
    public String getVersion() {
        return "v1_11_R1";
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getBlockDrops(Player pl, org.bukkit.block.Block bl, boolean silkTouch) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        BlockPosition blockPosition = new BlockPosition(bl.getX(), bl.getY(), bl.getZ());
        World world = player.world;
        Block block = world.getType(blockPosition).getBlock();

        if(!player.hasBlock(block.getBlockData()) || player.playerInteractManager.isCreative())
            return drops;

        // Has silk touch enchant
        if ((block.r() && !block.isTileEntity()) && (silkTouch || EnchantmentManager.a(Enchantments.SILK_TOUCH, player) > 0)) {

            int i = 0;
            Item item = Item.getItemOf(block);
            if (item != null && item.l()) {
                i = block.toLegacyData(block.getBlockData());
            }

            ItemStack itemStack = new ItemStack(item, 1, i);
            drops.add(CraftItemStack.asBukkitCopy(itemStack));
        }

        else if (!world.isClientSide) {
            int fortuneLevel = EnchantmentManager.a(Enchantments.LOOT_BONUS_BLOCKS, player),
                    dropCount = block.getDropCount(fortuneLevel, world.random);

            for(int i = 0; i < dropCount; i++) {
                if (world.random.nextFloat() < 1.0F) {
                    Item item = block.getDropType(block.getBlockData(), world.random, fortuneLevel);
                    if (item != null) {
                        ItemStack itemStack = new ItemStack(item, 1, block.getDropData(block.getBlockData()));
                        drops.add(CraftItemStack.asBukkitCopy(itemStack));
                    }
                }
            }
        }

        return drops;
    }

    @Override
    public List<org.bukkit.inventory.ItemStack> getCropDrops(Player pl, org.bukkit.block.Block bl) {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();

        EntityPlayer player = ((CraftPlayer) pl).getHandle();
        BlockPosition blockPosition = new BlockPosition(bl.getX(), bl.getY(), bl.getZ());
        World world = player.world;
        Block block = world.getType(blockPosition).getBlock();

        int age = ((CraftBlock) bl).getData();
        int fortuneLevel = EnchantmentManager.a(Enchantments.LOOT_BONUS_BLOCKS, player);

        if(block instanceof BlockCrops){
            int growthAge = 7;

            if(block instanceof BlockBeetroot)
                growthAge = 3;

            if (age >= growthAge) {
                //Give the item itself to the player
                if(block instanceof BlockCarrots) {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                }else if(block instanceof BlockPotatoes){
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                }else if(block instanceof BlockBeetroot) {
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.BEETROOT, 1, 0)));
                }else{
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.WHEAT, 1, 0)));
                }
                //Give the "seeds" to the player. I run -1 iteration for "replant"
                for(int i = 0; i < (fortuneLevel + 3) - 1; i++) {
                    if (world.random.nextInt(2 * growthAge) <= age) {
                        if(block instanceof BlockCarrots) {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.CARROT, 1, 0)));
                        }else if(block instanceof BlockPotatoes){
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POTATO, 1, 0)));
                            if (world.random.nextInt(50) == 0) {
                                drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.POISONOUS_POTATO, 1, 0)));
                            }
                        }else if(block instanceof BlockBeetroot) {
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.BEETROOT_SEEDS, 1, 0)));
                        }else{
                            drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.WHEAT_SEEDS, 1, 0)));
                        }
                    }
                }
            }
        }
        else if(block instanceof BlockCocoa){
            if(age >= 2) {
                for (int i = 0; i < 3; i++)
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.DYE, 1, EnumColor.BROWN.getInvColorIndex())));
            }
        }
        else if(block instanceof BlockNetherWart){
            if (age >= 3) {
                int amount = 2 + world.random.nextInt(3);
                if (fortuneLevel > 0) {
                    amount += world.random.nextInt(fortuneLevel + 1);
                }

                for(int i = 0; i < amount; i++)
                    drops.add(CraftItemStack.asBukkitCopy(new ItemStack(Items.NETHER_WART)));
            }
        }

        return drops;
    }

    @Override
    public int getIntTag(org.bukkit.inventory.ItemStack is, String key, int def) {
        ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
        NBTTagCompound tag = new NBTTagCompound();

        if(nmsStack.hasTag()){
            tag = nmsStack.getTag();
        }

        if(tag.hasKey(key)){
            return tag.getInt(key);
        }

        return def;
    }

    @Override
    public org.bukkit.inventory.ItemStack setIntTag(org.bukkit.inventory.ItemStack is, String key, int value) {
        ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
        NBTTagCompound tag = new NBTTagCompound();

        if(nmsStack.hasTag()){
            tag = nmsStack.getTag();
        }

        tag.setInt(key, value);

        nmsStack.setTag(tag);

        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public org.bukkit.inventory.ItemStack getItemInHand(Player player) {
        ItemStack itemStack = ((CraftInventoryPlayer) player.getInventory()).getInventory().getItemInHand();
        return CraftItemStack.asBukkitCopy(itemStack);
    }

    @Override
    public void setItemInHand(Player player, org.bukkit.inventory.ItemStack itemStack) {
        PlayerInventory playerInventory = ((CraftInventoryPlayer) player.getInventory()).getInventory();
        playerInventory.setItem(playerInventory.itemInHandIndex, CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public boolean isFullyGrown(org.bukkit.block.Block block) {
        if(block.getState().getData() instanceof Crops)
            return ((Crops) block.getState().getData()).getState() == CropState.RIPE;
        else if(block.getState().getData() instanceof CocoaPlant)
            return ((CocoaPlant) block.getState().getData()).getSize() == CocoaPlant.CocoaPlantSize.LARGE;
        else if(block.getState().getData() instanceof NetherWarts)
            return ((NetherWarts) block.getState().getData()).getState() == NetherWartsState.RIPE;

        return true;
    }

    @Override
    public void setCropState(org.bukkit.block.Block block, CropState cropState) {
        if(block.getType() == Material.COCOA){
            CocoaPlant cocoaPlant = (CocoaPlant) block.getState().getData();
            switch (cropState){
                case SEEDED:
                case GERMINATED:
                case VERY_SMALL:
                case SMALL:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.SMALL);
                    break;
                case MEDIUM:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.MEDIUM);
                    break;
                case TALL:
                case VERY_TALL:
                case RIPE:
                    cocoaPlant.setSize(CocoaPlant.CocoaPlantSize.LARGE);
                    break;
            }
            ((CraftBlock) block).setData(cocoaPlant.getData());
        }else {
            ((CraftBlock) block).setData(cropState.getData());
        }
    }

    @Override
    public void copyBlock(org.bukkit.block.Block from, org.bukkit.block.Block to) {
        CraftBlock fromBlock = (CraftBlock) from, toBlock = (CraftBlock) to;
        toBlock.setType(fromBlock.getType());
        toBlock.setData(fromBlock.getData());
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    @Override
    public void setAirFast(org.bukkit.block.Block block) {
        World world = ((CraftWorld) block.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        world.setTypeAndData(blockPosition, Block.getByCombinedId(0), 2);
    }

    @Override
    public Enchantment getGlowEnchant() {
        return new Enchantment(101) {
            @Override
            public String getName() {
                return "WildToolsGlow";
            }

            @Override
            public int getMaxLevel() {
                return 1;
            }

            @Override
            public int getStartLevel() {
                return 0;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return null;
            }

            @Override
            public boolean conflictsWith(Enchantment enchantment) {
                return false;
            }

            @Override
            public boolean canEnchantItem(org.bukkit.inventory.ItemStack itemStack) {
                return true;
            }

            @Override
            public boolean isTreasure() {
                return false;
            }

            @Override
            public boolean isCursed() {
                return false;
            }
        };
    }

}