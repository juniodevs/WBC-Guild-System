package com.guild.core.utils;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.time.TimeProvider;
import com.guild.models.Guild;
import com.guild.models.GuildMember;

/**
 * 占位符处理工具类
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
     * 替换工会相关占位符
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @return 替换后的文本
     */
    public static String replaceGuildPlaceholders(String text, Guild guild, Player player) {
        if (text == null || guild == null) {
            return text;
        }
        
        String result = text
            // 工会基本信息
            .replace("{guild_name}", guild.getName())
            .replace("{guild_tag}", guild.getTag() != null ? guild.getTag() : "")
            .replace("{guild_description}", guild.getDescription() != null ? guild.getDescription() : "")
            .replace("{guild_id}", String.valueOf(guild.getId()))
            .replace("{guild_created_time}", guild.getCreatedAt().format(DATE_FORMATTER))
            .replace("{guild_created_date}", guild.getCreatedAt().toLocalDate().toString())
            
            // 工会领导信息
            .replace("{leader_name}", guild.getLeaderName())
            .replace("{leader_uuid}", guild.getLeaderUuid().toString())
            
            // 玩家信息
            .replace("{player_name}", player != null ? player.getName() : "")
            .replace("{player_uuid}", player != null ? player.getUniqueId().toString() : "")
            .replace("{player_display_name}", player != null ? player.getDisplayName() : "")
            
            // 静态信息
            .replace("{guild_level}", String.valueOf(guild.getLevel()))
            .replace("{guild_max_members}", String.valueOf(guild.getMaxMembers()))
            .replace("{guild_frozen}", guild.isFrozen() ? "Congelada" : "Normal")
            
            // 工会经济和升级信息(valores padrão até implementar sistema econômico)
            .replace("{guild_balance}", "0.00")
            .replace("{guild_balance_formatted}", "0")
            .replace("{guild_next_level_requirement}", formatBalance(getNextLevelRequirement(guild.getLevel())))
            .replace("{guild_level_progress}", "0%");
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 异步替换工会相关占位符（包含动态数据）
     * @param text 原始文本
     * @param guild 工会对象
     * @param player 玩家对象
     * @param guildService 工会服务
     * @return 替换后的文本的CompletableFuture
     */
    public static CompletableFuture<String> replaceGuildPlaceholdersAsync(String text, Guild guild, Player player, com.guild.services.GuildService guildService) {
        if (text == null || guild == null) {
            return CompletableFuture.completedFuture(text);
        }
        
        // 先替换静态占位符
        String result = replaceGuildPlaceholders(text, guild, player);
        
        // 异步获取动态数据
        return guildService.getGuildMemberCountAsync(guild.getId()).thenApply(memberCount -> {
            try {
                return result
                    .replace("{member_count}", String.valueOf(memberCount))
                    .replace("{online_member_count}", String.valueOf(memberCount)); // 暂时使用总成员数，后续可以添加在线统计
            } catch (Exception e) {
                // 如果获取失败，使用默认值
                return result
                    .replace("{member_count}", "0")
                    .replace("{online_member_count}", "0");
            }
        });
    }
    
    /**
     * 替换成员相关占位符
     * @param text 原始文本
     * @param member 成员对象
     * @param guild 工会对象
     * @return 替换后的文本
     */
    public static String replaceMemberPlaceholders(String text, GuildMember member, Guild guild) {
        if (text == null || member == null) {
            return text;
        }
        
        String result = text
            // 成员基本信息
            .replace("{member_name}", member.getPlayerName())
            .replace("{member_uuid}", member.getPlayerUuid().toString())
            .replace("{member_role}", getRoleDisplayName(member.getRole()))
            .replace("{member_role_color}", getRoleColorFromConfig(member.getRole()))
            .replace("{member_join_time}", member.getJoinedAt().format(DATE_FORMATTER))
            .replace("{member_join_date}", member.getJoinedAt().toLocalDate().toString())
            
            // 工会信息
            .replace("{guild_name}", guild != null ? guild.getName() : "")
            .replace("{guild_tag}", guild != null && guild.getTag() != null ? guild.getTag() : "");
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 替换申请相关占位符
     * @param text 原始文本
     * @param applicantName 申请人名称
     * @param guildName 工会名称
     * @param applyTime 申请时间
     * @return 替换后的文本
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
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    
    /**
     * 替换通用占位符
     * @param text 原始文本
     * @param placeholders 占位符映射
     * @return 替换后的文本
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
        
        // 处理颜色代码
        return ColorUtils.colorize(result);
    }
    

    /**
     * 获取角色显示名称
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
     * 从配置获取角色颜色
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
     * 对外提供：获取职位颜色代码（如 "&6"）
     */
    public static String getRoleColorCode(GuildMember.Role role) {
        return getRoleColorFromConfig(role);
    }

    /**
     * 对外提供：获取带颜色的职位显示文本
     */
    public static String getColoredRoleDisplay(GuildMember.Role role) {
        String color = getRoleColorFromConfig(role);
        return ColorUtils.colorize(color + getRoleDisplayName(role));
    }

    /**
     * 获取职位分隔符（根据配置与是否有职位决定是否返回）
     */
    public static String getRoleSeparator(GuildMember.Role roleOrNull) {
        ensureRoleConfigCached();
        if (!cachedSeparatorEnabled) {
            return "";
        }
        // 未入会或无角色时不显示分隔符
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
            // 合理的默认值
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
