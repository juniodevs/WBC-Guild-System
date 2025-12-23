package com.guild.core.permissions;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.services.GuildService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PermissionManager {
    
    private final GuildPlugin plugin;
    private final Logger logger;
    private final Map<UUID, PlayerPermissions> playerPermissions = new HashMap<>();
    
    private RolePermissions defaultPermissions;
    private RolePermissions memberPermissions;
    private RolePermissions officerPermissions;
    private RolePermissions leaderPermissions;
    
    public PermissionManager(GuildPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reloadFromConfig();
    }
    
    public boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        if (player.hasPermission(permission)) {
            return true;
        }
        
        return hasInternalPermission(player, permission);
    }
    
    private boolean hasInternalPermission(Player player, String permission) {
        UUID playerUuid = player.getUniqueId();
        
        PlayerPermissions permissions = getPlayerPermissions(playerUuid);
        
        switch (permission) {
            case "guild.use":
                return true; 
                
            case "guild.create":
                return permissions.canCreateGuild();
                
            case "guild.invite":
                return permissions.canInviteMembers();
                
            case "guild.kick":
                return permissions.canKickMembers();
                
            case "guild.promote":
                return permissions.canPromoteMembers();
                
            case "guild.demote":
                return permissions.canDemoteMembers();
                
            case "guild.delete":
                return permissions.canDeleteGuild();
                
            case "guild.admin":
                return permissions.isAdmin();
                
            default:
                return false;
        }
    }
    
    private PlayerPermissions getPlayerPermissions(UUID playerUuid) {
        return playerPermissions.computeIfAbsent(playerUuid, uuid -> {
            PlayerPermissions resolved = new PlayerPermissions();
            GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
            GuildMember.Role role = null;
            if (guildService != null) {
                Guild guild = guildService.getPlayerGuild(uuid);
                if (guild != null) {
                    GuildMember member = guildService.getGuildMember(uuid);
                    if (member != null) {
                        role = member.getRole();
                    }
                }
            }
            RolePermissions rp = resolveRolePermissions(role);
            resolved.setCanCreateGuild(rp.canCreate);
            resolved.setCanInviteMembers(rp.canInvite);
            resolved.setCanKickMembers(rp.canKick);
            resolved.setCanDeleteGuild(rp.canDelete);
            resolved.setCanPromoteMembers(rp.canPromote);
            resolved.setCanDemoteMembers(rp.canDemote);
            
            return resolved;
        });
    }

    private RolePermissions resolveRolePermissions(GuildMember.Role role) {
        if (role == null) {
            return defaultPermissions;
        }
        switch (role) {
            case LEADER:
                return leaderPermissions;
            case OFFICER:
                return officerPermissions;
            case MEMBER:
            default:
                return memberPermissions;
        }
    }
    
    public void updatePlayerPermissions(UUID playerUuid) {
        playerPermissions.remove(playerUuid);
        
        getPlayerPermissions(playerUuid);
    }

    public void reloadFromConfig() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        this.defaultPermissions = readRolePermissions(cfg, "permissions.default",
                new RolePermissions(false, false, false, false, false, false));
        this.memberPermissions = readRolePermissions(cfg, "permissions.member",
                new RolePermissions(true, false, false, false, false, false));
        this.officerPermissions = readRolePermissions(cfg, "permissions.officer",
                new RolePermissions(true, true, true, false, false, false));
        
        RolePermissions leaderFallback = new RolePermissions(true, true, true, true, true, true);
        this.leaderPermissions = readRolePermissions(cfg, "permissions.leader", leaderFallback);
        playerPermissions.clear();
        logger.info("Matriz de permissões recarregada da configuração e cache de permissões de jogadores limpo");
    }

    private RolePermissions readRolePermissions(FileConfiguration cfg, String path, RolePermissions fallback) {
        if (cfg == null) return fallback;
        boolean canCreate = cfg.getBoolean(path + ".can-create", fallback.canCreate);
        boolean canInvite = cfg.getBoolean(path + ".can-invite", fallback.canInvite);
        boolean canKick = cfg.getBoolean(path + ".can-kick", fallback.canKick);
        boolean canPromote = cfg.getBoolean(path + ".can-promote", fallback.canPromote);
        boolean canDemote = cfg.getBoolean(path + ".can-demote", fallback.canDemote);
        boolean canDelete = cfg.getBoolean(path + ".can-delete", fallback.canDelete);
        return new RolePermissions(canCreate, canInvite, canKick, canPromote, canDemote, canDelete);
    }
    
    public boolean canInviteMembers(Player player) {
        if (!hasPermission(player, "guild.invite")) {
            return false;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canInviteMembers();
    }
    
    public boolean canKickMembers(Player player) {
        if (!hasPermission(player, "guild.kick")) {
            return false;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canKickMembers();
    }
    
    public boolean canDeleteGuild(Player player) {
        if (!hasPermission(player, "guild.delete")) {
            return false;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }
        
        return getPlayerPermissions(player.getUniqueId()).canDeleteGuild();
    }
    
    public boolean canCreateGuild(Player player) {
        if (!hasPermission(player, "guild.create")) {
            return false;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            return false;
        }
        
        return guildService.getPlayerGuild(player.getUniqueId()) == null;
    }
    
    private static class PlayerPermissions {
        private boolean canCreateGuild = false;
        private boolean canInviteMembers = false;
        private boolean canKickMembers = false;
        private boolean canDeleteGuild = false;
        private boolean canPromoteMembers = false;
        private boolean canDemoteMembers = false;
        private boolean isAdmin = false;
        
        public boolean canCreateGuild() { return canCreateGuild; }
        public void setCanCreateGuild(boolean canCreateGuild) { this.canCreateGuild = canCreateGuild; }
        
        public boolean canInviteMembers() { return canInviteMembers; }
        public void setCanInviteMembers(boolean canInviteMembers) { this.canInviteMembers = canInviteMembers; }
        
        public boolean canKickMembers() { return canKickMembers; }
        public void setCanKickMembers(boolean canKickMembers) { this.canKickMembers = canKickMembers; }
        
        public boolean canDeleteGuild() { return canDeleteGuild; }
        public void setCanDeleteGuild(boolean canDeleteGuild) { this.canDeleteGuild = canDeleteGuild; }
        
        public boolean canPromoteMembers() { return canPromoteMembers; }
        public void setCanPromoteMembers(boolean canPromoteMembers) { this.canPromoteMembers = canPromoteMembers; }
        
        public boolean canDemoteMembers() { return canDemoteMembers; }
        public void setCanDemoteMembers(boolean canDemoteMembers) { this.canDemoteMembers = canDemoteMembers; }
        
        public boolean isAdmin() { return isAdmin; }
        public void setAdmin(boolean admin) { isAdmin = admin; }
    }

    private static class RolePermissions {
        final boolean canCreate;
        final boolean canInvite;
        final boolean canKick;
        final boolean canPromote;
        final boolean canDemote;
        final boolean canDelete;
        RolePermissions(boolean canCreate, boolean canInvite, boolean canKick, boolean canPromote, boolean canDemote, boolean canDelete) {
            this.canCreate = canCreate;
            this.canInvite = canInvite;
            this.canKick = canKick;
            this.canPromote = canPromote;
            this.canDemote = canDemote;
            this.canDelete = canDelete;
        }
    }
}
