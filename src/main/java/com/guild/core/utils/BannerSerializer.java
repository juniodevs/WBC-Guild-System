package com.guild.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Utilitário para serializar e desserializar banners (estandartes) em Base64
 */
public class BannerSerializer {
    
    private static final Logger logger = Logger.getLogger("BannerSerializer");
    
    /**
     * Serializa um ItemStack de banner em uma string Base64
     * 
     * @param banner ItemStack do banner a ser serializado
     * @return String Base64 representando o banner, ou null se houver erro
     */
    public static String serialize(ItemStack banner) {
        if (banner == null || !isBanner(banner)) {
            return null;
        }
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeObject(banner);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
        } catch (IOException e) {
            logger.severe("Erro ao serializar banner: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Desserializa uma string Base64 em um ItemStack de banner
     * 
     * @param base64 String Base64 representando o banner
     * @return ItemStack do banner, ou banner branco padrão se houver erro
     */
    public static ItemStack deserialize(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return getDefaultBanner();
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            
            ItemStack banner = (ItemStack) dataInput.readObject();
            
            // Validar se é um banner válido
            if (isBanner(banner)) {
                return banner;
            } else {
                logger.warning("ItemStack desserializado não é um banner válido");
                return getDefaultBanner();
            }
            
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Erro ao desserializar banner: " + e.getMessage());
            return getDefaultBanner();
        }
    }
    
    /**
     * Retorna um banner branco padrão
     * 
     * @return ItemStack de um banner branco
     */
    public static ItemStack getDefaultBanner() {
        return new ItemStack(Material.WHITE_BANNER);
    }
    
    /**
     * Verifica se um ItemStack é um banner válido
     * 
     * @param item ItemStack a ser verificado
     * @return true se for um banner, false caso contrário
     */
    public static boolean isBanner(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        String materialName = item.getType().name();
        return materialName.endsWith("_BANNER");
    }
}
