package com.guild.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;

/**
 * GUI de Lista de Guildas
 */
public class GuildListGUI implements GUI {
    
    private final GuildPlugin plugin;
    private int currentPage = 0;
    private static final int GUILDS_PER_PAGE = 28; // 4 linhas 7 colunas, excluindo borda
    private String searchQuery = "";
    private String filterType = "all"; // all, name, tag
    
    public GuildListGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    public GuildListGUI(GuildPlugin plugin, String searchQuery, String filterType) {
        this.plugin = plugin;
        this.searchQuery = searchQuery;
        this.filterType = filterType;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.title", "&6Lista de Guildas"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("guild-list.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher borda
        fillBorder(inventory);
        
        // Adicionar botões de função
        setupFunctionButtons(inventory);
        
        // Carregar lista de guildas
        loadGuilds(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Verificar se é botão de função
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // Verificar se é botão de paginação
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // Verificar se é botão de guilda
        if (isGuildSlot(slot)) {
            handleGuildClick(player, slot, clickedItem, clickType);
        }
    }
    
    /**
     * Preencher borda
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
     * Configurar botões de função
     */
    private void setupFunctionButtons(Inventory inventory) {
        // Botão de pesquisa
        ItemStack search = createItem(
            Material.COMPASS,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.name", "&ePesquisar Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.search.lore.1", "&7Pesquisar por uma guilda específica")),
            ColorUtils.colorize("&7Pesquisa atual: " + (searchQuery.isEmpty() ? "Nenhuma" : searchQuery))
        );
        inventory.setItem(45, search);
        
        // Botão de filtro
        ItemStack filter = createItem(
            Material.HOPPER,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.name", "&eFiltrar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.filter.lore.1", "&7Filtrar guildas por condição")),
            ColorUtils.colorize("&7Filtro atual: " + getFilterDisplayName())
        );
        inventory.setItem(47, filter);
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.name", "&7Voltar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.back.lore.1", "&7Voltar ao Menu Principal"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Carregar lista de guildas
     */
    private void loadGuilds(Inventory inventory) {
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // Garantir atualização da GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    // Exibir informação de sem guildas
                    ItemStack noGuilds = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&cSem Guildas"),
                        ColorUtils.colorize("&7Não há guildas no servidor ainda")
                    );
                    inventory.setItem(22, noGuilds);
                    return;
                }
                
                // Aplicar pesquisa e filtro
                List<Guild> filteredGuilds = filterGuilds(guilds);
                
                if (filteredGuilds.isEmpty()) {
                    // Exibir sem resultados de pesquisa
                    ItemStack noResults = createItem(
                        Material.BARRIER,
                        ColorUtils.colorize("&cSem Resultados"),
                        ColorUtils.colorize("&7Nenhuma guilda correspondente encontrada")
                    );
                    inventory.setItem(22, noResults);
                    return;
                }
                
                // Calcular paginação
                int totalPages = (filteredGuilds.size() - 1) / GUILDS_PER_PAGE;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                
                // Configurar botões de paginação
                setupPaginationButtons(inventory, totalPages);
                
                // Exibir guildas da página atual
                displayGuilds(inventory, filteredGuilds);
            });
        });
    }
    
    /**
     * Filtrar guildas
     */
    private List<Guild> filterGuilds(List<Guild> guilds) {
        List<Guild> filtered = new ArrayList<>();
        
        for (Guild guild : guilds) {
            boolean matches = true;
            
            // Aplicar pesquisa
            if (!searchQuery.isEmpty()) {
                switch (filterType) {
                    case "name":
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase());
                        break;
                    case "tag":
                        if (guild.getTag() != null) {
                            matches = guild.getTag().toLowerCase().contains(searchQuery.toLowerCase());
                        } else {
                            matches = false;
                        }
                        break;
                    default: // all
                        matches = guild.getName().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                (guild.getTag() != null && guild.getTag().toLowerCase().contains(searchQuery.toLowerCase()));
                        break;
                }
            }
            
            if (matches) {
                filtered.add(guild);
            }
        }
        
        return filtered;
    }
    
    /**
     * Exibir lista de guildas
     */
    private void displayGuilds(Inventory inventory, List<Guild> guilds) {
        int startIndex = currentPage * GUILDS_PER_PAGE;
        int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, guilds.size());
        
        // Criar tarefas assíncronas para todas as guildas
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        int slotIndex = 10; // Começar da linha 2, coluna 2
        for (int i = startIndex; i < endIndex; i++) {
            Guild guild = guilds.get(i);
            if (slotIndex >= 44) break; // Evitar exceder área de exibição
            
            final int finalSlotIndex = slotIndex;
            
            // Obter quantidade de membros assincronamente e criar item
            CompletableFuture<Void> future = plugin.getGuildService().getGuildMemberCountAsync(guild.getId())
                .thenAccept(memberCount -> {
                    // Atualizar GUI na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        ItemStack guildItem = createGuildItemWithMemberCount(guild, memberCount);
                        inventory.setItem(finalSlotIndex, guildItem);
                    });
                });
            
            futures.add(future);
            
            slotIndex++;
            if (slotIndex % 9 == 8) { // Pular borda
                slotIndex += 2;
            }
        }
        
        // Aguardar conclusão de todas as tarefas assíncronas
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Configurar botões de paginação
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.name", "&cPágina Anterior")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.previous-page.lore.1", "&7Ver página anterior"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // Botão de próxima página
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.name", "&aPróxima Página")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("guild-list.items.next-page.lore.1", "&7Ver próxima página"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * Criar item de guilda (com quantidade de membros)
     */
    private ItemStack createGuildItemWithMemberCount(Guild guild, int memberCount) {
        List<String> lore = new ArrayList<>();
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Tag: {guild_tag}", guild, null));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Líder: {leader_name}", guild, null));
        lore.add(ColorUtils.colorize("&7Membros: " + memberCount));
        lore.add(PlaceholderUtils.replaceGuildPlaceholders("&7Criada em: {guild_created_time}", guild, null));
        lore.add("");
        lore.add(ColorUtils.colorize("&aBotão Esquerdo: Ver Detalhes"));
        lore.add(ColorUtils.colorize("&eBotão Direito: Solicitar Entrada"));
        
        // Usar banner da guilda ou banner branco padrão
        ItemStack bannerItem;
        if (guild.getBanner() != null) {
            bannerItem = guild.getBanner().clone();
        } else {
            bannerItem = com.guild.core.utils.BannerSerializer.getDefaultBanner();
        }
        
        ItemMeta meta = bannerItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PlaceholderUtils.replaceGuildPlaceholders("&e{guild_name}", guild, null));
            meta.setLore(lore);
            bannerItem.setItemMeta(meta);
        }
        
        return bannerItem;
    }
    
    /**
     * Criar item de guilda (método original, para compatibilidade)
     */
    private ItemStack createGuildItem(Guild guild) {
        return createGuildItemWithMemberCount(guild, 0); // Usar valor padrão
    }
    
    /**
     * Obter nome de exibição do filtro
     */
    private String getFilterDisplayName() {
        switch (filterType) {
            case "name":
                return "Por Nome";
            case "tag":
                return "Por Tag";
            default:
                return "Todos";
        }
    }
    
    /**
     * Verificar se é botão de função
     */
    private boolean isFunctionButton(int slot) {
        return slot == 45 || slot == 47 || slot == 49;
    }
    
    /**
     * Verificar se é botão de paginação
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * Verificar se é slot de guilda
     */
    private boolean isGuildSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * Tratar clique em botão de função
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 45: // Pesquisa
                handleSearch(player);
                break;
            case 47: // Filtro
                handleFilter(player);
                break;
            case 49: // Voltar
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * Tratar clique em botão de paginação
     */
    private void handlePaginationButton(Player player, int slot) {
        if (slot == 18) { // Página anterior
            if (currentPage > 0) {
                currentPage--;
                refreshInventory(player);
            }
        } else if (slot == 26) { // Próxima página
            currentPage++;
            refreshInventory(player);
        }
    }
    
    /**
     * Tratar clique em guilda
     */
    private void handleGuildClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            // Ver detalhes
            handleViewGuildDetails(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // Solicitar entrada
            handleApplyToGuild(player, slot);
        }
    }
    
    /**
     * Tratar pesquisa
     */
    private void handleSearch(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.search-dev", "&aFunção de pesquisa em desenvolvimento...");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * Tratar filtro
     */
    private void handleFilter(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.filter-dev", "&aFunção de filtro em desenvolvimento...");
        player.sendMessage(ColorUtils.colorize(message));
    }
    
    /**
     * Tratar visualização de detalhes da guilda
     */
    private void handleViewGuildDetails(Player player, int slot) {
        // Obter lista de guildas da página atual
        plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (guilds == null || guilds.isEmpty()) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&cNenhuma guilda encontrada");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Calcular índice da guilda na lista
                int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                if (guildIndex >= 0 && guildIndex < guilds.size()) {
                    Guild guild = guilds.get(guildIndex);
                    
                    // Abrir GUI de informações da guilda
                    GuildInfoGUI guildInfoGUI = new GuildInfoGUI(plugin, player, guild);
                    plugin.getGuiManager().openGUI(player, guildInfoGUI);
                }
            });
        });
    }
    
    /**
     * Tratar solicitação de entrada na guilda
     */
    private void handleApplyToGuild(Player player, int slot) {
        // Verificar se jogador já tem guilda
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(playerGuild -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (playerGuild != null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.already-in-guild", "&cVocê já está em uma guilda!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                
                // Obter lista de guildas da página atual
                plugin.getGuildService().getAllGuildsAsync().thenAccept(guilds -> {
                    // Garantir execução de operações GUI na thread principal
                    CompatibleScheduler.runTask(plugin, () -> {
                        if (guilds == null || guilds.isEmpty()) {
                            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-guilds", "&cNenhuma guilda encontrada");
                            player.sendMessage(ColorUtils.colorize(message));
                            return;
                        }
                        
                        // Calcular índice da guilda na lista
                        int guildIndex = currentPage * GUILDS_PER_PAGE + (slot - 10);
                        if (guildIndex >= 0 && guildIndex < guilds.size()) {
                            Guild guild = guilds.get(guildIndex);
                            
                            // Verificar se já existe solicitação pendente
                            plugin.getGuildService().hasPendingApplicationAsync(player.getUniqueId(), guild.getId()).thenAccept(hasPending -> {
                                // Garantir execução de operações GUI na thread principal
                                CompatibleScheduler.runTask(plugin, () -> {
                                    if (hasPending) {
                                        String message = plugin.getConfigManager().getMessagesConfig().getString("apply.already-applied", "&cVocê já solicitou entrada nesta guilda!");
                                        player.sendMessage(ColorUtils.colorize(message));
                                        return;
                                    }
                                    
                                    // Enviar solicitação
                                    plugin.getGuildService().submitApplicationAsync(guild.getId(), player.getUniqueId(), player.getName(), "").thenAccept(success -> {
                                        // Garantir execução de operações GUI na thread principal
                                        CompatibleScheduler.runTask(plugin, () -> {
                                            if (success) {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.success", "&aSolicitação enviada!");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            } else {
                                                String message = plugin.getConfigManager().getMessagesConfig().getString("apply.failed", "&cFalha ao enviar solicitação!");
                                                player.sendMessage(ColorUtils.colorize(message));
                                            }
                                        });
                                    });
                                });
                            });
                        }
                    });
                });
            });
        });
    }
    
    /**
     * Atualizar inventário
     */
    private void refreshInventory(Player player) {
        plugin.getGuiManager().refreshGUI(player);
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
