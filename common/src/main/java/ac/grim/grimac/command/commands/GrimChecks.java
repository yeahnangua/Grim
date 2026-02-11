package ac.grim.grimac.command.commands;

import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class GrimChecks implements BuildableCommand {

    private static Consumer<Sender> guiOpener;

    public static void setGuiOpener(Consumer<Sender> opener) {
        guiOpener = opener;
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("checks")
                        .permission("grim.checks")
                        .handler(this::handleChecks)
                        .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    private void handleChecks(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (guiOpener != null) {
            guiOpener.accept(sender);
        } else {
            sender.sendMessage("Check category GUI is not available on this platform.");
        }
    }
}
