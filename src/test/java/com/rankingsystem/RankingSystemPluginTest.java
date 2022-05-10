package com.rankingsystem;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RankingSystemPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RankingSystemPlugin.class);
		RuneLite.main(args);
	}
}