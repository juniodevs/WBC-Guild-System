package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI de Relações da Guilda - Gerenciar relações da guilda
 */
public class GuildRelationsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 28 relações por página (7 colunas x 4 linhas)
    private List<GuildRelation> relations = new ArrayList<>();
    
    public GuildRelationsGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-relations.title", "&6Relações da Guilda"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-relations.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Carrega dados de relações
        loadRelations().thenAccept(relationsList -> {
            this.relations = relationsList;
            
            // Garante execução de operações de GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                // Mostra lista de relações
                displayRelations(inventory);
                
                // Adiciona botões de função
                addFunctionButtons(inventory);
                
                // Adiciona botões de paginação
                addPaginationButtons(inventory);
            });
        });
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Botão de voltar
        if (itemName.contains("Voltar")) {
            MainGuildGUI mainGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGUI);
            return;
        }
        
        // Botão de criar relação
        if (itemName.contains("Criar Relação")) {
            openCreateRelationGUI(player);
            return;
        }
        
        // Botões de paginação
        if (itemName.contains("Página Anterior")) {
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
            return;
        }
        
        if (itemName.contains("Próxima Página")) {
            int maxPage = (relations.size() - 1) / itemsPerPage;
            if (currentPage < maxPage) {
                currentPage++;
                refreshInventory(player);
            }
            return;
        }
        
        // Clique em item de relação - Verifica se está no intervalo de colunas 2-8, linhas 2-5
        if (slot >= 10 && slot <= 43) {
            int row = slot / 9;
            int col = slot % 9;
            if (row >= 1 && row <= 4 && col >= 1 && col <= 7) {
                int relativeIndex = (row - 1) * 7 + (col - 1);
                int relationIndex = (currentPage * itemsPerPage) + relativeIndex;
                if (relationIndex < relations.size()) {
                    GuildRelation relation = relations.get(relationIndex);
                    handleRelationClick(player, relation, clickType);
                }
            }
        }
    }
    
    /**
     * Carrega dados de relações da guilda
     */
    private CompletableFuture<List<GuildRelation>> loadRelations() {
        return plugin.getGuildService().getGuildRelationsAsync(guild.getId());
    }
    
    /**
     * Mostra lista de relações
     */
    private void displayRelations(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, relations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            GuildRelation relation = relations.get(i);
            int relativeIndex = i - startIndex;
            
            // Calcula posição nas colunas 2-8, linhas 2-5 (slots 10-43)
            int row = (relativeIndex / 7) + 1; // Linhas 2-5
            int col = (relativeIndex % 7) + 1; // Colunas 2-8
            int slot = row * 9 + col;
            
            ItemStack relationItem = createRelationItem(relation);
            inventory.setItem(slot, relationItem);
        }
    }
    
    /**
     * Cria item de exibição de relação
     */
    private ItemStack createRelationItem(GuildRelation relation) {
        String otherGuildName = relation.getOtherGuildName(guild.getId());
        GuildRelation.RelationType type = relation.getType();
        GuildRelation.RelationStatus status = relation.getStatus();
        
        Material material = getRelationMaterial(type);
        String color = type.getColor();
        String displayName = color + otherGuildName + " - " + type.getDisplayName();
        
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7Tipo de Relação: " + color + type.getDisplayName()));
        lore.add(ColorUtils.colorize("&7Status: " + getStatusColor(status) + status.getDisplayName()));
        lore.add(ColorUtils.colorize("&7Iniciador: " + relation.getInitiatorName()));
        lore.add(ColorUtils.colorize("&7Criado em: " + formatDateTime(relation.getCreatedAt())));
        
        if (relation.getExpiresAt() != null) {
            lore.add(ColorUtils.colorize("&7Expira em: " + formatDateTime(relation.getExpiresAt())));
        }
        
        lore.add("");
        
        // Adiciona dicas de operação com base no tipo e status da relação
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                lore.add(ColorUtils.colorize("&cBotão Direito: Cancelar Relação"));
            } else {
                lore.add(ColorUtils.colorize("&aBotão Esquerdo: Aceitar Relação"));
                lore.add(ColorUtils.colorize("&cBotão Direito: Rejeitar Relação"));
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                lore.add(ColorUtils.colorize("&eBotão Esquerdo: Terminar Trégua"));
            } else if (type == GuildRelation.RelationType.WAR) {
                lore.add(ColorUtils.colorize("&eBotão Esquerdo: Propor Trégua"));
            } else {
                lore.add(ColorUtils.colorize("&cBotão Direito: Excluir Relação"));
            }
        }
        
        return createItem(material, displayName, lore.toArray(new String[0]));
    }
    
    /**
     * Obtém material correspondente ao tipo de relação
     */
    private Material getRelationMaterial(GuildRelation.RelationType type) {
        switch (type) {
            case ALLY: return Material.GREEN_WOOL;
            case ENEMY: return Material.RED_WOOL;
            case WAR: return Material.NETHERITE_SWORD;
            case TRUCE: return Material.YELLOW_WOOL;
            case NEUTRAL: return Material.GRAY_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
    
    /**
     * Obtém cor do status
     */
    private String getStatusColor(GuildRelation.RelationStatus status) {
        switch (status) {
            case PENDING: return "&e";
            case ACTIVE: return "&a";
            case EXPIRED: return "&7";
            case CANCELLED: return "&c";
            default: return "&f";
        }
    }
    
    /**
     * Formata data e hora
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "Desconhecido";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    /**
     * Adiciona botões de função
     */
    private void addFunctionButtons(Inventory inventory) {
        // Botão de criar relação
        ItemStack createRelation = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&aCriar Relação"),
            ColorUtils.colorize("&7Criar nova relação de guilda"),
            ColorUtils.colorize("&7Aliados, Inimigos, Guerra, etc.")
        );
        inventory.setItem(45, createRelation);
        
        // Botão de estatísticas de relações
        ItemStack statistics = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eEstatísticas de Relações"),
            ColorUtils.colorize("&7Ver estatísticas de relações"),
            ColorUtils.colorize("&7Número de aliados, inimigos, etc.")
        );
        inventory.setItem(47, statistics);
    }
    
    /**
     * Adiciona botões de paginação
     */
    private void addPaginationButtons(Inventory inventory) {
        int maxPage = (relations.size() - 1) / itemsPerPage;
        
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&cPágina Anterior"),
                ColorUtils.colorize("&7Ver página anterior")
            );
            inventory.setItem(45, previousPage);
        }
        
        // Botão de próxima página
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&aPróxima Página"),
                ColorUtils.colorize("&7Ver próxima página")
            );
            inventory.setItem(53, nextPage);
        }
        
        // Botão de voltar
        ItemStack backButton = createItem(
            Material.BARRIER,
            ColorUtils.colorize("&cVoltar"),
            ColorUtils.colorize("&7Voltar ao menu principal")
        );
        inventory.setItem(49, backButton);
        
        // Exibição de número da página
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&ePág " + (currentPage + 1)),
            ColorUtils.colorize("&7Total " + (maxPage + 1) + " págs"),
            ColorUtils.colorize("&7Total " + relations.size() + " relações")
        );
        inventory.setItem(47, pageInfo);
    }
    
    /**
     * Processa clique em relação
     */
    private void handleRelationClick(Player player, GuildRelation relation, ClickType clickType) {
        GuildRelation.RelationStatus status = relation.getStatus();
        GuildRelation.RelationType type = relation.getType();
        
        if (status == GuildRelation.RelationStatus.PENDING) {
            if (relation.getInitiatorUuid().equals(player.getUniqueId())) {
                // Iniciador cancela relação
                if (clickType == ClickType.RIGHT) {
                    cancelRelation(player, relation);
                }
            } else {
                // Outra parte processa relação
                if (clickType == ClickType.LEFT) {
                    acceptRelation(player, relation);
                } else if (clickType == ClickType.RIGHT) {
                    rejectRelation(player, relation);
                }
            }
        } else if (status == GuildRelation.RelationStatus.ACTIVE) {
            if (type == GuildRelation.RelationType.TRUCE) {
                if (clickType == ClickType.LEFT) {
                    endTruce(player, relation);
                }
            } else if (type == GuildRelation.RelationType.WAR) {
                if (clickType == ClickType.LEFT) {
                    proposeTruce(player, relation);
                }
            } else {
                if (clickType == ClickType.RIGHT) {
                    deleteRelation(player, relation);
                }
            }
        }
    }
    
    /**
     * Aceita relação
     */
    private void acceptRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE)
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-success", "&aRelação com {guild} aceita!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.accept-failed", "&cFalha ao aceitar relação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * Rejeita relação
     */
    private void rejectRelation(Player player, GuildRelation relation) {
        if (relation.getType() == GuildRelation.RelationType.TRUCE) {
            // Se for trégua, volta para GUERRA e ATIVO
            plugin.getGuildService().updateGuildRelationAsync(
                relation.getId(),
                GuildRelation.RelationType.WAR,
                GuildRelation.RelationStatus.ACTIVE,
                relation.getInitiatorUuid(),
                relation.getInitiatorName()
            ).thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-success", "&cRelação com {guild} rejeitada!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-failed", "&cFalha ao rejeitar relação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
        } else {
            plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
                .thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-success", "&cRelação com {guild} rejeitada!");
                            message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                            player.sendMessage(ColorUtils.colorize(message));
                            refreshInventory(player);
                        } else {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.reject-failed", "&cFalha ao rejeitar relação!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
        }
    }
    
    /**
     * Cancela relação
     */
    private void cancelRelation(Player player, GuildRelation relation) {
        if (relation.getType() == GuildRelation.RelationType.TRUCE) {
            plugin.getGuildService().updateGuildRelationAsync(
                relation.getId(),
                GuildRelation.RelationType.WAR,
                GuildRelation.RelationStatus.ACTIVE,
                relation.getInitiatorUuid(),
                relation.getInitiatorName()
            ).thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-success", "&cRelação com {guild} cancelada!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-failed", "&cFalha ao cancelar relação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
        } else {
            plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED)
                .thenAccept(success -> {
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (success) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-success", "&cRelação com {guild} cancelada!");
                            message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                            player.sendMessage(ColorUtils.colorize(message));
                            refreshInventory(player);
                        } else {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.cancel-failed", "&cFalha ao cancelar relação!");
                            player.sendMessage(ColorUtils.colorize(message));
                        }
                    });
                });
        }
    }
    
    /**
     * Termina trégua
     */
    private void endTruce(Player player, GuildRelation relation) {
        // Termina trégua, muda para relação neutra
        plugin.getGuildService().updateGuildRelationAsync(
            relation.getId(),
            GuildRelation.RelationType.NEUTRAL,
            GuildRelation.RelationStatus.ACTIVE,
            player.getUniqueId(),
            player.getName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end", "&aTrégua com {guild} terminou, relação agora é neutra!");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-end-failed", "&cFalha ao terminar trégua!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * Propõe trégua
     */
    private void proposeTruce(Player player, GuildRelation relation) {
        // Cria proposta de trégua
        plugin.getGuildService().updateGuildRelationAsync(
            relation.getId(),
            GuildRelation.RelationType.TRUCE,
            GuildRelation.RelationStatus.PENDING,
            player.getUniqueId(),
            player.getName()
        ).thenAccept(success -> {
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-proposed", "&eTrégua proposta para {guild}!");
                    message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                    player.sendMessage(ColorUtils.colorize(message));
                    refreshInventory(player);
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relations.truce-propose-failed", "&cFalha ao propor trégua!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * Exclui relação
     */
    private void deleteRelation(Player player, GuildRelation relation) {
        plugin.getGuildService().deleteGuildRelationAsync(relation.getId())
            .thenAccept(success -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-success", "&aRelação com {guild} excluída!");
                        message = message.replace("{guild}", relation.getOtherGuildName(guild.getId()));
                        player.sendMessage(ColorUtils.colorize(message));
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.delete-failed", "&cFalha ao excluir relação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            });
    }
    
    /**
     * Abre GUI de criação de relação
     */
    private void openCreateRelationGUI(Player player) {
        CreateRelationGUI createRelationGUI = new CreateRelationGUI(plugin, guild, player);
        plugin.getGuiManager().openGUI(player, createRelationGUI);
    }
    
    /**
     * Atualiza inventário
     */
    private void refreshInventory(Player player) {
        if (player.isOnline()) {
            plugin.getGuiManager().refreshGUI(player);
        }
    }
    
    /**
     * Preenche a borda
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Cria item
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
