package top.craft_hello.tpa.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.craft_hello.tpa.Messages;
import top.craft_hello.tpa.Request;

public class Back implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(sender instanceof Player)) {
            Messages.consoleUseError(sender);
            return true;
        }
        if (!sender.hasPermission("tpa.back")){
            Messages.notPermission(sender);
            return true;
        }
        Request request = new Request(sender, s, strings);
        request.back(false);
        return true;
    }
}
