package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

import com.guild.core.utils.CompatibleScheduler;

/**
 * GUI de Criação de Guilda
 */
public class CreateGuildGUI implements GUI {
    
    private final GuildPlugin plugin;
    private String guildName = "";
    private String guildTag = "";
    private String guildDescription = "";
    
    public CreateGuildGUI(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    
    public CreateGuildGUI(GuildPlugin plugin, String guildName, String guildTag, String guildDescription) {
        this.plugin = plugin;
        this.guildName = guildName;
        this.guildTag = guildTag;
        this.guildDescription = guildDescription;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.title", "&6Criar Guilda"));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("create-guild.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preencher borda
        fillBorder(inventory);
        
        // Adicionar botões de entrada
        setupInputButtons(inventory);
        
        // Adicionar botões de confirmação/cancelamento
        setupActionButtons(inventory);
        
        // Exibir informações de entrada atuais
        displayCurrentInput(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        switch (slot) {
            case 20: // Entrada de nome da guilda
                handleNameInput(player);
                break;
            case 22: // Entrada de tag da guilda
                handleTagInput(player);
                break;
            case 24: // Entrada de descrição da guilda
                handleDescriptionInput(player);
                break;
            case 39: // Confirmar criação
                handleConfirmCreate(player);
                break;
            case 41: // Cancelar
                handleCancel(player);
                break;
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
     * Configurar botões de entrada
     */
    private void setupInputButtons(Inventory inventory) {
        // Botão de entrada de nome da guilda
        ItemStack nameInput = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.name", "&eNome da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.1", "&7Clique para inserir o nome da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.name-input.lore.2", "&7Comprimento: 3-20 caracteres"))
        );
        inventory.setItem(20, nameInput);
        
        // Botão de entrada de tag da guilda
        ItemStack tagInput = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.name", "&eTag da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.1", "&7Clique para inserir a tag da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.2", "&7Comprimento: Máx 6 caracteres")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.tag-input.lore.3", "&7Opcional"))
        );
        inventory.setItem(22, tagInput);
        
        // Botão de entrada de descrição da guilda
        ItemStack descriptionInput = createItem(
            Material.BOOK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.name", "&eDescrição da Guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.1", "&7Clique para inserir a descrição da guilda")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.2", "&7Comprimento: Máx 100 caracteres")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.description-input.lore.3", "&7Opcional"))
        );
        inventory.setItem(24, descriptionInput);
    }
    
    /**
     * Configurar botões de ação
     */
    private void setupActionButtons(Inventory inventory) {
        // Botão de confirmar criação
        String confirmName = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.name", "&aConfirmar Criação");
        String confirmLore1 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.1", "&7Confirmar criação da guilda");
        String confirmLore3 = plugin.getConfigManager().getGuiConfig().getString("create-guild.items.confirm.lore.3", "&7Criador: {player_name}");
        
        // Substituir variáveis
        confirmLore3 = confirmLore3.replace("{player_name}", "Jogador Atual");
        
        ItemStack confirm = createItem(
            Material.EMERALD_BLOCK,
            ColorUtils.colorize(confirmName),
            ColorUtils.colorize(confirmLore1),
            ColorUtils.colorize(confirmLore3)
        );
        inventory.setItem(39, confirm);
        
        // Botão de cancelar
        ItemStack cancel = createItem(
            Material.REDSTONE_BLOCK,
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.name", "&cCancelar")),
            ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("create-guild.items.cancel.lore.1", "&7Cancelar criação da guilda"))
        );
        inventory.setItem(41, cancel);
    }
    
    /**
     * Exibir informações de entrada atuais
     */
    private void displayCurrentInput(Inventory inventory) {
        // Nome atual da guilda
        String nameDisplay = guildName.isEmpty() ? "Não definido" : guildName;
        ItemStack currentName = createItem(
            Material.NAME_TAG,
            ColorUtils.colorize("&eNome Atual da Guilda"),
            ColorUtils.colorize("&7" + nameDisplay)
        );
        inventory.setItem(11, currentName);
        
        // Tag atual da guilda
        String tagDisplay = guildTag.isEmpty() ? "Não definido" : "[" + guildTag + "]";
        ItemStack currentTag = createItem(
            Material.OAK_SIGN,
            ColorUtils.colorize("&eTag Atual da Guilda"),
            ColorUtils.colorize("&7" + tagDisplay)
        );
        inventory.setItem(13, currentTag);
        
        // Descrição atual da guilda
        String descriptionDisplay = guildDescription.isEmpty() ? "Não definido" : guildDescription;
        ItemStack currentDescription = createItem(
            Material.BOOK,
            ColorUtils.colorize("&eDescrição Atual da Guilda"),
            ColorUtils.colorize("&7" + descriptionDisplay)
        );
        inventory.setItem(15, currentDescription);
    }
    
    /**
     * Tratar entrada de nome da guilda
     */
    private void handleNameInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-name", "&aPor favor, digite o nome da guilda no chat (3-20 caracteres):");
        player.sendMessage(ColorUtils.colorize(message));
        
        // Forçar fechamento da GUI para o jogador ver o prompt de entrada
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);
        
        // Atrasar configuração do modo de entrada, garantir que a GUI esteja totalmente fechada
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Configurar modo de entrada
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() < 3) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNome da guilda muito curto! Mínimo de {min} caracteres.");
                    errorMessage = errorMessage.replace("{min}", "3");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }
                
                if (input.length() > 20) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-long", "&cNome da guilda muito longo! Máximo de {max} caracteres.");
                    errorMessage = errorMessage.replace("{max}", "20");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }
                
                guildName = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.name-set", "&aNome da guilda definido como: {name}");
                successMessage = successMessage.replace("{name}", guildName);
                player.sendMessage(ColorUtils.colorize(successMessage));
                
                // Reabrir GUI mostrando conteúdo atualizado
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Atraso de 2 ticks (0.1s)
    }
    
    /**
     * Tratar entrada de tag da guilda
     */
    private void handleTagInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-tag", "&aPor favor, digite a tag da guilda no chat (máx 6 caracteres, opcional):");
        player.sendMessage(ColorUtils.colorize(message));
        
        // Forçar fechamento da GUI para o jogador ver o prompt de entrada
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);
        
        // Atrasar configuração do modo de entrada, garantir que a GUI esteja totalmente fechada
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Configurar modo de entrada
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 6) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag da guilda muito longa! Máximo de {max} caracteres.");
                    errorMessage = errorMessage.replace("{max}", "6");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }
                
                guildTag = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.tag-set", "&aTag da guilda definida como: {tag}");
                successMessage = successMessage.replace("{tag}", guildTag.isEmpty() ? "Nenhuma" : guildTag);
                player.sendMessage(ColorUtils.colorize(successMessage));
                
                // Reabrir GUI mostrando conteúdo atualizado
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Atraso de 2 ticks (0.1s)
    }
    
    /**
     * Tratar entrada de descrição da guilda
     */
    private void handleDescriptionInput(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.input-description", "&aPor favor, digite a descrição da guilda no chat (máx 100 caracteres, opcional):");
        player.sendMessage(ColorUtils.colorize(message));
        
        // Forçar fechamento da GUI para o jogador ver o prompt de entrada
        if (player.getOpenInventory() != null) {
            player.closeInventory();
        }
        plugin.getGuiManager().closeGUI(player);
        
        // Atrasar configuração do modo de entrada, garantir que a GUI esteja totalmente fechada
        CompatibleScheduler.runTaskLater(plugin, () -> {
            // Configurar modo de entrada
            plugin.getGuiManager().setInputMode(player, input -> {
                if (input.length() > 100) {
                    String errorMessage = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cA descrição da guilda não pode exceder 100 caracteres!");
                    player.sendMessage(ColorUtils.colorize(errorMessage));
                    return false;
                }
                
                guildDescription = input;
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("gui.description-set", "&aDescrição da guilda definida como: {description}");
                successMessage = successMessage.replace("{description}", guildDescription.isEmpty() ? "Nenhuma" : guildDescription);
                player.sendMessage(ColorUtils.colorize(successMessage));
                
                // Reabrir GUI mostrando conteúdo atualizado
                plugin.getGuiManager().openGUI(player, new CreateGuildGUI(plugin, guildName, guildTag, guildDescription));
                return true;
            });
        }, 2L); // Atraso de 2 ticks (0.1s)
    }
    
    /**
     * Tratar confirmação de criação
     */
    private void handleConfirmCreate(Player player) {
        // Validar entrada
        if (guildName.isEmpty()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-required", "&cPor favor, digite o nome da guilda primeiro!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (guildName.length() < 3) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNome da guilda muito curto! Mínimo de {min} caracteres.");
            message = message.replace("{min}", "3");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (guildName.length() > 20) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-long", "&cNome da guilda muito longo! Máximo de {max} caracteres.");
            message = message.replace("{max}", "20");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!guildTag.isEmpty() && guildTag.length() > 6) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag da guilda muito longa! Máximo de {max} caracteres.");
            message = message.replace("{max}", "6");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        if (!guildDescription.isEmpty() && guildDescription.length() > 100) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cA descrição da guilda não pode exceder 100 caracteres!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Criar guilda
        String finalTag = guildTag.isEmpty() ? null : guildTag;
        String finalDescription = guildDescription.isEmpty() ? null : guildDescription;
        
        plugin.getGuildService().createGuildAsync(guildName, finalTag, finalDescription, player.getUniqueId(), player.getName()).thenAccept(success -> {
            // Garantir execução de operações GUI na thread principal
            CompatibleScheduler.runTask(plugin, () -> {
                if (success) {
                    String template = plugin.getConfigManager().getMessagesConfig().getString("create.success", "&aGuilda {name} criada com sucesso!");
                    // Usar isolamento de cor para evitar que cores embutidas em {name} afetem o texto subsequente
                    String rendered = ColorUtils.replaceWithColorIsolation(template, "{name}", guildName);
                    player.sendMessage(rendered);
                    
                    // Fechar GUI e voltar ao menu principal
                    plugin.getGuiManager().closeGUI(player);
                    plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
                } else {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("create.failed", "&cFalha ao criar guilda!");
                    player.sendMessage(ColorUtils.colorize(message));
                }
            });
        });
    }
    
    /**
     * Tratar cancelamento
     */
    private void handleCancel(Player player) {
        plugin.getGuiManager().closeGUI(player);
        plugin.getGuiManager().openGUI(player, new MainGuildGUI(plugin));
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
