package com.guild.core.placeholder;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.guild.core.utils.PlaceholderUtils;

import com.guild.core.time.TimeProvider;
import java.util.concurrent.CompletableFuture;

/**
 * Guild插件 PlaceholderAPI 扩展
 * 提供完整的工会数据变量支持
 */
public class GuildPlaceholderExpansion extends PlaceholderExpansion {
    
    private final GuildPlugin plugin;
    private final GuildService guildService;
    
    public GuildPlaceholderExpansion(GuildPlugin plugin, GuildService guildService) {
        this.plugin = plugin;
        this.guildService = guildService;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "guild";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "GuildTeam";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        String[] args = params.split("_");
        if (args.length == 0) {
            return "";
        }
        
        try {
            switch (args[0].toLowerCase()) {
                // 基础工会信息
                case "name":
                    return getGuildName(player);
                case "tag":
                    return getGuildTag(player);
                case "description":
                    return getGuildDescription(player);
                case "leader":
                    return getGuildLeader(player);
                case "membercount":
                    return getGuildMemberCount(player);
                case "maxmembers":
                    return getGuildMaxMembers(player);
                case "level":
                    return getGuildLevel(player);
                case "frozen":
                    return getGuildFrozenStatus(player);
                
                // 玩家在工会中的信息
                case "role":
                    return getPlayerRoleColored(player);
                case "roleraw":
                    return getPlayerRoleRaw(player);
                case "rolecolor":
                    return getPlayerRoleColor(player);
                case "rolecolored":
                    return getPlayerRoleColored(player);
                case "roleprefix":
                    return getPlayerRolePrefix(player);
                case "joined":
                    return getPlayerJoinedTime(player);
                
                // 工会状态检查
                case "hasguild":
                    return hasGuild(player);
                case "isleader":
                    return isLeader(player);
                case "isofficer":
                    return isOfficer(player);
                case "ismember":
                    return isMember(player);
                
                // 工会权限
                case "caninvite":
                    return canInvite(player);
                case "cankick":
                    return canKick(player);
                case "canpromote":
                    return canPromote(player);
                case "candemote":
                    return canDemote(player);
                
                default:
                    return "";
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar placeholder: " + e.getMessage());
            return "";
        }
    }
    
    // ==================== 基础工会信息 ====================
    
    private String getGuildName(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getName() : "Sem Guilda";
        } catch (Exception e) {
            return "Sem Guilda";
        }
    }
    
    private String getGuildTag(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getTag() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildDescription(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getDescription() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildLeader(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? guild.getLeaderName() : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getGuildMemberCount(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            if (guild == null) return "0";
            
            CompletableFuture<Integer> future = guildService.getGuildMemberCountAsync(guild.getId());
            return String.valueOf(future.get());
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getGuildMaxMembers(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getMaxMembers()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getGuildLevel(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? String.valueOf(guild.getLevel()) : "0";
        } catch (Exception e) {
            return "0";
        }
    }
    

    private String getGuildFrozenStatus(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? (guild.isFrozen() ? "Congelado" : "Normal") : "Sem Guilda";
        } catch (Exception e) {
            return "Sem Guilda";
        }
    }
    
    // ==================== 玩家在工会中的信息 ====================
    
    private String getPlayerRoleRaw(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? member.getRole().getDisplayName() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRoleColor(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "";
            return PlaceholderUtils.getRoleColorCode(member.getRole());
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRoleColored(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "";
            return PlaceholderUtils.getColoredRoleDisplay(member.getRole());
        } catch (Exception e) {
            return "";
        }
    }

    private String getPlayerRolePrefix(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            GuildMember.Role role = member != null ? member.getRole() : null;
            return PlaceholderUtils.getRoleSeparator(role);
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getPlayerJoinedTime(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null || member.getJoinedAt() == null) return "";
            return member.getJoinedAt().format(TimeProvider.FULL_FORMATTER);
        } catch (Exception e) {
            return "";
        }
    }
    

    // ==================== 工会状态检查 ====================
    
    private String hasGuild(Player player) {
        try {
            Guild guild = guildService.getPlayerGuild(player.getUniqueId());
            return guild != null ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String isLeader(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.LEADER ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String isOfficer(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null && member.getRole() == GuildMember.Role.OFFICER ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String isMember(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            return member != null ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    // ==================== 工会权限 ====================
    
    private String canInvite(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Não";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String canKick(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Não";
            
            return (member.getRole() == GuildMember.Role.LEADER || member.getRole() == GuildMember.Role.OFFICER) ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String canPromote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Não";
            
            return member.getRole() == GuildMember.Role.LEADER ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
    private String canDemote(Player player) {
        try {
            GuildMember member = guildService.getGuildMember(player.getUniqueId());
            if (member == null) return "Não";
            
            return member.getRole() == GuildMember.Role.LEADER ? "Sim" : "Não";
        } catch (Exception e) {
            return "Não";
        }
    }
    
}
