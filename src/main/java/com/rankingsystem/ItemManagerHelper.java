package com.rankingsystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.*;
import net.runelite.client.game.ItemMapping;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ItemManagerHelper {
    @Inject
    private RankingSystemPlugin plugin;
    Item[] items;
    private final Client client;
    public Map<String, int[]> itemMapping;

    @Inject
    private ItemManagerHelper(Client client) {
        this.client = client;
//        ItemMapping.ITEM_TRIDENT;
        //TODO: Make this look better and grab every single id for these item
        itemMapping = Stream.of(new Object[][]{
                {"graceful hood", new int[]{11850, 11851, 13579, 13580, 13591, 13592, 13603, 13604, 13615, 13616, 13627, 13628, 13667, 13668, 21061, 21063,}},
                {"graceful top", new int[]{11854, 11855, 13583, 13584, 13595, 13596, 13607, 13608, 13619, 13620, 13631, 13632, 13671, 13672, 21067, 21069,}},
                {"graceful legs", new int[]{11856, 11857, 13585, 13586, 13597, 13598, 13609, 13610, 13621, 13622, 13633, 13634, 13673, 13674, 21070, 21072,}},
                {"graceful boots", new int[]{11860, 11861, 13589, 13590, 13601, 13602, 13613, 13614, 13625, 13626, 13637, 13638, 13677, 13678, 21076, 21078,}},
                {"graceful gloves", new int[]{11858, 11859, 13587, 13588, 13599, 13600, 13611, 13612, 13623, 13624, 13635, 13636, 13675, 13676, 21073, 21075,}},
                {"graceful cape", new int[]{11852, 11853, 13581, 13582, 13593, 13594, 13605, 13606, 13617, 13618, 13629, 13630, 13669, 13670, 21064, 21066,}},
                {"pharaoh's sceptre", new int[]{9044, 9045, 9046, 9047, 9048, 9049, 9050, 9051, 13074, 13075, 13076, 13077, 13078}},
                {"quest point cape", new int[]{9813, 10662, 13068}},
                {"achievement diary cape", new int[]{13069, 19476}},
                {"fire cape", new int[]{6570, 10566, 10637, 20445}},
                {"infernal cape", new int[]{21287, 21295, 21297}},
                {"black mask", new int[]{8901, 8902, 8903, 8904, 8905, 8906, 8907, 8908, 8909, 8910, 8911, 8912, 8913,
                        8914, 8915, 8916, 8917, 8918, 8919, 8920, 8921, 8922, 11774, 11775, 11776, 11777, 11778, 11779,
                        11780, 11781, 11782, 11783, 11784,}},
                {"slayer helm", new int[]{11864, 11865, 14002, 14003, 19639, 19640, 19641, 19642, 19643, 19644, 19645,
                        19646, 19647, 19648, 19649, 19650, 21264, 21265, 21266, 21267, 21888, 21889, 21890, 21891, 23073,
                        23074, 23075, 23076, 24370, 24371, 24444, 24445}},
                {"ava's assembler", new int[]{21898, 21899, 21900, 21901, 21914, 21915, 21916, 21917, 22109, 22110, 24135, 24222}},
                {"vorkath's head", new int[]{2425}},
                {"imbued saradomin cape", new int[]{21776, 21777, 21778, 21779, 21791, 21792, 23608, 24232, 24236, 24237, 24238, 24239, 24248}},
                {"imbued zamorak cape", new int[]{21780, 21781, 21782, 21783, 21795, 21796, 23606, 24233, 24244, 24245, 24246, 24247, 24250}},
                {"imbued guthix cape", new int[]{21784, 21785, 21786, 21787, 21793, 21794, 23604, 24234, 24240, 24241, 24242, 24243, 24249}},
                {"dragon warhammer", new int[]{13576, 13577, 14728}},
                {"berserker ring", new int[]{6737, 6738, 11773, 18398, 18399}},

        }).collect(Collectors.toMap(data -> (String) data[0], data -> (int[]) data[1]));
    }

    public void UpdateOwnedItems() {
        //TODO: get items from any container
        items = ArrayUtils.addAll(checkContainers(InventoryID.BANK), checkContainers(InventoryID.INVENTORY));
        items = ArrayUtils.addAll(items, checkContainers(InventoryID.EQUIPMENT));
    }

    private Item[] checkContainers(InventoryID inventoryID) {
        Item[] containerItems = new Item[0];
        ItemContainer container = client.getItemContainer(inventoryID);
        if (container != null) {
            containerItems = container.getItems();
        }
        return containerItems;
    }

    public boolean HasItemCustom(String name) {
        int[] reqItems = itemMapping.get(name.toLowerCase());

        if (plugin.getCollectionLogEntry().size() > 0) {
            for (JsonArray items : plugin.getCollectionLogEntry().values()) {
                for (JsonElement item : items) {
                    JsonObject currentItem = item.getAsJsonObject();
                    if (currentItem.get("name").getAsString().toLowerCase().contains(name.toLowerCase()) && currentItem.get("obtained").getAsBoolean()) {
                        return true;
                    }
                }
            }
        }

        if (reqItems == null) {
            return false;
        }

        for (Item item : items) {
            if (Arrays.stream(reqItems).anyMatch(x -> x == item.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean HasItem(String name) {
        String modifiedName = "ITEM_" + name.replace(" ", "_").replace("'", "").toUpperCase();
        boolean result = false;

        try {
            int[] itemIDs = ItemMapping.valueOf(modifiedName).getUntradableItems();

            if (itemIDs != null) {

                for (int itemID : itemIDs) {
                    result = Arrays.stream(items).anyMatch(x -> x.getId() == itemID);
                }

            } else {
                int itemID = ItemMapping.valueOf(modifiedName).getTradeableItem();
                result = Arrays.stream(items).anyMatch(x -> x.getId() == itemID);
            }
        } catch (Exception ex) {
            result = HasItemCustom(name);
        }

        //If the result has not been found, try to get it from the custom list.
        if (!result) {
            result = HasItemCustom(name);
        }

        return result;
    }


}