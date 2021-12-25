package net.ingen084.fireworksarrow;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import net.md_5.bungee.api.ChatColor;

public class Plugin extends JavaPlugin implements Listener
{
    NamespacedKey recipeKey;
    NamespacedKey nbtKey;

    NamespacedKey torchRecipeKey;
    NamespacedKey torchNbtKey;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        nbtKey = new NamespacedKey(this, "fireworks_data");
        recipeKey = new NamespacedKey(this, "fireworks_arrow");
        Bukkit.addRecipe(new ShapelessRecipe(recipeKey, new ItemStack(Material.TIPPED_ARROW))
            .addIngredient(Material.FIREWORK_ROCKET)
            .addIngredient(Material.ARROW));
        
        torchNbtKey = new NamespacedKey(this, "torch_data");
        torchRecipeKey = new NamespacedKey(this, "torch_arrow");
        Bukkit.addRecipe(new ShapelessRecipe(torchRecipeKey, new ItemStack(Material.SPECTRAL_ARROW))
            .addIngredient(Material.TORCH)
            .addIngredient(Material.ARROW));
    }

    @EventHandler
    public void onPrepareCraftEvent(PrepareItemCraftEvent event) {
        var inv = event.getInventory();
        if (event.getRecipe() instanceof ShapelessRecipe && ((ShapelessRecipe)event.getRecipe()).getKey().equals(recipeKey)) {
            FireworkMeta fmeta = null;
            for (var i : inv.getStorageContents()) {
                if (i.getType() == Material.FIREWORK_ROCKET) {
                    fmeta = (FireworkMeta)i.getItemMeta();
                    break;
                }
            }
            if (fmeta == null) {
                inv.setResult(null);
                return;
            }

            var res = inv.getResult();
            var meta = res.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            meta.addEnchant(Enchantment.DURABILITY, 1, false);
            meta.setDisplayName(ChatColor.RESET + "" + ChatColor.AQUA + "花火がつけられた矢");
            meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.BYTE_ARRAY, FireworkMetaSerializer.serialize(fmeta));
            res.setItemMeta(meta);
        } else if (event.getRecipe() instanceof ShapelessRecipe && ((ShapelessRecipe)event.getRecipe()).getKey().equals(torchRecipeKey)) {
            var res = inv.getResult();
            var meta = res.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
            meta.addEnchant(Enchantment.DURABILITY, 1, false);
            meta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "松明がつけられた矢");
            meta.getPersistentDataContainer().set(torchNbtKey, PersistentDataType.INTEGER, 1);
            res.setItemMeta(meta);
        }
    }

    @EventHandler
    public void shootBowEvent(EntityShootBowEvent event) {
        var meta = event.getArrowItem().getItemMeta();
        if (meta == null)
            return;
        if (meta.getPersistentDataContainer().has(nbtKey, PersistentDataType.BYTE_ARRAY)) {
            var ent = event.getProjectile();
            ent.getPersistentDataContainer().set(nbtKey, PersistentDataType.INTEGER, 1);
            var firework = (Firework)ent.getWorld().spawn(ent.getLocation(), Firework.class);
            firework.setFireworkMeta(FireworkMetaSerializer.deserialize(meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.BYTE_ARRAY)));
            firework.setVelocity(ent.getVelocity().normalize());
            ent.addPassenger(firework);
            event.setConsumeArrow(true);
        } else if (meta.getPersistentDataContainer().has(torchNbtKey, PersistentDataType.INTEGER)) {
            var ent = event.getProjectile();
            ent.getPersistentDataContainer().set(torchNbtKey, PersistentDataType.INTEGER, 1);
            event.setConsumeArrow(true);
        }
    }

    @EventHandler
    public void arrowHitEvent(ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.ARROW && event.getEntity().getPersistentDataContainer().has(nbtKey, PersistentDataType.INTEGER)) {
            if (event.getEntity().getPassengers().size() > 0) {
                for (var e : event.getEntity().getPassengers()) {
                    if (e instanceof Firework)
                        ((Firework)e).detonate();
                }
                event.getEntity().remove();
            }
        } else if (event.getEntityType() == EntityType.SPECTRAL_ARROW && event.getEntity().getPersistentDataContainer().has(torchNbtKey, PersistentDataType.INTEGER)) {
            if (event.getHitBlock() != null) {
                var face = event.getHitBlockFace();
                var opp = face.getOppositeFace();
                var target = event.getHitBlock().getRelative(face);

                if (!target.getType().isAir() || face == BlockFace.DOWN || !target.getRelative(opp).isSolid()) {
                    target.getWorld().dropItem(target.getLocation().add(.5, .5, .5), new ItemStack(Material.TORCH));
                } else {
                    if (opp == BlockFace.DOWN) {
                        target.setType(Material.TORCH);
                    } else {
                        target.setType(Material.WALL_TORCH);
                        if (target.getBlockData() instanceof Directional) {
                            var dir = (Directional)target.getBlockData();
                            dir.setFacing(face);
                            target.setBlockData(dir);
                        }
                    }
                }
                
                event.getEntity().getPersistentDataContainer().remove(torchNbtKey);
            }
            event.getEntity().remove();
        }
    }
}
