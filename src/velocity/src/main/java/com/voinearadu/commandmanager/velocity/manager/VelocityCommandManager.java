package com.voinearadu.commandmanager.velocity.manager;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.voinearadu.commandmanager.common.command.CommonCommand;
import com.voinearadu.commandmanager.common.manager.CommonCommandManager;
import com.voinearadu.commandmanager.velocity.command.VelocityCommand;
import com.voinearadu.reflections.Reflections;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class VelocityCommandManager extends CommonCommandManager {

    private final ProxyServer proxy;
    private final VelocityMiniMessageManager miniMessageManager;

    public VelocityCommandManager(@NotNull Reflections reflections, @NotNull String basePermission,
                                  @NotNull ProxyServer proxy) {
        super(reflections, Player.class, ConsoleCommandSource.class, CommandSource.class, basePermission);

        this.proxy = proxy;
        this.miniMessageManager = new VelocityMiniMessageManager();
    }

    @Override
    protected void platformRegister(@NotNull CommonCommand primitiveCommand) {
        VelocityCommand command = (VelocityCommand) primitiveCommand;

        com.velocitypowered.api.command.CommandManager commandManager = proxy.getCommandManager();

        CommandMeta commandMeta = commandManager.metaBuilder(command.getMainAlias())
                .aliases(command.getAliases().subList(1, command.getAliases().size()).toArray(new String[0]))
                .plugin(this)
                .build();


        commandManager.register(commandMeta, command);
    }

    @Override
    public final void sendMessage(Object user, String message) {
        CommandSource source = (CommandSource) user;
        source.sendMessage(miniMessageManager.parse(message));
    }

    @Override
    public void broadcastMessage(String message) {
        proxy.sendMessage(miniMessageManager.parse(message));
    }

}
