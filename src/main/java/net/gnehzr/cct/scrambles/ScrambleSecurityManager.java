package net.gnehzr.cct.scrambles;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;

import java.security.Permission;

public class ScrambleSecurityManager extends SecurityManager {
    private ClassLoader pluginLoader;
    private boolean enabled = true;
    private final Configuration configuration;

    public ScrambleSecurityManager(ClassLoader pluginLoader, Configuration configuration) {
        this.pluginLoader = pluginLoader;
        this.configuration = configuration;
    }

    public void configurationChanged() {
        enabled = configuration.getBoolean(VariableKey.SCRAMBLE_PLUGINS_SECURE, false);
    }

    public void checkPermission(Permission perm) {
        //we can't do this by setting a policy,
        //because of doPrivileged() calls in APIs
        //like swing
        if (enabled) {
            for (Class<?> c : getClassContext()) {
                if (pluginLoader.equals(c.getClassLoader())) {
                    throw new SecurityException(perm.toString());
                }
            }
        }
//		super.checkPermission(perm);
    }
}
