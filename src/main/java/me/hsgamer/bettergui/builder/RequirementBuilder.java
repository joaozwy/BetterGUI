package me.hsgamer.bettergui.builder;

import static me.hsgamer.bettergui.BetterGUI.getInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.hsgamer.bettergui.object.Requirement;
import me.hsgamer.bettergui.object.requirement.ConditionRequirement;
import me.hsgamer.bettergui.object.requirement.CooldownRequirement;
import me.hsgamer.bettergui.object.requirement.ExpLevelRequirement;
import me.hsgamer.bettergui.object.requirement.PermissionRequirement;
import me.hsgamer.bettergui.object.requirementset.RequirementSet;
import me.hsgamer.bettergui.object.variable.LocalVariableManager;
import me.hsgamer.bettergui.util.CaseInsensitiveStringMap;
import me.hsgamer.bettergui.util.CommonUtils;
import org.bukkit.configuration.ConfigurationSection;

public final class RequirementBuilder {

  private static final Map<String, Class<? extends Requirement<?, ?>>> requirementsClass = new CaseInsensitiveStringMap<>();

  static {
    register("condition", ConditionRequirement.class);
    register("level", ExpLevelRequirement.class);
    register("permission", PermissionRequirement.class);
    register("cooldown", CooldownRequirement.class);
  }

  private RequirementBuilder() {

  }

  /**
   * Register new requirement type
   *
   * @param type  the name of the type
   * @param clazz the class
   */
  public static void register(String type, Class<? extends Requirement<?, ?>> clazz) {
    if (type.toLowerCase().startsWith("not-")) {
      getInstance().getLogger()
          .warning(() -> "Invalid requirement type '" + type
              + "': Should not start with 'not-'. Ignored...");
      return;
    }
    requirementsClass.put(type, clazz);
  }

  public static Optional<Requirement<?, ?>> getRequirement(String type,
      LocalVariableManager<?> localVariableManager) {
    // Check Inverted mode
    boolean inverted = false;
    if (type.toLowerCase().startsWith("not-")) {
      type = type.substring(4);
      inverted = true;
    }

    if (requirementsClass.containsKey(type)) {
      Class<? extends Requirement<?, ?>> clazz = requirementsClass.get(type);
      try {
        Requirement<?, ?> requirement = clazz.getDeclaredConstructor().newInstance();
        requirement.setVariableManager(localVariableManager);
        requirement.setInverted(inverted);
        return Optional.of(requirement);
      } catch (Exception e) {
        // IGNORED
      }
    }
    return Optional.empty();
  }

  public static List<Requirement<?, ?>> loadRequirementsFromSection(ConfigurationSection section,
      LocalVariableManager<?> localVariableManager) {
    List<Requirement<?, ?>> requirements = new ArrayList<>();
    section.getKeys(false).forEach(type -> {
      Optional<Requirement<?, ?>> rawRequirement = getRequirement(type, localVariableManager);
      if (!rawRequirement.isPresent()) {
        return;
      }
      Requirement<?, ?> requirement = rawRequirement.get();
      if (section.isConfigurationSection(type)) {
        Map<String, Object> keys = new CaseInsensitiveStringMap<>(
            section.getConfigurationSection(type).getValues(false));
        if (keys.containsKey(Settings.VALUE)) {
          requirement.setValue(keys.get(Settings.VALUE));
          if (keys.containsKey(Settings.TAKE)) {
            requirement.canTake((Boolean) keys.get(Settings.TAKE));
          }
        } else {
          getInstance().getLogger().warning(
              "The requirement \"" + type + "\" doesn't have VALUE");
        }
      } else {
        requirement.setValue(section.get(type));
      }
      requirements.add(requirement);
    });

    return requirements;
  }

  public static List<RequirementSet> getRequirementSet(ConfigurationSection section,
      LocalVariableManager<?> localVariableManager) {
    List<RequirementSet> list = new ArrayList<>();
    section.getKeys(false).forEach(key -> {
      if (section.isConfigurationSection(key)) {
        ConfigurationSection subsection = section.getConfigurationSection(key);
        List<Requirement<?, ?>> requirements = loadRequirementsFromSection(subsection,
            localVariableManager);
        if (!requirements.isEmpty()) {
          RequirementSet requirementSet = new RequirementSet(key, requirements);
          Map<String, Object> keys = new CaseInsensitiveStringMap<>(subsection.getValues(false));
          if (keys.containsKey(Settings.SUCCESS_COMMAND)) {
            requirementSet.setSuccessCommands(CommandBuilder.getCommands(localVariableManager,
                CommonUtils.createStringListFromObject(keys.get(Settings.SUCCESS_COMMAND), true)));
          }
          if (keys.containsKey(Settings.FAIL_COMMAND)) {
            requirementSet.setFailCommands(CommandBuilder.getCommands(localVariableManager,
                CommonUtils.createStringListFromObject(keys.get(Settings.FAIL_COMMAND), true)));
          }
          list.add(requirementSet);
        }
      }
    });
    return list;
  }

  private static class Settings {

    // Requirement settings
    static final String VALUE = "value";
    static final String TAKE = "take";

    // Set settings
    static final String SUCCESS_COMMAND = "success-command";
    static final String FAIL_COMMAND = "fail-command";
  }
}
