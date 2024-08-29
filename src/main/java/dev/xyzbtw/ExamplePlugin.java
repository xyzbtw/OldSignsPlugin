package dev.xyzbtw;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * OldSigns plugin
 *
 * @author xyzbtw
 */
public class ExamplePlugin extends Plugin {
	
	@Override
	public void onLoad() {
		this.getLogger().info("Loading old signs plugin!");
		
		final OldSigns exampleModule = new OldSigns();
		RusherHackAPI.getModuleManager().registerFeature(exampleModule);
		
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Old signs unloaded!");
	}
	
}