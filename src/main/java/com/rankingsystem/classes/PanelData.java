package com.rankingsystem.classes;

import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelData {
    public Map<Integer, RankData> rankEligibility = new HashMap<>();
    public ClanSettings clanSettings = new ClanSettings() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public List<ClanMember> getMembers() {
            return null;
        }

        @Nullable
        @Override
        public ClanMember findMember(String name) {
            return null;
        }

        @Nullable
        @Override
        public ClanTitle titleForRank(ClanRank clanRank) {
            return null;
        }
    };
    public ClanMember clanMembers = new ClanMember() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public ClanRank getRank() {
            return ClanRank.GUEST;
        }
    };
    public String clanRank = "";
    public boolean loadedBossData = false;
    public boolean loadedCombatTasks = false;
}
