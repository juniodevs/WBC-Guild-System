package com.guild.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import com.guild.core.utils.CompatibleScheduler;

/**
 * Listener de eventos do jogador
 */
public class PlayerListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public PlayerListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Evento de jogador entrando no servidor
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Verificar status de guerra da guilda
        checkWarStatus(event.getPlayer());
    }
    
    /**
     * Verificar status de guerra da guilda e enviar notificações
     */
    private void checkWarStatus(org.bukkit.entity.Player player) {
        // Verificar guilda do jogador de forma assíncrona
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild != null) {
                // Verificar todas as relações da guilda
                plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
                    // Garantir execução na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        for (com.guild.models.GuildRelation relation : relations) {
                            if (relation.isWar()) {
                                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.war-notification", "&4[Guerra de Guildas] &cSua guilda está em guerra com {guild}!");
                                message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                                player.sendMessage(com.guild.core.utils.ColorUtils.colorize(message));
                            }
                        }
                    });
                });
            }
        });
    }
    
    /**
     * Evento de jogador saindo do servidor
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpar estado da GUI do jogador
        GUIManager guiManager = plugin.getGuiManager();
        if (guiManager != null) {
            guiManager.closeGUI(event.getPlayer());
        }
    }
    
    /**
     * Processar evento de entrada de chat (usado para modo de entrada da GUI)
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        GUIManager guiManager = plugin.getGuiManager();
        
        if (guiManager != null && guiManager.isInInputMode(event.getPlayer())) {
            // Cancelar evento para impedir que a mensagem seja enviada no chat
            event.setCancelled(true);
            
            // Processar entrada - executar na thread principal
            String input = event.getMessage();
            CompatibleScheduler.runTask(plugin, () -> {
                try {
                    guiManager.handleInput(event.getPlayer(), input);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar entrada da GUI: " + e.getMessage());
                    e.printStackTrace();
                    // Limpar modo de entrada quando ocorrer erro
                    guiManager.clearInputMode(event.getPlayer());
                }
            });
        }
    }
}
