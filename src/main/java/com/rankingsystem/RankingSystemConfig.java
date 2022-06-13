package com.rankingsystem;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("rankingsystem")
public interface RankingSystemConfig extends Config
{
	String PluginName = "Ranking System";
	boolean useTestingScript = false;

//	@ConfigItem(
//			keyName = "use_testing_script",
//			name = "Use testing script",
//			description = "Use the testing script",
//			position = 1
//	)
//	default boolean useTestingScript()
//	{
//		return true;
//	}
}
