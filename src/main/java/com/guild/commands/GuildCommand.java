package com.guild.commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.guild.GuildPlugin;
import com.guild.core.utils.ColorUtils;
import com.guild.core.utils.CompatibleScheduler;
import com.guild.gui.MainGuildGUI;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import com.guild.models.GuildRelation;
import com.guild.services.GuildService;
public class GuildCommand implements CommandExecutor, TabCompleter {
    private final GuildPlugin plugin;
    public GuildCommand(GuildPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.player-only", "&cEste comando só pode ser executado por jogadores!")));
            return true;
        }
        if (args.length == 0) {
            MainGuildGUI mainGuildGUI = new MainGuildGUI(plugin);
            plugin.getGuiManager().openGUI(player, mainGuildGUI);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player, args);
                break;
            case "info":
                handleInfo(player);
                break;
            case "members":
                handleMembers(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "decline":
                handleDecline(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "relation":
                handleRelation(player, args);
                break;
            case "logs":
                handleLogs(player, args);
                break;
            case "placeholder":
                handlePlaceholder(player, args);
                break;
            case "time":
                handleTime(player);
                break;
            case "help":
                handleHelp(player);
                break;
            default:
                player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.unknown-command", "&cComando desconhecido! Use /guild help para ver a ajuda.")));
                break;
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "info", "members", "invite", "kick", "promote", "demote", "accept", "decline", "leave", "delete", "relation", "logs", "placeholder", "time", "help"
            );
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "relation":
                    List<String> relationSubCommands = Arrays.asList("list", "create", "delete", "accept", "reject");
                    for (String cmd : relationSubCommands) {
                        if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(cmd);
                        }
                    }
                    break;
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                List<String> suggestions = Arrays.asList("Nome da Guilda Alvo");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            } else if (subCommand.equals("relation") && (subSubCommand.equals("delete") || subSubCommand.equals("accept") || subSubCommand.equals("reject"))) {
                List<String> suggestions = Arrays.asList("Nome da Guilda");
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(suggestion);
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            if (subCommand.equals("relation") && subSubCommand.equals("create")) {
                List<String> relationTypes = Arrays.asList("ally", "enemy", "war", "truce", "neutral");
                for (String type : relationTypes) {
                    if (type.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(type);
                    }
                }
            }
        }
        return completions;
    }
    private void handleCreate(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.create")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cVocê não tem permissão para realizar esta operação!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.usage", "&eUso: /guild create <Nome da Guilda> [Tag] [Descrição]");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String name = args[1];
        String tag = args.length > 2 ? args[2] : null;
        String description = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
        if (name.length() < 3 || name.length() > 20) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.name-too-short", "&cNome da guilda muito curto! Mínimo de 3 caracteres.");
            player.sendMessage(ColorUtils.colorize(message.replace("{min}", "3")));
            return;
        }
        if (tag != null && (tag.length() < 2 || tag.length() > 6)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.tag-too-long", "&cTag da guilda muito longa! Máximo de 6 caracteres.");
            player.sendMessage(ColorUtils.colorize(message.replace("{max}", "6")));
            return;
        }
        if (description != null && description.length() > 100) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("create.description-too-long", "&cA descrição da guilda não pode exceder 100 caracteres!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        guildService.createGuildAsync(name, tag, description, player.getUniqueId(), player.getName())
            .thenAcceptAsync(success -> {
                if (success) {
                    String template = plugin.getConfigManager().getMessagesConfig().getString("create.success", "&aGuilda {name} criada com sucesso!");
                    player.sendMessage(ColorUtils.replaceWithColorIsolation(template, "{name}", name));
                    
                    String nameMessage = plugin.getConfigManager().getMessagesConfig().getString("create.name-info", "&eNome da Guilda: {name}");
                    player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", name)));
                    if (tag != null) {
                        String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("create.tag-info", "&eTag da Guilda: [{tag}]");
                        player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", tag)));
                    }
                    if (description != null) {
                        String descMessage = plugin.getConfigManager().getMessagesConfig().getString("create.description-info", "&eDescrição da Guilda: {description}");
                        player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", description)));
                    }
                } else {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("create.failed", "&cFalha ao criar guilda! Possíveis razões:");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                    String reason1 = plugin.getConfigManager().getMessagesConfig().getString("create.failed-reason-1", "&c- Nome ou tag da guilda já existem");
                    String reason2 = plugin.getConfigManager().getMessagesConfig().getString("create.failed-reason-2", "&c- Você já está em outra guilda");
                    player.sendMessage(ColorUtils.colorize(reason1));
                    player.sendMessage(ColorUtils.colorize(reason2));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }
    private void handleInfo(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        int memberCount = guildService.getGuildMemberCount(guild.getId());
        String header = plugin.getConfigManager().getMessagesConfig().getString("info.title", "&6=== Informações da Guilda ===");
        player.sendMessage(ColorUtils.colorize(header));
        String nameMessage = plugin.getConfigManager().getMessagesConfig().getString("info.name", "&eNome: &f{name}");
        player.sendMessage(ColorUtils.colorize(nameMessage.replace("{name}", guild.getName())));
        if (guild.getTag() != null && !guild.getTag().isEmpty()) {
            String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("info.tag", "&eTag: &f{tag}");
            player.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
        }
        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            String descMessage = plugin.getConfigManager().getMessagesConfig().getString("info.description", "&eDescrição: &f{description}");
            player.sendMessage(ColorUtils.colorize(descMessage.replace("{description}", guild.getDescription())));
        }
        String leaderMessage = plugin.getConfigManager().getMessagesConfig().getString("info.leader", "&eLíder: &f{leader}");
        player.sendMessage(ColorUtils.colorize(leaderMessage.replace("{leader}", guild.getLeaderName())));
        String membersMessage = plugin.getConfigManager().getMessagesConfig().getString("info.members", "&eMembros: &f{count}/{max}");
        player.sendMessage(ColorUtils.colorize(membersMessage
            .replace("{count}", String.valueOf(memberCount))
            .replace("{max}", String.valueOf(guild.getMaxMembers()))));
        String roleMessage = plugin.getConfigManager().getMessagesConfig().getString("info.role", "&eSeu Cargo: &f{role}");
        player.sendMessage(ColorUtils.colorize(roleMessage.replace("{role}", member.getRole().getDisplayName())));
        java.time.format.DateTimeFormatter TF = com.guild.core.time.TimeProvider.FULL_FORMATTER;
        String createdMessage = plugin.getConfigManager().getMessagesConfig().getString("info.created", "&eCriado em: &f{date}");
        String createdFormatted = guild.getCreatedAt() != null ? guild.getCreatedAt().format(TF) : "Desconhecido";
        player.sendMessage(ColorUtils.colorize(createdMessage.replace("{date}", createdFormatted)));
    }
    private void handleMembers(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        List<GuildMember> members = guildService.getGuildMembers(guild.getId());
        if (members.isEmpty()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("members.no-members", "&cNenhum membro na guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String title = plugin.getConfigManager().getMessagesConfig().getString("members.title", "&6=== Membros da Guilda ===");
        player.sendMessage(ColorUtils.colorize(title));
        for (GuildMember member : members) {
            String status = "";
            Player onlinePlayer = Bukkit.getPlayer(member.getPlayerUuid());
            if (onlinePlayer != null) {
                status = "&a[Online]";
            } else {
                status = "&7[Offline]";
            }
            String memberFormat = plugin.getConfigManager().getMessagesConfig().getString("members.member-format", "&e{role} {name} &7- {status}");
            String memberMessage = memberFormat
                .replace("{role}", member.getRole().getDisplayName())
                .replace("{name}", member.getPlayerName())
                .replace("{status}", status);
            player.sendMessage(ColorUtils.colorize(memberMessage));
        }
        String totalMessage = plugin.getConfigManager().getMessagesConfig().getString("members.total", "&eTotal: {count} pessoas");
        player.sendMessage(ColorUtils.colorize(totalMessage.replace("{count}", String.valueOf(members.size()))));
    }
    private void handleInvite(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.invite")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.no-permission", "&cVocê não tem permissão para convidar jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.usage", "&eUso: /guild invite <Nome do Jogador>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String targetPlayerName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (!plugin.getPermissionManager().canInviteMembers(player)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.no-permission", "&cVocê não tem permissão para convidar jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (guildService.getPlayerGuild(targetPlayer.getUniqueId()) != null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.already-in-guild", "&cJogador {player} já está em outra guilda!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.cannot-invite-self", "&cVocê não pode convidar a si mesmo!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        guildService.sendInvitationAsync(guild.getId(), player.getUniqueId(), player.getName(), targetPlayer.getUniqueId(), targetPlayerName)
            .thenAcceptAsync(success -> {
                if (success) {
                    String sentMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.sent", "&aConvite de guilda enviado para {player}!");
                    player.sendMessage(ColorUtils.colorize(sentMessage.replace("{player}", targetPlayerName)));
                    String inviteTitle = plugin.getConfigManager().getMessagesConfig().getString("invite.title", "&6=== Convite de Guilda ===");
                    targetPlayer.sendMessage(ColorUtils.colorize(inviteTitle));
                    String inviteMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.received", "&e{inviter} convidou você para entrar na guilda: {guild}.");
                    targetPlayer.sendMessage(ColorUtils.colorize(inviteMessage
                        .replace("{inviter}", player.getName())
                        .replace("{guild}", guild.getName())));
                    if (guild.getTag() != null && !guild.getTag().isEmpty()) {
                        String tagMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.guild-tag", "&eTag da Guilda: [{tag}]");
                        targetPlayer.sendMessage(ColorUtils.colorize(tagMessage.replace("{tag}", guild.getTag())));
                    }
                    String acceptMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accept-command", "&eDigite &a/guild accept {inviter} &e para aceitar.");
                    targetPlayer.sendMessage(ColorUtils.colorize(acceptMessage.replace("{inviter}", player.getName())));
                    String declineMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.decline-command", "&eDigite &c/guild decline {inviter} &e para recusar.");
                    targetPlayer.sendMessage(ColorUtils.colorize(declineMessage.replace("{inviter}", player.getName())));
                } else {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.already-invited", "&c{player} já recebeu um convite!");
                    player.sendMessage(ColorUtils.colorize(failMessage.replace("{player}", targetPlayerName)));
                }
            }, runnable -> CompatibleScheduler.runTask(plugin, runnable));
    }
    private void handleKick(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.kick")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.no-permission", "&cVocê não tem permissão para expulsar jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.usage", "&eUso: /guild kick <Nome do Jogador>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String targetPlayerName = args[1];
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (!plugin.getPermissionManager().canKickMembers(player)) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.no-permission", "&cVocê não tem permissão para expulsar jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.not-in-guild", "&cJogador {player} não está na sua guilda!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.cannot-kick-self", "&cVocê não pode expulsar a si mesmo! Use /guild leave para sair da guilda.");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("kick.cannot-kick-leader", "&cVocê não pode expulsar o líder da guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        boolean success = guildService.removeGuildMember(targetPlayer.getUniqueId(), player.getUniqueId());
        if (success) {
            String successMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.success", "&a{player} foi expulso da guilda!");
            player.sendMessage(ColorUtils.colorize(successMessage.replace("{player}", targetPlayerName)));
            String kickedMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.kicked", "&cVocê foi expulso da guilda {guild}!");
            targetPlayer.sendMessage(ColorUtils.colorize(kickedMessage.replace("{guild}", guild.getName())));
        } else {
            String failMessage = plugin.getConfigManager().getMessagesConfig().getString("kick.failed", "&cFalha ao expulsar jogador!");
            player.sendMessage(ColorUtils.colorize(failMessage));
        }
    }
    private void handleLeave(Player player) {
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildMember member = guildService.getGuildMember(player.getUniqueId());
        if (member == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.member-error", "&cErro nas informações do membro da guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (member.getRole() == GuildMember.Role.LEADER) {
            String message1 = plugin.getConfigManager().getMessagesConfig().getString("leave.leader-cannot-leave", "&cO líder da guilda não pode sair da guilda!");
            String message2 = plugin.getConfigManager().getMessagesConfig().getString("leave.use-delete", "&cSe você deseja dissolver a guilda, use o comando /guild delete.");
            player.sendMessage(ColorUtils.colorize(message1));
            player.sendMessage(ColorUtils.colorize(message2));
            return;
        }
        boolean success = guildService.removeGuildMember(player.getUniqueId(), player.getUniqueId());
        if (success) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.success-with-guild", "&aVocê saiu da guilda: {guild}");
            player.sendMessage(ColorUtils.colorize(message.replace("{guild}", guild.getName())));
        } else {
            String message = plugin.getConfigManager().getMessagesConfig().getString("leave.failed", "&cFalha ao sair da guilda!");
            player.sendMessage(ColorUtils.colorize(message));
        }
    }
    private void handleDelete(Player player) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.delete")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("delete.no-permission", "&cVocê não tem permissão para deletar a guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String warningMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.warning", "&cAviso: Deletar a guilda irá dissolvê-la permanentemente e todos os membros serão removidos!");
        String confirmMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.confirm-command", "&cSe você tem certeza que deseja deletar a guilda, digite novamente: /guild delete confirm");
        String cancelMessage = plugin.getConfigManager().getMessagesConfig().getString("delete.cancel-command", "&cOu digite: /guild delete cancel para cancelar");
        player.sendMessage(ColorUtils.colorize(warningMessage));
        player.sendMessage(ColorUtils.colorize(confirmMessage));
        player.sendMessage(ColorUtils.colorize(cancelMessage));
    }
    private void handlePromote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.promote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.no-permission", "&cVocê não tem permissão para promover jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.usage", "&eUso: /guild promote <Jogador>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String targetPlayerName = args[1];
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.not-in-guild", "&cJogador {player} não está na sua guilda!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.cannot-promote-self", "&cVocê não pode promover a si mesmo!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;
        if (currentRole == GuildMember.Role.MEMBER) {
            newRole = GuildMember.Role.OFFICER;
        } else if (currentRole == GuildMember.Role.OFFICER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.already-highest", "&cJogador {player} já está no cargo mais alto!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (newRole != null) {
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.success", "&a{player} foi promovido para {role}!");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));
                String promotedMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.success", "&aVocê foi promovido para {role}!");
                targetPlayer.sendMessage(ColorUtils.colorize(promotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.promote.cannot-promote", "&cNão foi possível promover este jogador!");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }
    private void handleDemote(Player player, String[] args) {
        if (!plugin.getPermissionManager().hasPermission(player, "guild.demote")) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.no-permission", "&cVocê não tem permissão para rebaixar jogadores!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.usage", "&eUso: /guild demote <Jogador>");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String targetPlayerName = args[1];
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("info.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        GuildMember targetMember = guildService.getGuildMember(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.not-in-guild", "&cJogador {player} não está na sua guilda!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote-self", "&cVocê não pode rebaixar a si mesmo!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (targetMember.getRole() == GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote-leader", "&cNão é possível rebaixar o líder da guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole = null;
        if (currentRole == GuildMember.Role.OFFICER) {
            newRole = GuildMember.Role.MEMBER;
        } else if (currentRole == GuildMember.Role.MEMBER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.already-lowest", "&cJogador {player} já está no cargo mais baixo!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", targetPlayerName)));
            return;
        }
        if (newRole != null) {
            boolean success = guildService.updateMemberRole(targetPlayer.getUniqueId(), newRole, player.getUniqueId());
            if (success) {
                String successMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.success", "&a{player} foi rebaixado para {role}!");
                player.sendMessage(ColorUtils.colorize(successMessage
                    .replace("{player}", targetPlayerName)
                    .replace("{role}", newRole.getDisplayName())));
                String demotedMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.success", "&aVocê foi rebaixado para {role}!");
                targetPlayer.sendMessage(ColorUtils.colorize(demotedMessage.replace("{role}", newRole.getDisplayName())));
            } else {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("permissions.demote.cannot-demote", "&cNão foi possível rebaixar este jogador!");
                player.sendMessage(ColorUtils.colorize(failMessage));
            }
        }
    }
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.accept-command", "&eDigite &a/guild accept {inviter} &e para aceitar o convite");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Busca o convite para obter informações da guilda
        guildService.getPendingInvitationAsync(player.getUniqueId(), inviter.getUniqueId()).thenAccept(invitation -> {
            if (invitation == null) {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                player.sendMessage(ColorUtils.colorize(failMessage));
                return;
            }
            
            // Busca a guilda para obter o nome
            guildService.getGuildByIdAsync(invitation.getGuildId()).thenAccept(guild -> {
                if (guild == null) {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                    return;
                }
                
                boolean success = guildService.processInvitation(player.getUniqueId(), inviter.getUniqueId(), true);
                if (success) {
                    // Mensagem para quem aceitou
                    String successMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accepted", "&aVocê aceitou o convite de {guild}!");
                    player.sendMessage(ColorUtils.colorize(successMessage.replace("{guild}", guild.getName())));
                    
                    // Mensagem para quem convidou
                    String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.accepted-inviter", "&a{player} aceitou seu convite para entrar em {guild}");
                    inviter.sendMessage(ColorUtils.colorize(inviterMessage
                        .replace("{player}", player.getName())
                        .replace("{guild}", guild.getName())));
                } else {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                }
            });
        });
    }
    private void handleDecline(Player player, String[] args) {
        if (args.length < 2) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("invite.decline-command", "&eDigite &c/guild decline {inviter} &e para recusar o convite");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        String inviterName = args[1];
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.player-not-found", "&cJogador {player} não está online!");
            player.sendMessage(ColorUtils.colorize(message.replace("{player}", inviterName)));
            return;
        }
        GuildService guildService = plugin.getServiceContainer().get(GuildService.class);
        if (guildService == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("general.service-error", "&cServiço de guilda não inicializado!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        
        // Busca o convite para obter informações da guilda
        guildService.getPendingInvitationAsync(player.getUniqueId(), inviter.getUniqueId()).thenAccept(invitation -> {
            if (invitation == null) {
                String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                player.sendMessage(ColorUtils.colorize(failMessage));
                return;
            }
            
            // Busca a guilda para obter o nome
            guildService.getGuildByIdAsync(invitation.getGuildId()).thenAccept(guild -> {
                if (guild == null) {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                    return;
                }
                
                boolean success = guildService.processInvitation(player.getUniqueId(), inviter.getUniqueId(), false);
                if (success) {
                    // Mensagem para quem recusou
                    String successMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.declined", "&cVocê recusou o convite de {guild}!");
                    player.sendMessage(ColorUtils.colorize(successMessage.replace("{guild}", guild.getName())));
                    
                    // Mensagem para quem convidou
                    String inviterMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.declined-inviter", "&c{player} recusou seu convite para entrar em {guild}");
                    inviter.sendMessage(ColorUtils.colorize(inviterMessage
                        .replace("{player}", player.getName())
                        .replace("{guild}", guild.getName())));
                } else {
                    String failMessage = plugin.getConfigManager().getMessagesConfig().getString("invite.expired", "&cO convite da guilda expirou!");
                    player.sendMessage(ColorUtils.colorize(failMessage));
                }
            });
        });
    }
    private void handleHelp(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("help.title", "&6=== Ajuda do Sistema de Guildas ===");
        player.sendMessage(ColorUtils.colorize(title));
        String mainMenu = plugin.getConfigManager().getMessagesConfig().getString("help.main-menu", "&e/guild &7- Abrir menu principal da guilda");
        player.sendMessage(ColorUtils.colorize(mainMenu));
        String create = plugin.getConfigManager().getMessagesConfig().getString("help.create", "&e/guild create <Nome> [Tag] [Descrição] &7- Criar guilda");
        player.sendMessage(ColorUtils.colorize(create));
        String info = plugin.getConfigManager().getMessagesConfig().getString("help.info", "&e/guild info &7- Ver informações da guilda");
        player.sendMessage(ColorUtils.colorize(info));
        String members = plugin.getConfigManager().getMessagesConfig().getString("help.members", "&e/guild members &7- Ver membros da guilda");
        player.sendMessage(ColorUtils.colorize(members));
        String invite = plugin.getConfigManager().getMessagesConfig().getString("help.invite", "&e/guild invite <Jogador> &7- Convidar jogador para a guilda");
        player.sendMessage(ColorUtils.colorize(invite));
        String kick = plugin.getConfigManager().getMessagesConfig().getString("help.kick", "&e/guild kick <Jogador> &7- Expulsar membro da guilda");
        player.sendMessage(ColorUtils.colorize(kick));
        String promote = plugin.getConfigManager().getMessagesConfig().getString("help.promote", "&e/guild promote <Jogador> &7- Promover membro da guilda");
        player.sendMessage(ColorUtils.colorize(promote));
        String demote = plugin.getConfigManager().getMessagesConfig().getString("help.demote", "&e/guild demote <Jogador> &7- Rebaixar membro da guilda");
        player.sendMessage(ColorUtils.colorize(demote));
        String accept = plugin.getConfigManager().getMessagesConfig().getString("help.accept", "&e/guild accept <Convidado> &7- Aceitar convite da guilda");
        player.sendMessage(ColorUtils.colorize(accept));
        String decline = plugin.getConfigManager().getMessagesConfig().getString("help.decline", "&e/guild decline <Convidado> &7- Recusar convite da guilda");
        player.sendMessage(ColorUtils.colorize(decline));
        String leave = plugin.getConfigManager().getMessagesConfig().getString("help.leave", "&e/guild leave &7- Sair da guilda");
        player.sendMessage(ColorUtils.colorize(leave));
        String delete = plugin.getConfigManager().getMessagesConfig().getString("help.delete", "&e/guild delete &7- Deletar guilda");
        player.sendMessage(ColorUtils.colorize(delete));
        String help = plugin.getConfigManager().getMessagesConfig().getString("help.help", "&e/guild help &7- Mostrar esta mensagem de ajuda");
        player.sendMessage(ColorUtils.colorize(help));
        String relation = "&e/guild relation &7- Gerenciar relações da guilda";
        player.sendMessage(ColorUtils.colorize(relation));
        String economy = "&e/guild economy &7- Gerenciar economia da guilda";
        player.sendMessage(ColorUtils.colorize(economy));
        String deposit = "&e/guild deposit <Quantia> &7- Depositar fundos na guilda";
        player.sendMessage(ColorUtils.colorize(deposit));
        String withdraw = "&e/guild withdraw <Quantia> &7- Sacar fundos da guilda";
        player.sendMessage(ColorUtils.colorize(withdraw));
        String transfer = "&e/guild transfer <Guilda> <Quantia> &7- Transferir fundos para outra guilda";
        player.sendMessage(ColorUtils.colorize(transfer));
        String logs = "&e/guild logs &7- Ver logs de operação da guilda";
        player.sendMessage(ColorUtils.colorize(logs));
    }
    private void handleRelation(Player player, String[] args) {
        Guild guild = plugin.getGuildService().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        GuildMember member = plugin.getGuildService().getGuildMember(player.getUniqueId());
        if (member == null || member.getRole() != GuildMember.Role.LEADER) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.only-leader", "&cApenas o líder da guilda pode gerenciar relações!");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        if (args.length == 1) {
            showRelationHelp(player);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list":
                handleRelationList(player, guild);
                break;
            case "create":
                if (args.length < 4) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-usage", "&eUso: /guild relation create <Guilda Alvo> <Tipo de Relação>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationCreate(player, guild, args[2], args[3]);
                break;
            case "delete":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-usage", "&eUso: /guild relation delete <Guilda Alvo>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationDelete(player, guild, args[2]);
                break;
            case "accept":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-usage", "&eUso: /guild relation accept <Guilda Alvo>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationAccept(player, guild, args[2]);
                break;
            case "reject":
                if (args.length < 3) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-usage", "&eUso: /guild relation reject <Guilda Alvo>");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                handleRelationReject(player, guild, args[2]);
                break;
            default:
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.unknown-subcommand", "&cSubcomando desconhecido! Use /guild relation para ver a ajuda.");
                player.sendMessage(ColorUtils.colorize(message));
                break;
        }
    }
    private void showRelationHelp(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("relation.help-title", "&6=== Gerenciamento de Relações da Guilda ===");
        player.sendMessage(ColorUtils.colorize(title));
        String list = plugin.getConfigManager().getMessagesConfig().getString("relation.help-list", "&e/guild relation list &7- Ver todas as relações");
        player.sendMessage(ColorUtils.colorize(list));
        String create = plugin.getConfigManager().getMessagesConfig().getString("relation.help-create", "&e/guild relation create <Guilda> <Tipo> &7- Criar relação");
        player.sendMessage(ColorUtils.colorize(create));
        String delete = plugin.getConfigManager().getMessagesConfig().getString("relation.help-delete", "&e/guild relation delete <Guilda> &7- Deletar relação");
        player.sendMessage(ColorUtils.colorize(delete));
        String accept = plugin.getConfigManager().getMessagesConfig().getString("relation.help-accept", "&e/guild relation accept <Guilda> &7- Aceitar pedido de relação");
        player.sendMessage(ColorUtils.colorize(accept));
        String reject = plugin.getConfigManager().getMessagesConfig().getString("relation.help-reject", "&e/guild relation reject <Guilda> &7- Recusar pedido de relação");
        player.sendMessage(ColorUtils.colorize(reject));
        String types = plugin.getConfigManager().getMessagesConfig().getString("relation.help-types", "&7Tipos de Relação: &eally(Aliado), enemy(Inimigo), war(Guerra), truce(Trégua), neutral(Neutro)");
        player.sendMessage(ColorUtils.colorize(types));
    }
    private void handleRelationList(Player player, Guild guild) {
        plugin.getGuildService().getGuildRelationsAsync(guild.getId()).thenAccept(relations -> {
            if (relations == null || relations.isEmpty()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.no-relations", "&7Sua guilda ainda não tem relações.");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            String title = plugin.getConfigManager().getMessagesConfig().getString("relation.list-title", "&6=== Lista de Relações da Guilda ===");
            player.sendMessage(ColorUtils.colorize(title));
            for (GuildRelation relation : relations) {
                String otherGuildName = relation.getOtherGuildName(guild.getId());
                String status = relation.getStatus().name();
                String type = relation.getType().name();
                String relationInfo = plugin.getConfigManager().getMessagesConfig().getString("relation.list-format", "&e{other_guild} &7- {type} ({status})")
                    .replace("{other_guild}", otherGuildName)
                    .replace("{type}", type)
                    .replace("{status}", status);
                player.sendMessage(ColorUtils.colorize(relationInfo));
            }
        });
    }
    private void handleRelationCreate(Player player, Guild guild, String targetGuildName, String relationTypeStr) {
        GuildRelation.RelationType relationType;
        try {
            relationType = GuildRelation.RelationType.valueOf(relationTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            String message = plugin.getConfigManager().getMessagesConfig().getString("relation.invalid-type", "&cTipo de relação inválido! Tipos válidos: ally, enemy, war, truce, neutral");
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGuilda alvo {guild} não existe!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            if (targetGuild.getId() == guild.getId()) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.cannot-relation-self", "&cNão é possível criar relação consigo mesmo!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            plugin.getGuildService().createGuildRelationAsync(guild.getId(), targetGuild.getId(), guild.getName(), targetGuild.getName(), relationType, player.getUniqueId(), player.getName())
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-success", "&aPedido de relação {type} enviado para {guild}!")
                            .replace("{guild}", targetGuildName)
                            .replace("{type}", relationType.name());
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.create-failed", "&cFalha ao criar relação! Talvez a relação já exista.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    private void handleRelationDelete(Player player, Guild guild, String targetGuildName) {
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGuilda alvo {guild} não existe!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().deleteGuildRelationAsync(relation.getId());
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-success", "&aRelação com {guild} deletada!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.delete-failed", "&cFalha ao deletar relação! Talvez a relação não exista.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    private void handleRelationAccept(Player player, Guild guild, String targetGuildName) {
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGuilda alvo {guild} não existe!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.ACTIVE);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-success", "&aPedido de relação de {guild} aceito!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.accept-failed", "&cFalha ao aceitar relação! Talvez não haja pedido pendente.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    private void handleRelationReject(Player player, Guild guild, String targetGuildName) {
        plugin.getGuildService().getGuildByNameAsync(targetGuildName).thenAccept(targetGuild -> {
            if (targetGuild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("relation.target-not-found", "&cGuilda alvo {guild} não existe!")
                    .replace("{guild}", targetGuildName);
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            plugin.getGuildService().getGuildRelationAsync(guild.getId(), targetGuild.getId())
                .thenCompose(relation -> {
                    if (relation == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getGuildService().updateGuildRelationStatusAsync(relation.getId(), GuildRelation.RelationStatus.CANCELLED);
                })
                .thenAccept(success -> {
                    if (success) {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-success", "&cPedido de relação de {guild} recusado!")
                            .replace("{guild}", targetGuildName);
                        player.sendMessage(ColorUtils.colorize(message));
                    } else {
                        String message = plugin.getConfigManager().getMessagesConfig().getString("relation.reject-failed", "&cFalha ao recusar relação! Talvez não haja pedido pendente.");
                        player.sendMessage(ColorUtils.colorize(message));
                    }
                });
        });
    }
    private void handleLogs(Player player, String[] args) {
        plugin.getGuildService().getPlayerGuildAsync(player.getUniqueId()).thenAccept(guild -> {
            if (guild == null) {
                String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-guild", "&cVocê ainda não entrou em nenhuma guilda!");
                player.sendMessage(ColorUtils.colorize(message));
                return;
            }
            plugin.getGuildService().getGuildMemberAsync(guild.getId(), player.getUniqueId()).thenAccept(member -> {
                if (member == null) {
                    String message = plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cPermissão insuficiente!");
                    player.sendMessage(ColorUtils.colorize(message));
                    return;
                }
                plugin.getGuiManager().openGUI(player, new com.guild.gui.GuildLogsGUI(plugin, guild, player));
            });
        });
    }
    private void handlePlaceholder(Player player, String[] args) {
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessagesConfig().getString("general.no-permission", "&cVocê não tem permissão para realizar esta operação!")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cUso: /guild placeholder <Nome da Variável>"));
            player.sendMessage(ColorUtils.colorize("&eExemplo: /guild placeholder name"));
            player.sendMessage(ColorUtils.colorize("&eVariáveis disponíveis: name, tag, description, leader, membercount, role, hasguild, isleader, isofficer"));
            return;
        }
        String placeholder = "%guild_" + args[1] + "%";
        String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
        player.sendMessage(ColorUtils.colorize("&6=== Teste do PlaceholderAPI ==="));
        player.sendMessage(ColorUtils.colorize("&eVariável: &f" + placeholder));
        player.sendMessage(ColorUtils.colorize("&eResultado: &f" + result));
        player.sendMessage(ColorUtils.colorize("&6========================"));
    }
    private void handleTime(Player player) {
        String title = plugin.getConfigManager().getMessagesConfig().getString("time.title", "&6=== Teste de Tempo ===");
        String realNow = com.guild.core.time.TimeProvider.nowString();
        long ticks = player.getWorld().getTime() % 24000L;
        int hours = (int)((ticks / 1000L + 6) % 24); 
        int minutes = (int)((ticks % 1000L) * 60L / 1000L);
        String gameTime = String.format("%02d:%02d", hours, minutes);
        String ticksStr = String.valueOf(ticks);
        player.sendMessage(ColorUtils.colorize(title));
        player.sendMessage(ColorUtils.colorize("&eTempo Real: &f" + realNow));
        player.sendMessage(ColorUtils.colorize("&eTempo de Jogo: &f" + gameTime + " &7(" + ticksStr + " ticks)"));
    }
}