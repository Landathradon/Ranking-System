package com.rankingsystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import com.google.inject.Inject;

import javax.swing.*;

import com.rankingsystem.classes.PanelData;
import com.rankingsystem.ui.RankingSystemPluginPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
@PluginDescriptor(
        name = "Ranking System"
)
public class RankingSystemPlugin extends Plugin {
    @Inject
    @Getter
    private Client client;
    @Getter
    @Inject
    private ClientThread clientThread;
    @Inject
    private ItemManager itemManager;
    @Inject
    @Getter
    private RankingSystemConfig config;
    @Inject
    @Getter
    private RankHandler rankingSearch;
    @Getter
    private RankingSystemPluginPanel pluginPanel;
    @Inject
    private ItemManagerHelper itemManagerHelper;
    @Inject
    private ClientToolbar clientToolbar;
    private NavigationButton navButton;
    @Getter
    private final Map<Integer, Integer> trackedVarbits = new HashMap<>();
    @Getter
    private final Map<String, String> completedCombatAchievements = new HashMap<>();
    private PanelData panelData = new PanelData();
    private int lastLoginTick = -1;

    private String currentPlayerName = "";
    private boolean spawned = false;
    @Setter
    private boolean askedCheckCA = false;

    @Setter
    boolean askedPrayers = false;
    public boolean hasRigour = false;
    public boolean hasAugury = false;
    public boolean hasPiety = false;
    private static final int[] COLLECTION_LOG_TABS_PACKED_IDS = {40697867, 40697871, 40697887, 40697882, 40697889};
    private static final File COLLECTION_LOG_SAVE_DIR = new File(RUNELITE_DIR, "rankingsystem");
    @Getter
    private Map<String, JsonArray> collectionLogEntry = new HashMap<>();
    private static final int COMBAT_ACHIEVEMENT_WIDGET_ID = 715;
    private final int CA_FILTER_TIER_PACKED_ID = 46858259;
    private final int CA_FILTER_TYPE_PACKED_ID = 46858260;
    private final int CA_FILTER_MONSTER_PACKED_ID = 46858261;
    private final int CA_FILTER_COMPLETED_PACKED_ID = 46858262;
    private final int CA_ACHIEVEMENT_LIST_PACKED_ID = 46858250;
    private final int COMBAT_ACHIEVEMENT_COMPLETED_COLOR_CODE = 901389;
    private static final int[] COMBAT_TASK_AMOUNT = {0, 33, 41, 58, 112, 101, 76};
    private static final String[] COMBAT_TASK_NAMES = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
    public final String CANT_FIND = "Cant find";

    private final int[] varbitsToTrack = {
            Varbits.DIARY_ARDOUGNE_EASY, Varbits.DIARY_ARDOUGNE_MEDIUM, Varbits.DIARY_ARDOUGNE_HARD, Varbits.DIARY_ARDOUGNE_ELITE,
            Varbits.DIARY_DESERT_EASY, Varbits.DIARY_DESERT_MEDIUM, Varbits.DIARY_DESERT_HARD, Varbits.DIARY_DESERT_ELITE,
            Varbits.DIARY_FALADOR_EASY, Varbits.DIARY_FALADOR_MEDIUM, Varbits.DIARY_FALADOR_HARD, Varbits.DIARY_FALADOR_ELITE,
            Varbits.DIARY_FREMENNIK_EASY, Varbits.DIARY_FREMENNIK_MEDIUM, Varbits.DIARY_FREMENNIK_HARD, Varbits.DIARY_FREMENNIK_ELITE,
            Varbits.DIARY_KANDARIN_EASY, Varbits.DIARY_KANDARIN_MEDIUM, Varbits.DIARY_KANDARIN_HARD, Varbits.DIARY_KANDARIN_ELITE,
            Varbits.DIARY_KARAMJA_EASY, Varbits.DIARY_KARAMJA_MEDIUM, Varbits.DIARY_KARAMJA_HARD, Varbits.DIARY_KARAMJA_ELITE,
            Varbits.DIARY_KOUREND_EASY, Varbits.DIARY_KOUREND_MEDIUM, Varbits.DIARY_KOUREND_HARD, Varbits.DIARY_KOUREND_ELITE,
            Varbits.DIARY_LUMBRIDGE_EASY, Varbits.DIARY_LUMBRIDGE_MEDIUM, Varbits.DIARY_LUMBRIDGE_HARD, Varbits.DIARY_LUMBRIDGE_ELITE,
            Varbits.DIARY_MORYTANIA_EASY, Varbits.DIARY_MORYTANIA_MEDIUM, Varbits.DIARY_MORYTANIA_HARD, Varbits.DIARY_MORYTANIA_ELITE,
            Varbits.DIARY_VARROCK_EASY, Varbits.DIARY_VARROCK_MEDIUM, Varbits.DIARY_VARROCK_HARD, Varbits.DIARY_VARROCK_ELITE,
            Varbits.DIARY_WESTERN_EASY, Varbits.DIARY_WESTERN_MEDIUM, Varbits.DIARY_WESTERN_HARD, Varbits.DIARY_WESTERN_ELITE,
            Varbits.DIARY_WILDERNESS_EASY, Varbits.DIARY_WILDERNESS_MEDIUM, Varbits.DIARY_WILDERNESS_HARD, Varbits.DIARY_WILDERNESS_ELITE,
            Varbits.PRAYER_AUGURY, Varbits.PRAYER_RIGOUR, Varbits.PRAYER_PIETY
    };

    @Provides
    RankingSystemConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RankingSystemConfig.class);
    }

    @Override
    protected void startUp() {
        this.pluginPanel = new RankingSystemPluginPanel(this, panelData);

        navButton = NavigationButton.builder()
                .tooltip("Ranking System")
                .icon(ImageUtil.loadImageResource(getClass(), "/icon.png"))
                .priority(3)
                .panel(pluginPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        log.info(config.PluginName + " started!");
    }

    @Override
    protected void shutDown() {
        clearVariables();
        clientToolbar.removeNavigation(navButton);
        log.info(config.PluginName + " stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState()) {
            case LOGIN_SCREEN:
            case HOPPING:
            case LOGGING_IN:
            case LOGIN_SCREEN_AUTHENTICATOR:
                if(!Objects.equals(currentPlayerName, "")){
                    clearVariables();
                }
                break;
            case CONNECTION_LOST:
                // set to -1 here in-case of race condition with varbits changing before this handler is called
                // when game state becomes LOGGED_IN
                lastLoginTick = -1;
                break;
            case LOGGED_IN:
                lastLoginTick = client.getTickCount();
                break;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged varbitChanged) {

        if (lastLoginTick == -1 || client.getTickCount() - lastLoginTick < 8) {
            return; // Ignoring varbit change as only just logged in
        }

        for (@Varbit int varbit : varbitsToTrack) {
            int newValue = client.getVarbitValue(varbit);
//            int previousValue = trackedVarbits.getOrDefault(varbit, -1);
            trackedVarbits.put(varbit, newValue);

            if (newValue == 1) {
                if (varbit == Varbits.PRAYER_PIETY && !hasPiety) {
                    hasPiety = true;
                }
                if (varbit == Varbits.PRAYER_RIGOUR && !hasRigour) {
                    hasRigour = true;
                }
                if (varbit == Varbits.PRAYER_AUGURY && !hasAugury) {
                    hasAugury = true;
                }
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged ev) {
        if (ev.getContainerId() == InventoryID.BANK.getId()
                || (ev.getContainerId() == InventoryID.EQUIPMENT.getId())
                || (ev.getContainerId() == InventoryID.INVENTORY.getId())) {
            itemManagerHelper.UpdateOwnedItems();
        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned playerSpawned) {
        if (spawned) {
            return;
        }

        if (playerSpawned.getPlayer() == client.getLocalPlayer()) {
            currentPlayerName = playerSpawned.getPlayer().getName();
            spawned = true;
            clientThread.invokeLater(() -> {
                RankHandler.refreshStats(playerSpawned.getPlayer());
                loadCollectionLog();
            });
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widgetLoaded) {
        widgetHandler(widgetLoaded.getGroupId());
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed widgetClosed) {
        if (widgetClosed.getGroupId() == WidgetID.COLLECTION_LOG_ID) {
            saveCollectionLog();
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        if (ArrayUtils.contains(COLLECTION_LOG_TABS_PACKED_IDS, menuOptionClicked.getParam1())) {
            widgetHandler(WidgetID.COLLECTION_LOG_ID);
        }
    }

    public void widgetHandler(int widgetID) {

        if (widgetID == COMBAT_ACHIEVEMENT_WIDGET_ID) {
            completedCombatAchievements.clear();

            // Children are rendered on tick after widget load. Invoke later to prevent null children on widget
            clientThread.invokeLater(() -> {

                Widget CA_FILTER_TIER = client.getWidget(CA_FILTER_TIER_PACKED_ID);
                Widget CA_FILTER_TYPE = client.getWidget(CA_FILTER_TYPE_PACKED_ID);
                Widget CA_FILTER_MONSTER = client.getWidget(CA_FILTER_MONSTER_PACKED_ID);
                Widget CA_FILTER_COMPLETED = client.getWidget(CA_FILTER_COMPLETED_PACKED_ID);

                if (Stream.of(CA_FILTER_TIER, CA_FILTER_TYPE, CA_FILTER_MONSTER, CA_FILTER_COMPLETED)
                        .anyMatch(x -> !Objects.equals(x.getChild(4).getText(), "All"))) {
                    if (askedCheckCA) {
                        SwingUtilities.invokeLater(() ->{
                            JOptionPane.showMessageDialog(pluginPanel, "Make sure every filter is set to \"All\" then re-open your combat tasks from the top left menu.", "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    return;
                }

                clientThread.invokeLater(() -> {
                    try {
                        Widget CA_ACHIEVEMENT_LIST = client.getWidget(CA_ACHIEVEMENT_LIST_PACKED_ID);

                        if (CA_ACHIEVEMENT_LIST == null){ return; }

                        int currentAchievement = 1;
                        for (Widget achievement : Objects.requireNonNull(CA_ACHIEVEMENT_LIST.getChildren())) {

                            if (achievement.getTextColor() == COMBAT_ACHIEVEMENT_COMPLETED_COLOR_CODE) {
                                completedCombatAchievements.put(achievement.getText(), getCombatAchievementTier(currentAchievement));
                            }

                            currentAchievement++;
                        }
                    } catch (Exception exception) {
                        // In some rare instance, "CA_ACHIEVEMENT_LIST" is not loaded and returns null then prevents the user from continuing.
                        completedCombatAchievements.put(CANT_FIND, "None");
                    }
                });
            });
        }
        if (widgetID == WidgetID.COLLECTION_LOG_ID) {
            clientThread.invokeLater(() -> {
                Widget itemsContainer = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_ITEMS);
                Widget itemsHeader = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER);

                if (itemsContainer == null || itemsHeader == null) {
                    return;
                }

                String currentItemHeader = itemsHeader.getChild(0).getText();
                Widget[] widgetItems = itemsContainer.getDynamicChildren();
                JsonArray items = new JsonArray();

                for (Widget widgetItem : widgetItems) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", widgetItem.getItemId());
                    item.addProperty("name", itemManager.getItemComposition(widgetItem.getItemId()).getName());
                    item.addProperty("quantity", widgetItem.getOpacity() == 0 ? widgetItem.getItemQuantity() : 0);
                    item.addProperty("obtained", widgetItem.getOpacity() == 0);

                    items.add(item);
                }

                addItemsFromCollectionLog(currentItemHeader, items);
            });
        }
    }

    public void addItemsFromCollectionLog(String currentItemHeader, JsonArray items) {
        collectionLogEntry.remove(currentItemHeader);
        collectionLogEntry.put(currentItemHeader, items);
    }

    public void clearVariables() {
        this.spawned = false;
        this.lastLoginTick = -1;
        this.trackedVarbits.clear();
        this.completedCombatAchievements.clear();
        this.hasRigour = false;
        this.hasAugury = false;
        this.hasPiety = false;
        this.askedPrayers = false;
        this.askedCheckCA = false;
        this.panelData = new PanelData();
        this.currentPlayerName = "";

        RankHandler.clear();
        pluginPanel.refreshPanel(panelData);
    }

    public static boolean between(int variable, int minValueInclusive, int maxValueInclusive) {
        return variable >= minValueInclusive && variable <= maxValueInclusive;
    }

    public static String getCombatAchievementTier(int currentTask) {
        String tierName = "";
        int previousMax = 0;
        for (int i = 0; i < COMBAT_TASK_NAMES.length; i++) {
            if (between(currentTask, COMBAT_TASK_AMOUNT[i], previousMax + COMBAT_TASK_AMOUNT[i + 1])) {
                tierName = COMBAT_TASK_NAMES[i];
                break;
            }
            previousMax += COMBAT_TASK_AMOUNT[i + 1];
        }
        return tierName;
    }

    private void saveCollectionLog() {
        if (collectionLogEntry == null) {
            return;
        }

        COLLECTION_LOG_SAVE_DIR.mkdir();

        String fileName = "rankingSystemData-" + client.getLocalPlayer().getName() + ".json";
        String filePath = COLLECTION_LOG_SAVE_DIR + File.separator + fileName;

        try {
            JsonObject collectionLogData = new JsonObject();
            JsonArray data = new JsonArray();

            JsonObject currentLogItem;
            for (Map.Entry<String, JsonArray> entry : collectionLogEntry.entrySet()) {
                currentLogItem = new JsonObject();
                currentLogItem.add(entry.getKey(), entry.getValue());
                data.add(currentLogItem);
            }
            collectionLogData.add("items", data);

            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(collectionLogData.toString());
            writer.close();

        } catch (IOException e) {
            log.error("Unable to export Collection log items: " + e.getMessage());
        }
    }

    void loadCollectionLog() {
        try {
            String fileName = "rankingSystemData-" + client.getLocalPlayer().getName() + ".json";
            FileReader reader = new FileReader(COLLECTION_LOG_SAVE_DIR + File.separator + fileName);
            JsonObject collectionLogData = new JsonParser().parse(reader).getAsJsonObject();

            for (JsonElement data : collectionLogData.get("items").getAsJsonArray()) {
                JsonObject currentLogItem = data.getAsJsonObject();
                String name = currentLogItem.keySet().toArray()[0].toString();
                collectionLogEntry.put(name, currentLogItem.get(name).getAsJsonArray());
            }

            reader.close();
        } catch (IOException e) {
            collectionLogEntry = new HashMap<>();
        }
    }
}
