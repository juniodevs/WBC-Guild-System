package com.guild.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildApplication;

public class ApplicationManagementGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private int currentPage = 0;
    private static final int APPLICATIONS_PER_PAGE = 28; // 4 linhas 7 colunas, excluindo bordas
    private boolean showingHistory = false; // false=Solicitações pendentes, true=Histórico de solicitações
    
    public ApplicationManagementGUI(GuildPlugin plugin, Guild guild) {
        this.plugin = plugin;
        this.guild = guild;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.title", "&6Gerenciar Solicitações"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("application-management.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher bordas
        fillBorder(inventory);
        
        // Adicionar botões de controle (barra inferior)
        setupControlButtons(inventory);
        
        // Carregar lista de solicitações
        loadApplications(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Verifica se é um botão de função
        if (isFunctionButton(slot)) {
            handleFunctionButton(player, slot);
            return;
        }
        
        // Verifica se é um botão de paginação
        if (isPaginationButton(slot)) {
            handlePaginationButton(player, slot);
            return;
        }
        
        // Verifica se é um botão de solicitação
        if (isApplicationSlot(slot)) {
            handleApplicationClick(player, slot, clickedItem, clickType);
        }
    }
    
    /**
     * Preencher bordas
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
     * Configurar botões de controle (barra inferior)
     */
    private void setupControlButtons(Inventory inventory) {
        // Botão de alternar modo (Pendentes/Histórico)
        Material toggleMaterial = showingHistory ? Material.BOOK : Material.PAPER;
        String toggleName = showingHistory ? 
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.show-pending.name", "&eVer Solicitações Pendentes")) :
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.show-history.name", "&eVer Histórico"));
        String toggleLore = showingHistory ?
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.show-pending.lore.1", "&7Clique para ver solicitações pendentes")) :
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.show-history.lore.1", "&7Clique para ver histórico de solicitações"));
        
        ItemStack toggleButton = createItem(toggleMaterial, toggleName, toggleLore);
        inventory.setItem(48, toggleButton);
        
        // Botão de voltar
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.name", "&7Voltar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.back.lore.1", "&7Voltar ao menu principal"))
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Carregar lista de solicitações
     */
    private void loadApplications(Inventory inventory) {
        if (showingHistory) {
            loadApplicationHistory(inventory);
        } else {
            loadPendingApplications(inventory);
        }
    }
    
    /**
     * Carregar solicitações pendentes
     */
    private void loadPendingApplications(Inventory inventory) {
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // Mostrar informação de sem solicitações
                ItemStack noApplications = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&aNenhuma solicitação pendente"),
                    ColorUtils.colorize("&7Não há solicitações pendentes no momento")
                );
                inventory.setItem(22, noApplications);
                return;
            }
            
            // Calcular paginação
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // Configurar botões de paginação
            setupPaginationButtons(inventory, totalPages);
            
            // Mostrar solicitações da página atual
            displayApplications(inventory, applications);
        });
    }
    
    /**
     * Carregar histórico de solicitações
     */
    private void loadApplicationHistory(Inventory inventory) {
        plugin.getGuildService().getApplicationHistoryAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                // Mostrar informação de sem histórico
                ItemStack noHistory = createItem(
                    Material.BARRIER,
                    ColorUtils.colorize("&aSem histórico de solicitações"),
                    ColorUtils.colorize("&7Não há histórico de solicitações no momento")
                );
                inventory.setItem(22, noHistory);
                return;
            }
            
            // Calcular paginação
            int totalPages = (applications.size() - 1) / APPLICATIONS_PER_PAGE;
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            
            // Configurar botões de paginação
            setupPaginationButtons(inventory, totalPages);
            
            // Mostrar solicitações da página atual
            displayApplications(inventory, applications);
        });
    }
    
    /**
     * Exibir lista de solicitações
     */
    private void displayApplications(Inventory inventory, List<GuildApplication> applications) {
        int startIndex = currentPage * APPLICATIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + APPLICATIONS_PER_PAGE, applications.size());
        
        int slotIndex = 10; // Começa na 2ª linha, 2ª coluna
        for (int i = startIndex; i < endIndex; i++) {
            GuildApplication application = applications.get(i);
            if (slotIndex >= 44) break; // Evita ultrapassar a área de exibição
            
            ItemStack applicationItem = createApplicationItem(application);
            inventory.setItem(slotIndex, applicationItem);
            
            slotIndex++;
            if (slotIndex % 9 == 8) { // Pular borda
                slotIndex += 2;
            }
        }
    }
    
    /**
     * Configurar botões de paginação
     */
    private void setupPaginationButtons(Inventory inventory, int totalPages) {
        // Botão de página anterior
        if (currentPage > 0) {
            ItemStack previousPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.name", "&cPágina Anterior")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.previous-page.lore.1", "&7Ver página anterior"))
            );
            inventory.setItem(18, previousPage);
        }
        
        // Botão de próxima página
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(
                Material.ARROW,
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.name", "&aPróxima Página")),
                ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("application-management.items.next-page.lore.1", "&7Ver próxima página"))
            );
            inventory.setItem(26, nextPage);
        }
    }
    
    /**
     * Criar item de solicitação
     */
    private ItemStack createApplicationItem(GuildApplication application) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        switch (application.getStatus()) {
            case PENDING:
                material = Material.YELLOW_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&eSolicitação de {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &ePendente"));
                lore.add(PlaceholderUtils.replaceApplicationPlaceholders("&7Data: {apply_time}", application.getPlayerName(), guild.getName(), application.getCreatedAt()));
                lore.add(ColorUtils.colorize("&7Mensagem: " + application.getMessage()));
                lore.add("");
                lore.add(ColorUtils.colorize("&aBotão Esquerdo: Aceitar"));
                lore.add(ColorUtils.colorize("&cBotão Direito (ou Q): Rejeitar"));
                break;
            case APPROVED:
                material = Material.GREEN_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&aSolicitação de {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &aAprovado"));
                break;
            case REJECTED:
                material = Material.RED_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&cSolicitação de {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &cRejeitado"));
                break;
            default:
                material = Material.GRAY_WOOL;
                name = PlaceholderUtils.replaceApplicationPlaceholders("&7Solicitação de {applicant_name}", application.getPlayerName(), guild.getName(), application.getCreatedAt());
                lore.add(ColorUtils.colorize("&7Status: &7Desconhecido"));
                break;
        }
        
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * Verifica se é um botão de função
     */
    private boolean isFunctionButton(int slot) {
        return slot == 48 || slot == 49;
    }
    
    /**
     * Verifica se é um botão de paginação
     */
    private boolean isPaginationButton(int slot) {
        return slot == 18 || slot == 26;
    }
    
    /**
     * Verifica se é um slot de solicitação
     */
    private boolean isApplicationSlot(int slot) {
        return slot >= 10 && slot <= 44 && slot % 9 != 0 && slot % 9 != 8;
    }
    
    /**
     * Tratar clique no botão de função
     */
    private void handleFunctionButton(Player player, int slot) {
        switch (slot) {
            case 48: // Alternar entre Pendentes e Histórico
                showingHistory = !showingHistory;
                currentPage = 0;
                refreshInventory(player);
                break;
            case 49: // Voltar
                plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                break;
        }
    }
    
    /**
     * Tratar clique no botão de paginação
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
     * Tratar clique na solicitação
     */
    private void handleApplicationClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (showingHistory) {
            // Histórico é apenas para visualização
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-history-view-only", "&7Este é um registro histórico, apenas visualização");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Solicitações pendentes podem ser aceitas ou rejeitadas
        if (clickType == ClickType.LEFT) {
            // Aceitar solicitação
            handleAcceptApplication(player, slot);
        } else if (clickType == ClickType.RIGHT) {
            // Rejeitar solicitação
            handleRejectApplication(player, slot);
        }
    }
    
    /**
     * Tratar aceitação de solicitação
     */
    private void handleAcceptApplication(Player player, int slot) {
        // Obter lista de solicitações da página atual
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&cNão há solicitações pendentes");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Calcular índice da solicitação na lista
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);
                
                // Processar solicitação
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.APPROVED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accepted", "&aSolicitação aceita!");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // Atualizar GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-accept-failed", "&cFalha ao aceitar solicitação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
        });
    }
    
    /**
     * Tratar rejeição de solicitação
     */
    private void handleRejectApplication(Player player, int slot) {
        // Obter lista de solicitações da página atual
        plugin.getGuildService().getPendingApplicationsAsync(guild.getId()).thenAccept(applications -> {
            if (applications == null || applications.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-pending-applications", "&cNão há solicitações pendentes");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Calcular índice da solicitação na lista
            int applicationIndex = currentPage * APPLICATIONS_PER_PAGE + (slot - 10);
            if (applicationIndex >= 0 && applicationIndex < applications.size()) {
                GuildApplication application = applications.get(applicationIndex);
                
                // Processar solicitação
                plugin.getGuildService().processApplicationAsync(application.getId(), GuildApplication.ApplicationStatus.REJECTED, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-rejected", "&cSolicitação rejeitada!");
                        player.sendMessage(ColorUtils.colorize(message));
                        
                        // Atualizar GUI
                        refreshInventory(player);
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.application-reject-failed", "&cFalha ao rejeitar solicitação!");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
            }
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
