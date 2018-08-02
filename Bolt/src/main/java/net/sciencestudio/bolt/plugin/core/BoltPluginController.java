package net.sciencestudio.bolt.plugin.core;

import java.net.URL;

/**
 * A BoltPluginController wraps a single plugin and allows it to be managed and introspected.
 * @author NAS
 *
 * @param <T>
 */
public interface BoltPluginController<T extends BoltPlugin> {

	Class<? extends T> getImplementationClass();

	/**
	 * Gets the base class that all plugins of this type must implement or extend
	 */
	Class<T> getPluginClass();

	T create();

	boolean isEnabled();

	/**
	 * Returns an instance of this plugin which is to be used for reference only. 
	 * Do not use this instance of the plugin directly.
	 */
	T getReferenceInstance();
	
	/**
	 * A short, descriptive name for this plugin. If the plugin cannot be loaded, returns null.
	 */
	String getName();

	/**
	 * A longer description of what this plugin is and what it does. If the plugin cannot be loaded, returns null.
	 * @return
	 */
	String getDescription();

	/**
	 * A version string for this plugin. If the plugin cannot be loaded, returns null.
	 */
	String getVersion();
	
	/**
	 * A UUID uniquely identifying the plugin.
	 */
	String getUUID();

	URL getSource();

	/**
	 * Returns true if (and only if) the UUID of the other plugin matches 
	 * this one, and the version of this plugin is the same or greater than the other one 
	 * @param other the plugin to test against
	 * @return true if this plugin's version is newer or the same, false if this plugin version is older, or if the UUIDs don't match
	 */
	default boolean isUpgradeFor(BoltPluginController<?> other) {
		if (getUUID() != other.getUUID()) {
			return false;
		}
		int cmp = new AlphaNumericComparitor(false).compare(getVersion(), other.getVersion());
		if (cmp >= 0) {
			//this plugin is an older (or same) version of the given one, so it's an upgrade
			return true;
		} else {
			//this is the newer version
			return false;
		}
	}
	
}