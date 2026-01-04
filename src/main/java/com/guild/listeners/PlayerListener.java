package com.guild.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import com.guild.core.utils.CompatibleScheduler;

/**
 * Listener de Eventos de Jogador
 */
public class PlayerListener implements Listener {
    
    private final GuildPlugin plugin;
    
    public PlayerListener(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Evento de entrada de jogador no servidor
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Verifica o status de guerra da guilda
        checkWarStatus(event.getPlayer());
        
        // Atualiza o nome de exibição do jogador com a tag da guilda
        updatePlayerDisplayName(event.getPlayer());
    }
    
    /**
     * Atualiza o nome de exibição do jogador com a tag da guilda
     */
    private void updatePlayerDisplayName(org.bukkit.entity.Player player) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (guild != null) {
                    // Define o nome de exibição com a tag verde
                    String displayName = "§a[" + guild.getTag() + "]§r " + player.getName();
                    player.setDisplayName(displayName);
                    player.setPlayerListName(displayName);
                } else {
                    // Remove a tag se o jogador não estiver em uma guilda
                    player.setDisplayName(player.getName());
                    player.setPlayerListName(player.getName());
                }
            });
        });
    }
    
    /**
     * Atualiza o nome de exibição de todos os jogadores online
     */
    public void updateAllPlayerDisplayNames() {
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplayName(player);
        }
    }
    
    /**
     * Verifica o status de guerra da guilda e envia notificação
     */
    private void checkWarStatus(org.bukkit.entity.Player player) {
        // Verifica a guilda do jogador assincronamente
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild != null) {
                // Verifica todas as relações da guilda
                plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
                    // Garante execução na thread principal
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
     * Evento de saída de jogador do servidor
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpa o estado da GUI do jogador
        GUIManager guiManager = plugin.getGuiManager();
        if (guiManager != null) {
            guiManager.closeGUI(event.getPlayer());
        }
    }
    
    /**
     * Processa evento de chat (usado para modo de entrada da GUI)
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        GUIManager guiManager = plugin.getGuiManager();
        
        if (guiManager != null && guiManager.isInInputMode(event.getPlayer())) {
            // Cancela o evento para evitar envio da mensagem no chat
            event.setCancelled(true);
            
            // Processa a entrada - executa na thread principal
            String input = event.getMessage();
            CompatibleScheduler.runTask(plugin, () -> {
                try {
                    guiManager.handleInput(event.getPlayer(), input);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar entrada da GUI: " + e.getMessage());
                    e.printStackTrace();
                    // Limpa o modo de entrada em caso de erro
                    guiManager.clearInputMode(event.getPlayer());
                }
            });
        } else {
            // Adiciona tag da guilda no nome do jogador no chat
            plugin.getGuildService().getPlayerGuildAsync(event.getPlayer().getUniqueId()).thenAccept(guild -> {
                if (guild != null) {
                    // Formato do chat: §a[TAG]§r nome: mensagem
                    String guildTag = "§a[" + guild.getTag() + "]§r ";
                    
                    // Substitui o placeholder %1$s (nome do jogador) para incluir a tag
                    String newFormat = event.getFormat().replace("%1$s", guildTag + "%1$s");
                    
                    // Executa na thread principal para modificar o formato
                    CompatibleScheduler.runTask(plugin, () -> {
                        event.setFormat(newFormat);
                    });
                }
            });
        }
    }
}
