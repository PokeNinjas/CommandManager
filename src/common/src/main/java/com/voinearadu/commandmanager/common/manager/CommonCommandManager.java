package com.voinearadu.commandmanager.common.manager;

import com.voinearadu.commandmanager.common.annotation.Command;
import com.voinearadu.commandmanager.common.command.CommonCommand;
import com.voinearadu.commandmanager.common.exception.CommandNotAnnotated;
import com.voinearadu.commandmanager.common.utils.CommonPermissionUtils;
import com.voinearadu.logger.Logger;
import com.voinearadu.reflections.Reflections;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public abstract class CommonCommandManager {

    private final @Getter Reflections reflections;
    private final List<CommonCommand> commands = new ArrayList<>();
    private final @Getter Class<?> playerClass;
    private final @Getter Class<?> consoleClass;
    private final @Getter Class<?> senderClass;
    private final @Getter String basePermission;

    public CommonCommandManager(@NotNull Reflections reflections, @NotNull Class<?> playerClass,
                                @NotNull Class<?> consoleClass, @NotNull Class<?> senderClass,
                                @NotNull String basePermission) {
        this.reflections = reflections;
        this.playerClass = playerClass;
        this.consoleClass = consoleClass;
        this.senderClass = senderClass;
        this.basePermission = basePermission;
    }

    public void register(@NotNull Class<? extends CommonCommand> commandClass) {
        try {
            Command commandAnnotation = commandClass.getAnnotation(Command.class);
            if (commandAnnotation != null && commandAnnotation.parent() != CommonCommand.class) {
                return;
            }

            if (Modifier.isAbstract(commandClass.getModifiers())) {
                return;
            }

            CommonCommand command = commandClass.getConstructor(CommonCommandManager.class).newInstance(this);
            register(command);
        } catch (Throwable error) {
            // Do not print NotAnnotated error as errors, but was warnings. There are legitimate uses of it not being
            // annotated, but it might also be an oversight that needs to be addressed.
            if (error instanceof CommandNotAnnotated) { //NOPMD - suppressed AvoidInstanceofChecksInCatchClause
                Logger.warn(error.getMessage());
                return;
            }

            if (error instanceof InvocationTargetException invocationError) { //NOPMD - suppressed AvoidInstanceofChecksInCatchClause
                if (invocationError.getTargetException() instanceof CommandNotAnnotated) {
                    Logger.warn(invocationError.getTargetException().getMessage());
                    return;
                }
            }

            Logger.error(error);
            Logger.error("There was an error while registering command " + commandClass.getName());
        }
    }

    private void register(@NotNull CommonCommand command) {
        Logger.log("Registering command(s) " + command.getAliases());
        commands.add(command);
        platformRegister(command);
    }


    public void enable(@NotNull Class<? extends CommonCommand> commandClass) {
        Command commandAnnotation = commandClass.getAnnotation(Command.class);
        if (commandAnnotation != null && commandAnnotation.parent() != CommonCommand.class) {
            return;
        }

        CommonCommand command = getCommand(commandClass);

        if (command != null) {
            command.enable();
        } else {
            Logger.error("Command " + commandClass.getName() + " is not registered.");
        }
    }

    public void disable(@NotNull Class<? extends CommonCommand> commandClass) {
        Command commandAnnotation = commandClass.getAnnotation(Command.class);
        if (commandAnnotation != null && commandAnnotation.parent() != CommonCommand.class) {
            return;
        }

        CommonCommand command = getCommand(commandClass);

        if (command != null) {
            command.disable();
        } else {
            Logger.error("Command " + commandClass.getName() + " is not registered.");
        }
    }

    public @Nullable CommonCommand getCommand(@NotNull Class<? extends CommonCommand> commandClass) {
        CommonCommand rootCommand = commands.stream()
                .filter(commandHolder -> commandHolder.getClass().equals(commandClass))
                .findFirst()
                .orElse(null);

        if (rootCommand != null) {
            return rootCommand;
        }

        return commands.stream()
                .filter(commandHolder -> commandHolder.getPrimitiveSubCommands().stream()
                        .anyMatch(subCommand -> subCommand.getClass().equals(commandClass)))
                .findFirst()
                .orElse(null);
    }

    protected abstract void platformRegister(@NotNull CommonCommand command);

    public boolean checkPermission(Object target, String permission) {
        if (consoleClass.isInstance(target)) {
            return true;
        }
        if (playerClass.isInstance(target)) {
            return CommonPermissionUtils.checkPermission(playerClass, playerClass.cast(target), permission);
        }
        return CommonPermissionUtils.checkPermission(playerClass, senderClass.cast(target), permission);
    }

    public abstract void sendMessage(Object target, String message);

    public abstract void broadcastMessage(String message);

}
