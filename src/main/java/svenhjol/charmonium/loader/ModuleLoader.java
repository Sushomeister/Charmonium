package svenhjol.charmonium.loader;

import svenhjol.charmonium.helper.ClassHelper;
import svenhjol.charmonium.helper.DebugHelper;
import svenhjol.charmonium.helper.LogHelper;
import svenhjol.charmonium.helper.StringHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 4.0.0-charmonium
 */
@SuppressWarnings("unused")
public abstract class ModuleLoader<T extends CharmModule> {
    private static final List<String> MOD_IDS = new ArrayList<>();
    private final Map<Class<? extends T>, Boolean> ENABLED_CLAZZ_CACHE = new HashMap<>();
    private final Map<String, T> MODULES = new TreeMap<>();
    private final String modId;
    private final String basePackage;

    public ModuleLoader(String modId, String basePackage) {
        this.modId = modId;
        this.basePackage = basePackage;

        MOD_IDS.add(modId);
    }

    public void init() {
        // do module registration
        this.register();

        // do module dependency checks
        this.dependencies();

        // do module tasks
        this.run();
    }

    protected void register() {
        prepareModules().forEach((id, module) -> {
            LogHelper.debug(ModuleLoader.class, "Registering " + module.getName());
            MODULES.put(StringHelper.upperCamelToSnake(module.getName()), module);
            module.register();
        });
    }

    protected void dependencies() {
        getModules().forEach(module -> {
            boolean debug = DebugHelper.isDebugMode();
            boolean enabledInConfig = module.isEnabledInConfig();
            boolean passedDependencyCheck = module.getDependencies().isEmpty() || module.getDependencies().stream().allMatch(dep -> dep.test(module));
            module.setEnabled(enabledInConfig && passedDependencyCheck);

            if (!enabledInConfig) {
                if (debug) LogHelper.warn(ModuleLoader.class, "Disabled in configuration: " + module.getName());
            } else if (!passedDependencyCheck) {
                if (debug) LogHelper.warn(ModuleLoader.class, "Failed dependency check: " + module.getName());
            } else if (!module.isEnabled()) {
                if (debug) LogHelper.warn(ModuleLoader.class, "Disabled automatically: " + module.getName());
            } else {
                LogHelper.info(ModuleLoader.class, "Enabled " + module.getName());
            }
        });
    }

    protected void run() {
        getEnabledModules().forEach(module -> {
            LogHelper.info(ModuleLoader.class, "Running " + module.getName());
            module.runWhenEnabled();
        });

        getDisabledModules().forEach(module -> {
            LogHelper.debug(ModuleLoader.class, "Running disabled tasks: " + module.getName());
            module.runWhenDisabled();
        });
    }

    /**
     * Use this anywhere to check a module's enabled status for any Charm-based module.
     */
    public boolean isEnabled(Class<? extends T> clazz) {
        if (!ENABLED_CLAZZ_CACHE.containsKey(clazz)) {
            boolean enabled = getModules().stream().anyMatch(module -> module.getClass().equals(clazz) && module.isEnabled());
            ENABLED_CLAZZ_CACHE.put(clazz, enabled);
        }
        return ENABLED_CLAZZ_CACHE.get(clazz);
    }

    public List<T> getModules() {
        return MODULES.values().stream().toList();
    }

    public List<T> getEnabledModules() {
        return MODULES.values().stream().filter(CharmModule::isEnabled).collect(Collectors.toList());
    }

    public List<T> getDisabledModules() {
        return MODULES.values().stream().filter(m -> !m.isEnabled()).collect(Collectors.toList());
    }

    @Nullable
    public T getModule(String moduleName) {
        String name;

        // if fully qualified name, split
        if (moduleName.contains(".")) {
            String[] split = moduleName.split("\\.");
            name = split[split.length - 1];
        } else {
            name = moduleName;
        }

        String lower = StringHelper.upperCamelToSnake(name);
        return MODULES.getOrDefault(lower, null);
    }

    protected Map<String, T> prepareModules() {
        Map<String, T> discoveredModules = new LinkedHashMap<>();
        List<Class<T>> discoveredClasses = ClassHelper.getClassesInPackage(getBasePackage(), getModuleAnnotation());

        if (discoveredClasses.isEmpty())
            LogHelper.warn(ModuleLoader.class, "Seems no module classes were processed... this is probably bad.");

        Map<String, T> loaded = new TreeMap<>();
        for (Class<T> clazz : discoveredClasses) {
            try {
                T module = clazz.getDeclaredConstructor().newInstance();
                setupModuleAnnotations(clazz, module);

                String moduleName = module.getName();
                loaded.put(moduleName, module);
            } catch (Exception e) {
                LogHelper.error(ModuleLoader.class, "Error loading module " + clazz.toString() + ": " + e.getMessage());
            }
        }

        // defer module config to subclasses
        setupModuleConfig(new LinkedList<>(loaded.values()));

        // sort by module priority
        ArrayList<T> modList = new ArrayList<>(loaded.values());
        modList.sort((mod1, mod2) -> {
            if (mod1.getPriority() == mod2.getPriority()) {
                return mod1.getName().compareTo(mod2.getName()); // sort by name
            } else {
                return Integer.compare(mod2.getPriority(), mod1.getPriority()); // sort by priority
            }
        });

        // assemble loaded modules into discoveredModules and return
        for (T mod : modList) {
            for (Map.Entry<String, T> entry : loaded.entrySet()) {
                if (entry.getValue().equals(mod)) {
                    discoveredModules.put(entry.getKey(), mod);
                    break;
                }
            }
        }

        return discoveredModules;
    }

    protected String getModId() {
        return this.modId;
    }

    protected String getBasePackage() {
        return this.basePackage;
    }

    protected abstract String getModuleAnnotation();

    protected abstract void setupModuleAnnotations(Class<T> clazz, T module) throws IllegalStateException;

    protected void setupModuleConfig(List<T> modules) {
        // no op
    }

    public static List<String> getModIds() {
        return MOD_IDS;
    }
}
