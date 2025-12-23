package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.models.Guild;
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
 * GUI de Criação de Relação de Guilda
 */
public class CreateRelationGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final Player player;
    private GuildRelation.RelationType selectedType = null;
    private String targetGuildName = null;
    private int currentPage = 0;
    private final int itemsPerPage = 28;
    private List<Guild> availableGuilds = new ArrayList<>();
    
    public CreateRelationGUI(GuildPlugin plugin, Guild guild, Player player) {
        this.plugin = plugin;
        this.guild = guild;
        this.player = player;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize("&6Criar Relação de Guilda");
    }
    
    @Override
    public int getSize() {
        return 54;
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher borda
        fillBorder(inventory);
        
        // Carregar lista de guildas disponíveis
        loadAvailableGuilds().thenAccept(guilds -> {
            this.availableGuilds = guilds;
            
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                // Exibir seleção de tipo de relação
                displayRelationTypes(inventory);
                
                // Exibir seleção de guilda alvo
                displayTargetGuilds(inventory);
                
                // Adicionar botões de função
                addFunctionButtons(inventory);
                
                // Adicionar botões de paginação
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
            GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
            plugin.getGuiManager().openGUI(player, relationsGUI);
            return;
        }
        
        // Botão de confirmar criação
        if (itemName.contains("Confirmar Criação")) {
            if (selectedType != null && targetGuildName != null) {
                createRelation(player);
            } else {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.select-both", "&cPor favor, selecione o tipo de relação e a guilda alvo primeiro!");
                player.sendMessage(ColorUtils.colorize(message));
            }
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
            int maxPage = (availableGuilds.size() - 1) / itemsPerPage;
            if (currentPage < maxPage) {
                currentPage++;
                refreshInventory(player);
            }
            return;
        }
        
        // Seleção de tipo de relação (slot 0-8)
        if (slot >= 0 && slot < 9) {
            handleRelationTypeClick(player, slot);
            return;
        }
        
        // Seleção de guilda alvo (slot 9-44)
        if (slot >= 9 && slot < 45) {
            int guildIndex = (currentPage * itemsPerPage) + (slot - 9);
            if (guildIndex < availableGuilds.size()) {
                Guild targetGuild = availableGuilds.get(guildIndex);
                targetGuildName = targetGuild.getName();
                refreshInventory(player);
                
                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.target-selected", "&aGuilda alvo selecionada: {guild}");
                message = message.replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
            }
        }
    }
    
    /**
     * Carregar lista de guildas disponíveis
     */
    private CompletableFuture<List<Guild>> loadAvailableGuilds() {
        return plugin.getGuildService().getAllGuildsAsync().thenApply(guilds -> {
            List<Guild> available = new ArrayList<>();
            for (Guild g : guilds) {
                if (!g.getName().equals(guild.getName())) {
                    available.add(g);
                }
            }
            return available;
        });
    }
    
    /**
     * Exibir seleção de tipo de relação
     */
    private void displayRelationTypes(Inventory inventory) {
        GuildRelation.RelationType[] types = GuildRelation.RelationType.values();
        
        for (int i = 0; i < types.length && i < 9; i++) {
            GuildRelation.RelationType type = types[i];
            Material material = getRelationTypeMaterial(type);
            String color = type.getColor();
            String displayName = ColorUtils.colorize(color + type.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Tipo de Relação: " + color + type.getDisplayName()));
            
            // Adicionar descrição do tipo de relação
            switch (type) {
                case ALLY:
                    lore.add(ColorUtils.colorize("&7Guilda aliada, ajuda mútua"));
                    break;
                case ENEMY:
                    lore.add(ColorUtils.colorize("&7Guilda inimiga, ataque mútuo"));
                    break;
                case WAR:
                    lore.add(ColorUtils.colorize("&7Estado de guerra, guerra total"));
                    break;
                case TRUCE:
                    lore.add(ColorUtils.colorize("&7Acordo de trégua, paz temporária"));
                    break;
                case NEUTRAL:
                    lore.add(ColorUtils.colorize("&7Relação neutra, não interferência"));
                    break;
            }
            
            if (selectedType == type) {
                lore.add(ColorUtils.colorize("&a✓ Selecionado"));
            } else {
                lore.add(ColorUtils.colorize("&eClique para Selecionar"));
            }
            
            ItemStack item = createItem(material, displayName, lore.toArray(new String[0]));
            inventory.setItem(i, item);
        }
    }
    
    /**
     * Exibir seleção de guilda alvo
     */
    private void displayTargetGuilds(Inventory inventory) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableGuilds.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Guild targetGuild = availableGuilds.get(i);
            int slot = 9 + (i - startIndex);
            
            Material material = Material.SHIELD;
            String displayName = ColorUtils.colorize("&f" + targetGuild.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Nome da Guilda: " + targetGuild.getName()));
            if (targetGuild.getTag() != null && !targetGuild.getTag().isEmpty()) {
                lore.add(ColorUtils.colorize("&7Tag da Guilda: [" + targetGuild.getTag() + "]"));
            }
            lore.add(ColorUtils.colorize("&7Líder: " + targetGuild.getLeaderName()));
            
            if (targetGuildName != null && targetGuildName.equals(targetGuild.getName())) {
                lore.add(ColorUtils.colorize("&a✓ Selecionado"));
            } else {
                lore.add(ColorUtils.colorize("&eClique para Selecionar"));
            }
            
            ItemStack item = createItem(material, displayName, lore.toArray(new String[0]));
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Adicionar botões de função
     */
    private void addFunctionButtons(Inventory inventory) {
        // Botão de confirmar criação
        ItemStack confirmButton = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&aConfirmar Criação"),
            ColorUtils.colorize("&7Criar Relação de Guilda"),
            ColorUtils.colorize("&7Necessário selecionar tipo de relação e guilda alvo primeiro")
        );
        inventory.setItem(45, confirmButton);
        
        // Exibir seleção atual
        List<String> selectionLore = new ArrayList<>();
        selectionLore.add(ColorUtils.colorize("&7Tipo de Relação: " + (selectedType != null ? selectedType.getColor() + selectedType.getDisplayName() : "&cNenhuma")));
        selectionLore.add(ColorUtils.colorize("&7Guilda Alvo: " + (targetGuildName != null ? "&a" + targetGuildName : "&cNenhuma")));
        
        ItemStack selectionInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&eSeleção Atual"),
            selectionLore.toArray(new String[0])
        );
        inventory.setItem(47, selectionInfo);
    }
    
    /**
     * Adicionar botões de paginação
     */
    private void addPaginationButtons(Inventory inventory) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&cPágina Anterior"),
                ColorUtils.colorize("&7Ver página anterior")
            );
            inventory.setItem(18, previousPage);
        }
        
        // Botão de próxima página
        int maxPage = (availableGuilds.size() - 1) / itemsPerPage;
        if (currentPage < maxPage) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize("&aPróxima Página"),
                ColorUtils.colorize("&7Ver próxima página")
            );
            inventory.setItem(26, nextPage);
        }
        
        // Botão de voltar
        ItemStack backButton = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7Voltar"),
            ColorUtils.colorize("&7Voltar ao Gerenciamento de Relações")
        );
        inventory.setItem(49, backButton);
        
        // Exibir número da página
        ItemStack pageInfo = createItem(
            Material.PAPER,
            ColorUtils.colorize("&ePágina " + (currentPage + 1) + ""),
            ColorUtils.colorize("&7Total " + (maxPage + 1) + ""),
            ColorUtils.colorize("&7Total de " + availableGuilds.size() + " guildas")
        );
        inventory.setItem(22, pageInfo);
    }
    
    /**
     * Tratar clique no tipo de relação
     */
    private void handleRelationTypeClick(Player player, int slot) {
        GuildRelation.RelationType[] types = GuildRelation.RelationType.values();
        if (slot < types.length) {
            selectedType = types[slot];
            refreshInventory(player);
            
            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.type-selected", "&aTipo de relação selecionado: {type}");
            message = message.replace("{type}", selectedType.getDisplayName());
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    
    /**
     * Criar relação
     */
    private void createRelation(Player player) {
        // Encontrar guilda alvo
        final Guild[] targetGuild = {null};
        for (Guild g : availableGuilds) {
            if (g.getName().equals(targetGuildName)) {
                targetGuild[0] = g;
                break;
            }
        }
        
        if (targetGuild[0] == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relations.target-not-found", "&cGuilda alvo não existe!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Verificar se a relação já existe
        plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild[0].getId())
            .thenAccept(existingRelation -> {
                CompatibleScheduler.runTask(plugin, () -> {
                    if (existingRelation != null) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relations.already-exists", "&cA relação com {guild} já existe!");
                        message = message.replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                        return;
                    }
                    
                    // Criar nova relação
                    plugin.getGuildService().createGuildRelationAsync(
                        guild.getId(), targetGuild[0].getId(),
                        guild.getName(), targetGuild[0].getName(),
                        selectedType, player.getUniqueId(), player.getName()
                    ).thenAccept(success -> {
                        CompatibleScheduler.runTask(plugin, () -> {
                            if (success) {
                                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.create-success", "&aSolicitação de relação {type} enviada para {guild}!");
                                message = message.replace("{guild}", targetGuildName)
                                               .replace("{type}", selectedType.getDisplayName());
                                player.sendMessage(ColorUtils.colorize(message));
                                
                                // Voltar para a interface de gerenciamento de relações
                                GuildRelationsGUI relationsGUI = new GuildRelationsGUI(plugin, guild, player);
                                plugin.getGuiManager().openGUI(player, relationsGUI);
                            } else {
                                String message = plugin.getConfigManager().getMessagesConfig().getString("relations.create-failed", "&cFalha ao criar relação!");
                                player.sendMessage(ColorUtils.colorize(message));
                            }
                        });
                    });
                });
            });
    }
    
    /**
     * Obter material correspondente ao tipo de relação
     */
    private Material getRelationTypeMaterial(GuildRelation.RelationType type) {
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
     * Atualizar inventário
     */
    private void refreshInventory(Player player) {
        CreateRelationGUI newGUI = new CreateRelationGUI(plugin, guild, player);
        newGUI.selectedType = this.selectedType;
        newGUI.targetGuildName = this.targetGuildName;
        newGUI.currentPage = this.currentPage;
        plugin.getGuiManager().openGUI(player, newGUI);
    }
    
    /**
     * Preencher borda
     */
    private void fillBorder(Inventory inventory) {
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Criar item
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
