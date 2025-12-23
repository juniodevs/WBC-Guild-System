package com.guild.core.utils;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.time.TimeProvider;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * Utilitários de processamento de placeholders
 */
public class PlaceholderUtils {
    
    private static final DateTimeFormatter DATE_FORMATTER = TimeProvider.FULL_FORMATTER;
    private static String cachedLeaderColor;
    private static String cachedOfficerColor;
    private static String cachedMemberColor;
    private static String cachedSeparatorText;
    private static boolean cachedSeparatorEnabled;
    private static boolean cachedSeparatorFollowRoleColor;
    private static String cachedSeparatorDefaultColor;
    
    /**
     * Substitui placeholders relacionados à guilda
     * @param text Texto original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @return Texto substituído
     */
    public static String replaceGuildPlaceholders(String text, Guild guild, Player player) {
        if (text == null || guild == null) {
            return text;
        }
        
        String result = text
            // Informações básicas da guilda
            .replace("{guild_name}", guild.getName())
            .replace("{guild_tag}", guild.getTag() != null ? guild.getTag() : "")
            .replace("{guild_description}", guild.getDescription() != null ? guild.getDescription() : "")
            .replace("{guild_id}", String.valueOf(guild.getId()))
            .replace("{guild_created_time}", guild.getCreatedAt().format(DATE_FORMATTER))
            .replace("{guild_created_date}", guild.getCreatedAt().toLocalDate().toString())
            
            // Informações do líder da guilda
            .replace("{leader_name}", guild.getLeaderName())
            .replace("{leader_uuid}", guild.getLeaderUuid().toString())
            
            // Informações do jogador
            .replace("{player_name}", player != null ? player.getName() : "")
            .replace("{player_uuid}", player != null ? player.getUniqueId().toString() : "")
            .replace("{player_display_name}", player != null ? player.getDisplayName() : "")
            
            // Informações estáticas
            .replace("{guild_level}", String.valueOf(guild.getLevel()))
            .replace("{guild_max_members}", String.valueOf(guild.getMaxMembers()))
            .replace("{guild_frozen}", guild.isFrozen() ? "Congelado" : "Normal");
        
        // Processa códigos de cores
        return ColorUtils.colorize(result);
    }
    
    /**
     * Substitui assincronamente placeholders relacionados à guilda (incluindo dados dinâmicos)
     * @param text Texto original
     * @param guild Objeto da guilda
     * @param player Objeto do jogador
     * @param guildService Serviço da guilda
     * @return CompletableFuture do texto substituído
     */
    public static CompletableFuture<String> replaceGuildPlaceholdersAsync(String text, Guild guild, Player player, com.guild.services.GuildService guildService) {
        if (text == null || guild == null) {
            return CompletableFuture.completedFuture(text);
        }
        
        // Substitui placeholders estáticos primeiro
        String result = replaceGuildPlaceholders(text, guild, player);
        
        // Obtém dados dinâmicos assincronamente
        return guildService.getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            try {
                return result
                    .replace("{member_count}", String.valueOf(memberCount))
                    .replace("{online_member_count}", String.valueOf(memberCount)); // Usando contagem total de membros temporariamente, pode adicionar estatísticas online depois
            } catch (Exception e) {
                // Se falhar ao obter, usa valor padrão
                return result
                    .replace("{member_count}", "0")
                    .replace("{online_member_count}", "0");
            }
        });
    }
    
    /**
     * Substitui placeholders relacionados a membros
     * @param text Texto original
     * @param member Objeto do membro
     * @param guild Objeto da guilda
     * @return Texto substituído
     */
    public static String replaceMemberPlaceholders(String text, GuildMember member, Guild guild) {
        if (text == null || member == null) {
            return text;
        }
        
        String result = text
            // Informações básicas do membro
            .replace("{member_name}", member.getPlayerName())
            .replace("{member_uuid}", member.getPlayerUuid().toString())
            .replace("{member_role}", getRoleDisplayName(member.getRole()))
            .replace("{member_role_color}", getRoleColorFromConfig(member.getRole()))
            .replace("{member_join_time}", member.getJoinedAt().format(DATE_FORMATTER))
            .replace("{member_join_date}", member.getJoinedAt().toLocalDate().toString())
            
            // Informações da guilda
            .replace("{guild_name}", guild != null ? guild.getName() : "")
            .replace("{guild_tag}", guild != null && guild.getTag() != null ? guild.getTag() : "");
        
        // Processa códigos de cores
        return ColorUtils.colorize(result);
    }
    
    /**
     * Substitui placeholders relacionados a aplicações
     * @param text Texto original
     * @param applicantName Nome do solicitante
     * @param guildName Nome da guilda
     * @param applyTime Data da solicitação
     * @return Texto substituído
     */
    public static String replaceApplicationPlaceholders(String text, String applicantName, String guildName, java.time.LocalDateTime applyTime) {
        if (text == null) {
            return text;
        }
        
        String result = text
            .replace("{applicant_name}", applicantName != null ? applicantName : "")
            .replace("{guild_name}", guildName != null ? guildName : "")
            .replace("{apply_time}", applyTime != null ? applyTime.format(DATE_FORMATTER) : "")
            .replace("{apply_date}", applyTime != null ? applyTime.toLocalDate().toString() : "");
        
        // Processa códigos de cores
        return ColorUtils.colorize(result);
    }
    
    /**
     * Substitui placeholders gerais
     * @param text Texto original
     * @param placeholders Mapeamento de placeholders
     * @return Texto substituído
     */
    public static String replacePlaceholders(String text, String... placeholders) {
        if (text == null) {
            return text;
        }
        
        String result = text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                result = result.replace(placeholder, value != null ? value : "");
            }
        }
        
        // Processa códigos de cores
        return ColorUtils.colorize(result);
    }
    

    /**
     * Obtém o nome de exibição do cargo
     */
    private static String getRoleDisplayName(GuildMember.Role role) {
        switch (role) {
            case LEADER: return "Líder";
            case OFFICER: return "Oficial";
            case MEMBER: return "Membro";
            default: return "Desconhecido";
        }
    }
    
    /**
     * Obtém a cor do cargo da configuração
     */
    private static String getRoleColorFromConfig(GuildMember.Role role) {
        ensureRoleConfigCached();
        switch (role) {
            case LEADER: return cachedLeaderColor;
            case OFFICER: return cachedOfficerColor;
            case MEMBER: return cachedMemberColor;
            default: return "&f";
        }
    }

    /**
     * Fornece externamente: Obtém o código de cor do cargo (ex: "&6")
     */
    public static String getRoleColorCode(GuildMember.Role role) {
        return getRoleColorFromConfig(role);
    }

    /**
     * Fornece externamente: Obtém o texto de exibição do cargo com cor
     */
    public static String getColoredRoleDisplay(GuildMember.Role role) {
        String color = getRoleColorFromConfig(role);
        return ColorUtils.colorize(color + getRoleDisplayName(role));
    }

    /**
     * Obtém o separador de cargo (decide se retorna com base na configuração e se tem cargo)
     */
    public static String getRoleSeparator(GuildMember.Role roleOrNull) {
        ensureRoleConfigCached();
        if (!cachedSeparatorEnabled) {
            return "";
        }
        // Não mostra separador se não for membro ou não tiver cargo
        if (roleOrNull == null) {
            return "";
        }
        String color = cachedSeparatorFollowRoleColor ? getRoleColorFromConfig(roleOrNull) : cachedSeparatorDefaultColor;
        return ColorUtils.colorize(color + cachedSeparatorText);
    }

    private static void ensureRoleConfigCached() {
        if (cachedLeaderColor != null) {
            return;
        }
        GuildPlugin plugin = GuildPlugin.getInstance();
        if (plugin == null || plugin.getConfigManager() == null) {
            // Valores padrão razoáveis
            cachedLeaderColor = "&6";
            cachedOfficerColor = "&b";
            cachedMemberColor = "&7";
            cachedSeparatorText = " | ";
            cachedSeparatorEnabled = true;
            cachedSeparatorFollowRoleColor = true;
            cachedSeparatorDefaultColor = "&7";
            return;
        }
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        cachedLeaderColor = cfg.getString("display.role-colors.leader", "&6");
        cachedOfficerColor = cfg.getString("display.role-colors.officer", "&b");
        cachedMemberColor = cfg.getString("display.role-colors.member", "&7");
        cachedSeparatorText = cfg.getString("display.role-separator.text", " | ");
        cachedSeparatorEnabled = cfg.getBoolean("display.role-separator.enabled", true);
        cachedSeparatorFollowRoleColor = cfg.getBoolean("display.role-separator.color-per-role", true);
        cachedSeparatorDefaultColor = cfg.getString("display.role-separator.default-color", "&7");
    }
    
    /**
     * 格式化余额显示
     */
    private static String formatBalance(double balance) {
        if (balance >= 1000000) {
            return String.format("%.1fM", balance / 1000000);
        } else if (balance >= 1000) {
            return String.format("%.1fK", balance / 1000);
        } else {
            return String.format("%.2f", balance);
        }
    }
    
    /**
     * 获取下一级升级所需金额
     */
    private static double getNextLevelRequirement(int currentLevel) {
        // 基础升级费用，可以根据需要调整公式
        return 10000 * Math.pow(1.5, currentLevel);
    }
    
    /**
     * 格式化升级进度显示
     */
    private static String formatLevelProgress(double currentBalance, double requiredBalance) {
        if (requiredBalance <= 0) {
            return "100%";
        }
        double progress = (currentBalance / requiredBalance) * 100;
        progress = Math.min(progress, 100); // 限制最大为100%
        return String.format("%.1f%%", progress);
    }
    
    /**
     * Formata o progresso de nível (sobrecarga para quando não há balance)
     */
    private static String formatLevelProgress() {
        return "0%";
    }
    
}
