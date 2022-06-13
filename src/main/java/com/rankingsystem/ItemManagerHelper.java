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

@Singleton
public class ItemManagerHelper {
    @Inject
    private RankingSystemPlugin plugin;
    Item[] items;
    private final Client client;

    @Inject
    private ItemManagerHelper(Client client) {
        this.client = client;
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

    public boolean HasItemCustom(String name, JsonArray listOfItems) {
        int[] reqItems = JsonArrayToIntArray(listOfItems);

        if (plugin.getCollectionLogEntry().size() > 0) {
            for (JsonArray items : plugin.getCollectionLogEntry().values()) {
                for (JsonElement item : items) {
                    JsonObject currentItem = item.getAsJsonObject();
                    if (currentItem.get("name").getAsString().toLowerCase().contains(name.toLowerCase()) && currentItem.get("obtained").getAsBoolean()) {
                        return true;
                    }
                    else if (Arrays.stream(reqItems).anyMatch(x -> x == currentItem.get("id").getAsInt()) && currentItem.get("obtained").getAsBoolean()) {
                        return true;
                    }
                }
            }
        }

        if (reqItems.length <= 0) {
            return false;
        }

        for (Item item : items) {
            if (Arrays.stream(reqItems).anyMatch(x -> x == item.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean HasItem(String name, JsonArray listOfItems) {
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
            result = HasItemCustom(name, listOfItems);
        }

        //If the result has not been found, try to get it from the custom list.
        if (!result) {
            result = HasItemCustom(name, listOfItems);
        }

        return result;
    }

    public static int[] JsonArrayToIntArray(JsonArray jsonArray){
        int[] intArray = new int[jsonArray.size()];
        for (int i = 0; i < intArray.length; ++i) {
            intArray[i] = jsonArray.get(i).getAsInt();
        }
        return intArray;
    }

}