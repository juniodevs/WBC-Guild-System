package com.guild.gui;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.models.Guild;

/**
 * GUI de Convidar Membro
 */
public class InviteMemberGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private List<Player> onlinePlayers;
    
    public InviteMemberGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
        this.onlinePlayers = Bukkit.getOnlinePlayers().stream()
            .filter(player -> !player.getUniqueId().equals(guild.getLeaderUuid()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Convidar Membro - Pág " + (currentPage + 1));
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Mostra jogadores online
        displayOnlinePlayers(inventory);
        
        // Adiciona botões de navegação
        setupNavigationButtons(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        int[] validSlots = {10, 11, 12, 13, 14, 15, 16,
                            19, 20, 21, 22, 23, 24, 25,
                            28, 29, 30, 31, 32, 33, 34,
                            37, 38, 39, 40, 41, 42, 43};
        
        // Verifica se é um slot válido de jogador
        int slotIndex = -1;
        for (int i = 0; i < validSlots.length; i++) {
            if (validSlots[i] == slot) {
                slotIndex = i;
                break;
            }
        }
        
        if (slotIndex != -1) {
            // Área de cabeças dos jogadores
            int playerIndex = slotIndex + (currentPage * 28);
            if (playerIndex < onlinePlayers.size()) {
                Player targetPlayer = onlinePlayers.get(playerIndex);
                handleInvitePlayer(player, targetPlayer);
            }
        } else if (slot == 45) {
            // Página anterior
            if (currentPage > 0) {
                currentPage--;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 53) {
            // Próxima página
            int maxPage = (onlinePlayers.size() - 1) / 28;
            if (currentPage < maxPage) {
                currentPage++;
                plugin.getGuiManager().refreshGUI(player);
            }
        } else if (slot == 49) {
            // Voltar
            plugin.getGuiManager().openGUI(player, new GuildSettingsGUI(plugin, guild));
        }
    }
    
    /**
     * Preenche a borda
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        // Bordas superior e inferior
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        // Bordas laterais
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Mostra jogadores online
     */
    private void displayOnlinePlayers(Inventory inventory) {
        int startIndex = currentPage * 28;
        int endIndex = Math.min(startIndex + 28, onlinePlayers.size());
        
        int[] validSlots = {10, 11, 12, 13, 14, 15, 16,
                            19, 20, 21, 22, 23, 24, 25,
                            28, 29, 30, 31, 32, 33, 34,
                            37, 38, 39, 40, 41, 42, 43};
        
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = onlinePlayers.get(i);
            int slotIndex = i - startIndex;
            int slot = validSlots[slotIndex];
            
            ItemStack playerHead = createPlayerHead(targetPlayer);
            inventory.setItem(slot, playerHead);
        }
    }
    
    /**
     * Configura botões de navegação
     */
    private void setupNavigationButtons(Inventory inventory) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack prevPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePágina Anterior"),
                ColorUtils.colorize("&7Clique para ver a página anterior")
            );
            inventory.setItem(45, prevPage);
        }
        
        // Botão de próxima página
        int maxPage = (onlinePlayers.size() - 1) / 28;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&ePróxima Página"),
                ColorUtils.colorize("&7Clique para ver a próxima página")
            );
            inventory.setItem(53, nextPage);
        }
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&cVoltar"),
            ColorUtils.colorize("&7Voltar para Configurações da Guilda")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Cria cabeça do jogador
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtils.colorize("&a" + player.getName()));
            meta.setLore(Arrays.asList(
                ColorUtils.colorize("&7Clique para convidar este jogador"),
                ColorUtils.colorize("&7para entrar na guilda")
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Processa convite de jogador
     */
    private void handleInvitePlayer(Player inviter, Player target) {
        // Verifica se o jogador alvo já está em uma guilda
        plugin.getGuildService().getGuildMemberAsync(target.getUniqueId()).thenAccept(member -> {
            if (member != null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&cEste jogador já está em uma guilda!");
                inviter.sendMessage(ColorUtils.colorize(message.replace("{player}", target.getName())));
                return;
            }
            
            // Envia convite
            plugin.getGuildService().sendInvitationAsync(guild.getId(), inviter.getUniqueId(), inviter.getName(), target.getUniqueId(), target.getName()).thenAccept(success -> {
                if (success) {
                    String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&aConvite enviado para &e{player}&a!")
                        .replace("{player}", target.getName());
                    inviter.sendMessage(ColorUtils.colorize(inviterMessage));
                    
                    // Mensagens para o jogador convidado
                    String inviteTitle = plugin.getConfigManager().getMessagesConfig().getString("invite.title", "&6=== Convite de Guilda ===");
                    target.sendMessage(ColorUtils.colorize(inviteTitle));
                    
                    String inviteMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&e{inviter} convidou você para entrar na guilda: {guild}")
                        .replace("{inviter}", inviter.getName())
                        .replace("{guild}", guild.getName());
                    target.sendMessage(ColorUtils.colorize(inviteMessage));
                    
                    String acceptMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accept-command", "&eDigite &a/guild accept {inviter} &epara aceitar");
                    target.sendMessage(ColorUtils.colorize(acceptMessage.replace("{inviter}", inviter.getName())));
                    
                    String declineMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.decline-command", "&eDigite &c/guild decline {inviter} &epara recusar");
                    target.sendMessage(ColorUtils.colorize(declineMessage.replace("{inviter}", inviter.getName())));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("invite.failed", "&cFalha ao enviar convite!");
                    inviter.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * Cria item
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
