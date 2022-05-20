package com.rankingsystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rankingsystem.classes.PanelData;
import com.rankingsystem.classes.RankData;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.clan.ClanMember;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreSkill;
import okhttp3.OkHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.inject.Inject;
import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.runelite.api.Varbits.*;

public class RankHandler {
    @Inject
    private Client client;
    @Inject
    private RankingSystemPlugin plugin;
    @Inject
    private RankingSystemConfig config;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private ItemManagerHelper itemManagerHelper;
    private static HiscoreClient hiscoreClient;
    @Inject
    private OkHttpClient okHttpClient;
    private static Map<Skill, Integer> baseLevels = new HashMap<>();
    private static Map<String, Integer> bossKills = new HashMap<>();
    @Getter
    private static Map<Integer, RankData> rankEligibility = new HashMap<>();
    private static PanelData panelData = new PanelData();
    private Player player;
    private static boolean askedToFillBossKills = false;
    private final int[] varbitsEasyDiaries = {
            Varbits.DIARY_ARDOUGNE_EASY,
            Varbits.DIARY_DESERT_EASY,
            Varbits.DIARY_FALADOR_EASY,
            Varbits.DIARY_FREMENNIK_EASY,
            Varbits.DIARY_KANDARIN_EASY,
            Varbits.DIARY_KARAMJA_EASY,
            Varbits.DIARY_KOUREND_EASY,
            Varbits.DIARY_LUMBRIDGE_EASY,
            Varbits.DIARY_MORYTANIA_EASY,
            Varbits.DIARY_VARROCK_EASY,
            Varbits.DIARY_WESTERN_EASY,
            Varbits.DIARY_WILDERNESS_EASY,
    };
    private final int[] varbitsMediumDiaries = {
            Varbits.DIARY_ARDOUGNE_MEDIUM,
            Varbits.DIARY_DESERT_MEDIUM,
            Varbits.DIARY_FALADOR_MEDIUM,
            Varbits.DIARY_FREMENNIK_MEDIUM,
            Varbits.DIARY_KANDARIN_MEDIUM,
            Varbits.DIARY_KARAMJA_MEDIUM,
            Varbits.DIARY_KOUREND_MEDIUM,
            Varbits.DIARY_LUMBRIDGE_MEDIUM,
            Varbits.DIARY_MORYTANIA_MEDIUM,
            Varbits.DIARY_VARROCK_MEDIUM,
            Varbits.DIARY_WESTERN_MEDIUM,
            Varbits.DIARY_WILDERNESS_MEDIUM,
    };
    private final int[] varbitsHardDiaries = {
            Varbits.DIARY_ARDOUGNE_HARD,
            Varbits.DIARY_DESERT_HARD,
            Varbits.DIARY_FALADOR_HARD,
            Varbits.DIARY_FREMENNIK_HARD,
            Varbits.DIARY_KANDARIN_HARD,
            Varbits.DIARY_KARAMJA_HARD,
            Varbits.DIARY_KOUREND_HARD,
            Varbits.DIARY_LUMBRIDGE_HARD,
            Varbits.DIARY_MORYTANIA_HARD,
            Varbits.DIARY_VARROCK_HARD,
            Varbits.DIARY_WESTERN_HARD,
            Varbits.DIARY_WILDERNESS_HARD,
    };

    private final int[] varbitsEliteDiaries = {
            Varbits.DIARY_ARDOUGNE_ELITE,
            Varbits.DIARY_DESERT_ELITE,
            Varbits.DIARY_FALADOR_ELITE,
            Varbits.DIARY_FREMENNIK_ELITE,
            Varbits.DIARY_KANDARIN_ELITE,
            Varbits.DIARY_KARAMJA_ELITE,
            Varbits.DIARY_KOUREND_ELITE,
            Varbits.DIARY_LUMBRIDGE_ELITE,
            Varbits.DIARY_MORYTANIA_ELITE,
            Varbits.DIARY_VARROCK_ELITE,
            Varbits.DIARY_WESTERN_ELITE,
            Varbits.DIARY_WILDERNESS_ELITE,
    };

    public static void clear() {
        askedToFillBossKills = false;
        bossKills = new HashMap<>();
        baseLevels = new HashMap<>();
        rankEligibility = new HashMap<>();
        panelData = new PanelData();
    }

    public void triggerSearch(final Client client) {
        panelData = new PanelData();
        this.client = client;
        hiscoreClient = new HiscoreClient(okHttpClient);
        player = client.getLocalPlayer();

        panelData.loadedBossData = bossKills.size() > 0;
        panelData.loadedCombatTasks = plugin.getCompletedCombatAchievements().size() > 0;
        plugin.getPluginPanel().refreshPanel(panelData);

        if (plugin.getClient().getGameState() != GameState.LOGGED_IN) {
            JOptionPane.showMessageDialog(plugin.getPluginPanel(), "You must be logged in to use this button.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(!player.isClanMember()){
            JOptionPane.showMessageDialog(plugin.getPluginPanel(), "You must be in a clan to use this plugin.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (bossKills.size() <= 0) {
            JOptionPane.showMessageDialog(plugin.getPluginPanel(), "Please try again in a few seconds", "Error", JOptionPane.ERROR_MESSAGE);
            if (!askedToFillBossKills) {
                refreshStats(player);
            }
            return;
        }
        if (plugin.getCompletedCombatAchievements().size() <= 0) {
            JOptionPane.showMessageDialog(plugin.getPluginPanel(), "Please open your combat tasks and make sure every filter is set to \"All\"", "Error", JOptionPane.ERROR_MESSAGE);
            plugin.setAskedCheckCA(true);
            return;
        }
        if (plugin.getCollectionLogEntry().size() <= 0) {
            plugin.loadCollectionLog();
            if (plugin.getCollectionLogEntry().size() <= 0) {
                JOptionPane.showMessageDialog(plugin.getPluginPanel(), "Please open your collection log to fill it.\nMake sure to click on every boss, raid, clue tier, mini-game and other for the initial load or it won't know if you got that item.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        if ((!plugin.hasPiety ||!plugin.hasRigour || !plugin.hasAugury) && !plugin.askedPrayers) {
            JOptionPane.showMessageDialog(plugin.getPluginPanel(), "If you have Piety, Rigour or Augury please turn them on/off once.", "Information", JOptionPane.INFORMATION_MESSAGE);
            plugin.askedPrayers = true;
            return;
        }

        itemManagerHelper.UpdateOwnedItems();
        plugin.getPluginPanel().refreshPanel(panelData);

        final int[] bases = client.getRealSkillLevels();
        for (final Skill s : Skill.values()) {
            if (s == Skill.OVERALL) {
                continue;
            } // For some reason the overall skill returns 1;
            baseLevels.put(s, bases[s.ordinal()]);
        }

        if (player.isClanMember() && Objects.equals(client.getClanSettings().getName(), config.ClanName)) {
            panelData.clanSettings = client.getClanSettings();
            panelData.clanMembers = client.getClanSettings().findMember(player.getName());
        }

        checkRanksEligibility(panelData.clanMembers);
        panelData.rankEligibility = rankEligibility;

        if (plugin.getConfig().useTestingScript) {
            for (Map.Entry<Integer, RankData> entry : rankEligibility.entrySet()) {
                RankData rankData = entry.getValue();
                sendMessage("Rank: " + rankData.RankName + " ? " + rankData.RankRequirements);
            }

            return;
        }

        plugin.getPluginPanel().refreshPanel(panelData);
    }

    private void checkRanksEligibility(ClanMember clanMembers) {

        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(System.getProperty("user.dir") + "\\src\\main\\resources\\" +
                (plugin.getConfig().useTestingScript ? "test" : "ranks") + ".json")) {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            JSONArray rankNames = (JSONArray) jsonObject.get("ClanRankValues");

            for (JSONObject rank : (Iterable<JSONObject>) rankNames) {

                if (clanMembers != null && rank.containsKey(clanMembers.getRank().name())) {
                    panelData.clanRank = (String) rank.get(clanMembers.getRank().name());
                }
            }

            JSONArray ranks = (JSONArray) jsonObject.get("Ranks");

            for (JSONObject rank : (Iterable<JSONObject>) ranks) {
                RankData rankData = new RankData();

                rankData.RankName = (String) rank.get("RankName");
                rankData.RankIcon = (String) rank.get("RankIcon");
                rankData.RankPriority = (int) (long) rank.get("RankPriority");
                rankData.RankType = (String) rank.get("RankType");

                JSONArray requirements = (JSONArray) rank.get("RankRequirements");

                int valid = 0;
                for (JSONObject req : (Iterable<JSONObject>) requirements) {
                    if (handleRequirements(req, rankData)) {
                        valid++;
                    }
                }

                if (requirements.size() == valid) {
                    rankData.RankRequirements = true;
                }
                rankEligibility.put(rankData.RankPriority, rankData);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private boolean handleRequirements(JSONObject req, RankData rankData) {
        String type = (String) req.get("Type");
        boolean result = false;
        int count = 0;
        int minAmount;
        JSONArray listOfBosses;

        switch (type.toLowerCase()) {
            case "total level":
                int totalLevel = (int) (long) req.get("Value");
                result = client.getTotalLevel() >= totalLevel;
                break;
            case "achievement diary":
                int diaryCompleted = (int) (long) req.get("Value");
                int[] difficulty = getDifficultyVarbits((String) req.get("Difficulty"));
                result = checkRankDiariesCompleted(difficulty, diaryCompleted);
                break;
            case "boss killcount":
                int requiredKillcount = (int) (long) req.get("Value");
                String bossName = (String) req.get("Boss");
                result = bossKills.get(bossName) >= requiredKillcount;
                break;
            case "base stats":
                int minLevel = (int) (long) req.get("Value");
                result = Collections.min(baseLevels.values()) >= minLevel;
                break;
            case "items":
                String item = (String) req.get("Value");
                result = itemManagerHelper.HasItem(item);
                break;
            case "prayer":
                String prayer = (String) req.get("Value");
                result = checkPrayerUnlocked(prayer);
                break;
            case "achieve amount":
                minAmount = (int) (long) req.get("MinReqNeeded");
                JSONArray requirements = (JSONArray) req.get("Value");

                for (JSONObject achieveReq : (Iterable<JSONObject>) requirements) {
                    if (handleRequirements(achieveReq, rankData)) {
                        count++;
                    }
                }
                result = count >= minAmount;
                break;
            case "hiscores":
                int minBossOnHiscores = (int) (long) req.get("Value");

                if (Objects.equals(rankData.RankType, "Pvm")) {
                    for (Map.Entry<String, Integer> boss : bossKills.entrySet()) {
                        if (boss.getValue() > 5) {
                            count++;
                        }
                    }
                }
                result = count >= minBossOnHiscores;
                break;
            case "boss killcount combined":
                minAmount = (int) (long) req.get("Value");
                listOfBosses = (JSONArray) req.get("Bosses");

                for (String boss : (Iterable<String>) listOfBosses) {
                    if (bossKills.get(boss) != -1) {
                        count += bossKills.get(boss);
                    }
                }

                result = count >= minAmount;
                break;
            case "combat achievements":
                boolean multipleAchievements = (Boolean) req.get("Multiple");
                if (multipleAchievements) {
                    minAmount = (int) (long) req.get("Value");
                    String achievementDifficulty = (String) req.get("Difficulty");

                    for (Map.Entry<String, String> Entry : plugin.getCompletedCombatAchievements().entrySet()) {
                        if (Objects.equals(Entry.getValue(), achievementDifficulty)) {
                            count++;
                        }
                    }
                    result = count >= minAmount;
                } else {
                    String achievementName = (String) req.get("Value");
                    result = plugin.getCompletedCombatAchievements().containsKey(achievementName);
                }
                break;
            case "unique items":
                boolean multipleBosses = (Boolean) req.get("MultipleBosses");
                minAmount = (int) (long) req.get("Value");
                JsonArray itemList;

                if (multipleBosses) {
                    listOfBosses = (JSONArray) req.get("Bosses");

                    for (String boss : (Iterable<String>) listOfBosses) {
                        itemList = plugin.getCollectionLogEntry().get(boss);
                        if (itemList == null) {
                            continue;
                        }
                        for (JsonElement uniqueItem : itemList) {
                            JsonObject currentUnique = uniqueItem.getAsJsonObject();
                            if (currentUnique.get("obtained").getAsBoolean()) {
                                count++;
                            }
                        }
                    }
                } else {
                    String bossNameUnique = (String) req.get("Boss");
                    itemList = plugin.getCollectionLogEntry().get(bossNameUnique);
                    if (itemList == null) {
                        break;
                    }
                    for (JsonElement uniqueItem : itemList) {
                        JsonObject currentUnique = uniqueItem.getAsJsonObject();
                        if (currentUnique.get("obtained").getAsBoolean()) {
                            count++;
                        }
                    }
                }

                result = count >= minAmount;
                break;
            case "rank completed":
                minAmount = (int) (long) req.get("Value");

                if(rankEligibility.size() < minAmount){ break; }

                for (Map.Entry<Integer, RankData> rank: rankEligibility.entrySet()) {
                    if(rank.getValue().RankRequirements){
                        count++;
                    }
                }

                result = count >= minAmount;
                break;

            default:
                break;
        }

        return result;
    }

    private int[] getDifficultyVarbits(String difficulty) {
        int[] difficultyVarbits = varbitsEasyDiaries;
        switch (difficulty.toLowerCase()) {
            case "medium":
                difficultyVarbits = varbitsMediumDiaries;
                break;
            case "hard":
                difficultyVarbits = varbitsHardDiaries;
                break;
            case "elite":
                difficultyVarbits = varbitsEliteDiaries;
                break;
        }

        return difficultyVarbits;
    }

    private void sendMessage(String chatMessage) {
        final String message = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(chatMessage)
                .build();

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(message)
                .build());
    }

    private boolean isAchievementDiaryCompleted(int diary, int value) {
        switch (diary) {
            case DIARY_KARAMJA_EASY:
            case DIARY_KARAMJA_MEDIUM:
            case DIARY_KARAMJA_HARD:
                return value == 2; // jagex, why?
            default:
                return value == 1;
        }
    }

    private boolean checkRankDiariesCompleted(int[] diaries, int howMany) {
        int count = 0;

        for (@Varbit int diary : diaries) {
            if (plugin.getTrackedVarbits().get(diary) == null) {
                return false;
            }
            if (isAchievementDiaryCompleted(diary, plugin.getTrackedVarbits().get(diary))) {
                count++;
            }
        }

        return count > howMany;
    }

    private boolean checkPrayerUnlocked(String name) {
        boolean prayer = false;
        switch (name.toLowerCase()) {
            case "piety":
                prayer = plugin.hasPiety;
                break;
            case "rigour":
                prayer = plugin.hasRigour;
                break;
            case "augury":
                prayer = plugin.hasAugury;
                break;
        }

        return prayer;
    }

    public static void refreshStats(Player player) {
        String username = player.getName();
        try {
            hiscoreClient.lookupAsync(username, HiscoreEndpoint.NORMAL).whenCompleteAsync((result, ex) ->
                    SwingUtilities.invokeLater(() ->
                    {
                        askedToFillBossKills = true;
                        if (result == null || ex != null) {
                            if (ex != null) {
//                            log.warn("Error fetching hiscore data for " + username + " " + ex.getMessage());
                            }
                            return;
                        }

                        for (HiscoreSkill skill : HiscoreSkill.values()) {
                            addResultsToBossMap(skill, result.getSkill(skill));
                        }
                    }));
        } catch (Exception ignored) {
        }
    }

    private static void addResultsToBossMap(HiscoreSkill boss, net.runelite.client.hiscore.Skill bossData) {
        bossKills.put(boss.getName(), bossData.getLevel());
    }
}
