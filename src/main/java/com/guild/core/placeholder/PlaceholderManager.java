package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.services.GuildService;

public class PlaceholderManager {
    
    private final GuildPlugin plugin;
    private GuildService guildService;
    private GuildPlaceholderExpansion placeholderExpansion;
    private boolean placeholderApiAvailable = false;
    
    public PlaceholderManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.guildService = null; 
    }
    
    public void setGuildService(GuildService guildService) {
        this.guildService = guildService;
    }
    
    public void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderExpansion = new GuildPlaceholderExpansion(plugin, guildService);
                placeholderExpansion.register();
                placeholderApiAvailable = true;
                plugin.getLogger().info("Placeholders do PlaceholderAPI registrados com sucesso");
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao inicializar PlaceholderAPI: " + e.getMessage());
                placeholderApiAvailable = false;
            }
        } else {
            plugin.getLogger().warning("PlaceholderAPI não encontrado, a funcionalidade de placeholders não estará disponível");
            placeholderApiAvailable = false;
        }
    }
    
    public boolean isPlaceholderApiAvailable() {
        return placeholderApiAvailable;
    }
}
