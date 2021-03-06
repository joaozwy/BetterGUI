package me.hsgamer.bettergui.object.command;

import static me.hsgamer.bettergui.BetterGUI.getInstance;

import co.aikar.taskchain.TaskChain;
import java.util.Optional;
import me.hsgamer.bettergui.Permissions;
import me.hsgamer.bettergui.object.Command;
import me.hsgamer.bettergui.object.Icon;
import me.hsgamer.bettergui.object.Menu;
import org.bukkit.entity.Player;

public class BackCommand extends Command {

  public BackCommand(String command) {
    super(command);
  }

  @Override
  public void addToTaskChain(Player player, TaskChain<?> taskChain) {
    Object object = getVariableManager().getParent();
    Runnable runnable = player::closeInventory;

    Optional<Menu<?>> parentMenu = Optional.empty();
    if (object instanceof Icon) {
      parentMenu = ((Icon) object).getMenu().getParentMenu(player);
    } else if (object instanceof Menu) {
      parentMenu = ((Menu<?>) object).getParentMenu(player);
    }

    if (parentMenu.isPresent()) {
      Menu<?> finalParentMenu = parentMenu.get();
      runnable = () -> finalParentMenu.createInventory(player, new String[0],
          player.hasPermission(Permissions.OPEN_MENU_BYPASS));
    }

    Runnable finalRunnable = runnable;
    taskChain.sync(() -> getInstance().getServer().getScheduler().scheduleSyncDelayedTask(
        getInstance(), finalRunnable));
  }
}
