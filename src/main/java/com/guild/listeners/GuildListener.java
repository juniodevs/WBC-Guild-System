package com.guild.listeners;

import com.guild.GuildPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

/**
 * Listener de eventos da guilda
 */
public class GuildListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public GuildListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Evento de chat do jogador (pode ser usado para funcionalidade de chat da guilda)
     */
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        // Aqui pode adicionar funcionalidade de chat da guilda
        // Por exemplo, detectar prefixo da guilda, processar chat da guilda, etc.
    }
}
