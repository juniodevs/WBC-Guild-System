package com.guild.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utilitário para serializar e desserializar banners (estandartes) em Base64 e JSON
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
    
    /**
     * Serializa um ItemStack de banner em JSON
     * Formato: { "base_color": "WHITE", "patterns": [{ "color": "BLACK", "pattern": "STRIPE_TOP" }] }
     * 
     * @param banner ItemStack do banner a ser serializado
     * @return String JSON representando o banner, ou null se houver erro
     */
    public static String serializeToJson(ItemStack banner) {
        if (banner == null || !isBanner(banner)) {
            return null;
        }
        
        try {
            BannerMeta meta = (BannerMeta) banner.getItemMeta();
            if (meta == null) {
                return null;
            }
            
            JsonObject json = new JsonObject();
            
            // Cor base - extrair do material do banner
            String materialName = banner.getType().name();
            String colorName = materialName.replace("_BANNER", "");
            DyeColor baseColor;
            try {
                baseColor = DyeColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                baseColor = DyeColor.WHITE;
            }
            json.addProperty("base_color", baseColor.name());
            
            // Padrões
            JsonArray patterns = new JsonArray();
            for (Pattern pattern : meta.getPatterns()) {
                JsonObject p = new JsonObject();
                p.addProperty("color", pattern.getColor().name());
                p.addProperty("pattern", pattern.getPattern().name());
                patterns.add(p);
            }
            json.add("patterns", patterns);
            
            return json.toString();
            
        } catch (Exception e) {
            logger.severe("Erro ao serializar banner para JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Desserializa uma string JSON em um ItemStack de banner
     * 
     * @param json String JSON representando o banner
     * @return ItemStack do banner, ou banner branco padrão se houver erro
     */
    public static ItemStack deserializeFromJson(String json) {
        if (json == null || json.isEmpty()) {
            return getDefaultBanner();
        }
        
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            
            // Cor base
            String baseColorName = jsonObject.get("base_color").getAsString();
            DyeColor baseColor;
            try {
                baseColor = DyeColor.valueOf(baseColorName);
            } catch (IllegalArgumentException e) {
                baseColor = DyeColor.WHITE;
            }
            
            // Criar banner com cor base
            Material bannerMaterial = Material.valueOf(baseColor.name() + "_BANNER");
            ItemStack banner = new ItemStack(bannerMaterial);
            BannerMeta meta = (BannerMeta) banner.getItemMeta();
            
            if (meta != null) {
                // Adicionar padrões
                JsonArray patterns = jsonObject.getAsJsonArray("patterns");
                if (patterns != null) {
                    for (int i = 0; i < patterns.size(); i++) {
                        JsonObject patternObj = patterns.get(i).getAsJsonObject();
                        
                        String colorName = patternObj.get("color").getAsString();
                        String patternName = patternObj.get("pattern").getAsString();
                        
                        try {
                            DyeColor color = DyeColor.valueOf(colorName);
                            PatternType patternType = PatternType.valueOf(patternName);
                            
                            meta.addPattern(new Pattern(color, patternType));
                        } catch (IllegalArgumentException e) {
                            logger.warning("Padrão inválido ignorado: " + colorName + " " + patternName);
                        }
                    }
                }
                
                banner.setItemMeta(meta);
            }
            
            return banner;
            
        } catch (Exception e) {
            logger.severe("Erro ao desserializar banner do JSON: " + e.getMessage());
            return getDefaultBanner();
        }
    }
}
