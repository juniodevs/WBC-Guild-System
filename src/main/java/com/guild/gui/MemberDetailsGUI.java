package com.guild.gui;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUI;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.PlaceholderUtils;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GUI de Detalhes do Membro
 */
public class MemberDetailsGUI implements GUI {
    
    private final GuildPlugin plugin;
    private final Guild guild;
    private final GuildMember member;
    private final Player viewer;
    
    public MemberDetailsGUI(GuildPlugin plugin, Guild guild, GuildMember member, Player viewer) {
        this.plugin = plugin;
        this.guild = guild;
        this.member = member;
        this.viewer = viewer;
    }
    
    @Override
    public String getTitle() {
        return ColorUtils.colorize(plugin.getConfigManager().getGuiConfig().getString("member-details.title", "&6Detalhes do Membro")
            .replace("{member_name}", member.getPlayerName()));
    }
    
    @Override
    public int getSize() {
        return plugin.getConfigManager().getGuiConfig().getInt("member-details.size", 54);
    }
    
    @Override
    public void setupInventory(Inventory inventory) {
        // Preenche a borda
        fillBorder(inventory);
        
        // Configura cabeça do membro
        setupMemberHead(inventory);
        
        // Configura informações básicas
        setupBasicInfo(inventory);
        
        // Configura informações de permissão
        setupPermissionInfo(inventory);
        
        // Configura botões de ação
        setupActionButtons(inventory);
        
        // Configura botão de voltar
        setupBackButton(inventory);
    }
    
    @Override
    public void onClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        // Verifica se é botão de ação
        if (isActionButton(slot)) {
            handleActionButton(player, slot);
            return;
        }
        
        // Verifica se é botão de voltar
        if (slot == 49) {
            plugin.getGuiManager().openGUI(player, new MemberManagementGUI(plugin, guild));
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
     * Configura cabeça do membro
     */
    private void setupMemberHead(Inventory inventory) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            // Define nome de exibição baseado no cargo
            String displayName;
            switch (member.getRole()) {
                case LEADER:
                    displayName = ColorUtils.colorize("&c" + member.getPlayerName() + " &7(Líder)");
                    break;
                case OFFICER:
                    displayName = ColorUtils.colorize("&6" + member.getPlayerName() + " &7(Oficial)");
                    break;
                default:
                    displayName = ColorUtils.colorize("&f" + member.getPlayerName() + " &7(Membro)");
                    break;
            }
            
            meta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7UUID: &f" + member.getPlayerUuid()));
            lore.add(ColorUtils.colorize("&7Cargo: &f" + member.getRole().getDisplayName()));
            
            // Formata data de entrada
            if (member.getJoinedAt() != null) {
                String joinTime = member.getJoinedAt().format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
                lore.add(ColorUtils.colorize("&7Entrou em: &f" + joinTime));
            } else {
                lore.add(ColorUtils.colorize("&7Entrou em: &fDesconhecido"));
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        inventory.setItem(13, head);
    }
    
    /**
     * Configura informações básicas
     */
    private void setupBasicInfo(Inventory inventory) {
        // Título de informações básicas
        ItemStack infoTitle = createItem(
            Material.BOOK,
            ColorUtils.colorize("&6Informações Básicas"),
            ColorUtils.colorize("&7Informações detalhadas do membro")
        );
        inventory.setItem(20, infoTitle);
        
        // Informações do cargo
        ItemStack roleInfo = createItem(
            Material.GOLDEN_HELMET,
            ColorUtils.colorize("&eInformações do Cargo"),
            ColorUtils.colorize("&7Cargo Atual: &f" + member.getRole().getDisplayName()),
            ColorUtils.colorize("&7Nível do Cargo: &f" + getRoleLevel(member.getRole())),
            ColorUtils.colorize("&7Online: &f" + (isPlayerOnline(member.getPlayerUuid()) ? "&aSim" : "&cNão"))
        );
        inventory.setItem(21, roleInfo);
        
        // Informações de tempo
        ItemStack timeInfo = createItem(
            Material.CLOCK,
            ColorUtils.colorize("&eInformações de Tempo"),
            ColorUtils.colorize("&7Entrou em: &f" + formatTime(member.getJoinedAt())),
            ColorUtils.colorize("&7Tempo na Guilda: &f" + getGuildDuration(member.getJoinedAt()))
        );
        inventory.setItem(22, timeInfo);
        
        // Informações de contribuição
        ItemStack contributionInfo = createItem(
            Material.EMERALD,
            ColorUtils.colorize("&eInformações de Contribuição"),
            ColorUtils.colorize("&7Contribuição: &f" + getMemberContribution()),
            ColorUtils.colorize("&7Atividade: &f" + getMemberActivity())
        );
        inventory.setItem(23, contributionInfo);
    }
    
    /**
     * Configura informações de permissão
     */
    private void setupPermissionInfo(Inventory inventory) {
        // Título de informações de permissão
        ItemStack permissionTitle = createItem(
            Material.SHIELD,
            ColorUtils.colorize("&6Informações de Permissão"),
            ColorUtils.colorize("&7Permissões atuais")
        );
        inventory.setItem(29, permissionTitle);
        
        // Lista de permissões específicas
        List<String> permissions = getRolePermissions(member.getRole());
        ItemStack permissionList = createItem(
            Material.PAPER,
            ColorUtils.colorize("&eLista de Permissões"),
            permissions.toArray(new String[0])
        );
        inventory.setItem(30, permissionList);
        
        // Nível de permissão
        ItemStack permissionLevel = createItem(
            Material.EXPERIENCE_BOTTLE,
            ColorUtils.colorize("&eNível de Permissão"),
            ColorUtils.colorize("&7Nível Atual: &f" + getPermissionLevel(member.getRole())),
            ColorUtils.colorize("&7Ações Executáveis: &f" + getExecutableActions(member.getRole()))
        );
        inventory.setItem(31, permissionLevel);
    }
    
    /**
     * Configura botões de ação
     */
    private void setupActionButtons(Inventory inventory) {
        // Verifica se o jogador atual tem permissão para executar ação
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), viewer.getUniqueId()).thenAccept(viewerMember -> {
            if (viewerMember == null) return;
            
            // Não pode modificar a si mesmo
            if (member.getPlayerUuid().equals(viewer.getUniqueId())) {
                return;
            }
            
            // Não pode modificar o líder
            if (member.getRole() == GuildMember.Role.LEADER) {
                return;
            }
            
            // Botão de expulsar (requer permissão de expulsar)
            if (viewerMember.getRole().canKick()) {
                ItemStack kickButton = createItem(
                    Material.REDSTONE_BLOCK,
                    ColorUtils.colorize("&cExpulsar Membro"),
                    ColorUtils.colorize("&7Expulsar membro da guilda"),
                    ColorUtils.colorize("&7Clique para confirmar expulsão")
                );
                inventory.setItem(37, kickButton);
            }
            
            // Botão de promover/rebaixar (apenas líder)
            if (viewerMember.getRole() == GuildMember.Role.LEADER) {
                if (member.getRole() == GuildMember.Role.OFFICER) {
                    // Botão de rebaixar
                    ItemStack demoteButton = createItem(
                        Material.IRON_INGOT,
                        ColorUtils.colorize("&7Rebaixar Membro"),
                        ColorUtils.colorize("&7Rebaixar Oficial para Membro"),
                        ColorUtils.colorize("&7Clique para confirmar rebaixamento")
                    );
                    inventory.setItem(39, demoteButton);
                } else {
                    // Botão de promover
                    ItemStack promoteButton = createItem(
                        Material.GOLD_INGOT,
                        ColorUtils.colorize("&6Promover Membro"),
                        ColorUtils.colorize("&7Promover Membro para Oficial"),
                        ColorUtils.colorize("&7Clique para confirmar promoção")
                    );
                    inventory.setItem(39, promoteButton);
                }
            }
            
            // Botão de enviar mensagem
            ItemStack messageButton = createItem(
                Material.PAPER,
                ColorUtils.colorize("&eEnviar Mensagem"),
                ColorUtils.colorize("&7Enviar mensagem privada para este membro"),
                ColorUtils.colorize("&7Clique para abrir o chat")
            );
            inventory.setItem(41, messageButton);
        });
    }
    
    /**
     * Configura botão de voltar
     */
    private void setupBackButton(Inventory inventory) {
        ItemStack back = createItem(
            Material.ARROW,
            ColorUtils.colorize("&7Voltar"),
            ColorUtils.colorize("&7Voltar ao gerenciamento de membros")
        );
        inventory.setItem(49, back);
    }
    
    /**
     * Verifica se é botão de ação
     */
    private boolean isActionButton(int slot) {
        return slot == 37 || slot == 39 || slot == 41;
    }
    
    /**
     * Processa clique em botão de ação
     */
    private void handleActionButton(Player player, int slot) {
        switch (slot) {
            case 37: // Expulsar membro
                handleKickMember(player);
                break;
            case 39: // Promover/Rebaixar membro
                handlePromoteDemoteMember(player);
                break;
            case 41: // Enviar mensagem
                handleSendMessage(player);
                break;
        }
    }
    
    /**
     * Processa expulsão de membro
     */
    private void handleKickMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || !executor.getRole().canKick()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.no-permission", "&cPermissão insuficiente");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            // Confirmar expulsão
            String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-kick", "&cTem certeza que deseja expulsar {member}? Digite &f/guild kick {member} confirm &cpara confirmar")
                .replace("{member}", member.getPlayerName());
            player.sendMessage(ColorUtils.colorize(message));
            player.closeInventory();
        });
    }
    
    /**
     * Processa promoção/rebaixamento de membro
     */
    private void handlePromoteDemoteMember(Player player) {
        // Verifica permissão
        plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(executor -> {
            if (executor == null || executor.getRole() != GuildMember.Role.LEADER) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.leader-only", "&cApenas o líder da guilda pode realizar esta ação");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            
            if (member.getRole() == GuildMember.Role.OFFICER) {
                // Rebaixar
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-demote", "&cTem certeza que deseja rebaixar {member}? Digite &f/guild demote {member} confirm &cpara confirmar")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            } else {
                // Promover
                String message = plugin.getConfigManager().getMessagesConfig().getString("gui.confirm-promote", "&aTem certeza que deseja promover {member} a Oficial? Digite &f/guild promote {member} confirm &apara confirmar")
                    .replace("{member}", member.getPlayerName());
                player.sendMessage(ColorUtils.colorize(message));
            }
            player.closeInventory();
        });
    }
    
    /**
     * Processa envio de mensagem
     */
    private void handleSendMessage(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().getString("gui.open-chat", "&eDigite a mensagem para enviar para {member}:")
            .replace("{member}", member.getPlayerName());
        player.sendMessage(ColorUtils.colorize(message));
        player.closeInventory();
        
        // Aqui pode ser integrado o sistema de chat, por enquanto é apenas um aviso
        // TODO: Implementar sistema de mensagem privada
    }
    
    /**
     * Obtém nível do cargo
     */
    private String getRoleLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Nível Máximo";
            case OFFICER:
                return "Avançado";
            default:
                return "Normal";
        }
    }
    
    /**
     * Verifica se jogador está online
     */
    private boolean isPlayerOnline(java.util.UUID playerUuid) {
        Player player = plugin.getServer().getPlayer(playerUuid);
        return player != null && player.isOnline();
    }
    
    /**
     * Formata tempo
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "Desconhecido";
        return dateTime.format(com.guild.core.time.TimeProvider.FULL_FORMATTER);
    }
    
    /**
     * Obtém tempo na guilda
     */
    private String getGuildDuration(java.time.LocalDateTime joinDateTime) {
        if (joinDateTime == null) return "Desconhecido";
        
        java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(joinDateTime, currentTime);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + " dias " + hours + " horas";
        } else if (hours > 0) {
            return hours + " horas " + minutes + " minutos";
        } else {
            return minutes + " minutos";
        }
    }
    
    /**
     * Obtém contribuição do membro
     */
    private String getMemberContribution() {
        // TODO: Implementar sistema de estatísticas de contribuição
        return "Pendente";
    }
    
    /**
     * Obtém atividade do membro
     */
    private String getMemberActivity() {
        // TODO: Implementar sistema de estatísticas de atividade
        return "Pendente";
    }
    
    /**
     * Obtém lista de permissões do cargo
     */
    private List<String> getRolePermissions(GuildMember.Role role) {
        List<String> permissions = new ArrayList<>();
        
        switch (role) {
            case LEADER:
                permissions.add(ColorUtils.colorize("&7✓ Todas as permissões"));
                permissions.add(ColorUtils.colorize("&7✓ Convidar Membros"));
                permissions.add(ColorUtils.colorize("&7✓ Expulsar Membros"));
                permissions.add(ColorUtils.colorize("&7✓ Promover/Rebaixar"));
                permissions.add(ColorUtils.colorize("&7✓ Gerenciar Guilda"));
                permissions.add(ColorUtils.colorize("&7✓ Dissolver Guilda"));
                break;
            case OFFICER:
                permissions.add(ColorUtils.colorize("&7✓ Convidar Membros"));
                permissions.add(ColorUtils.colorize("&7✓ Expulsar Membros"));
                permissions.add(ColorUtils.colorize("&7✗ Promover/Rebaixar"));
                permissions.add(ColorUtils.colorize("&7✗ Gerenciar Guilda"));
                permissions.add(ColorUtils.colorize("&7✗ Dissolver Guilda"));
                break;
            default:
                permissions.add(ColorUtils.colorize("&7✗ Convidar Membros"));
                permissions.add(ColorUtils.colorize("&7✗ Expulsar Membros"));
                permissions.add(ColorUtils.colorize("&7✗ Promover/Rebaixar"));
                permissions.add(ColorUtils.colorize("&7✗ Gerenciar Guilda"));
                permissions.add(ColorUtils.colorize("&7✗ Dissolver Guilda"));
                break;
        }
        
        return permissions;
    }
    
    /**
     * Obtém nível de permissão
     */
    private String getPermissionLevel(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Nível Máximo (Nível 3)";
            case OFFICER:
                return "Avançado (Nível 2)";
            default:
                return "Normal (Nível 1)";
        }
    }
    
    /**
     * Obtém ações executáveis
     */
    private String getExecutableActions(GuildMember.Role role) {
        switch (role) {
            case LEADER:
                return "Todas as ações";
            case OFFICER:
                return "Convidar, Expulsar";
            default:
                return "Ações básicas";
        }
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
