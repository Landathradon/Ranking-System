package com.rankingsystem.ui;

import com.rankingsystem.RankingSystemPlugin;

import java.awt.BorderLayout;

import com.rankingsystem.RankingSystemRankHandler;
import lombok.Getter;
import net.runelite.client.ui.PluginPanel;

public class RankingSystemPluginPanel extends PluginPanel {
    @Getter
    private final RankingSystemPlugin plugin;
    private RankingSystemRankHandler.PanelData panelData;

    public RankingSystemPluginPanel(RankingSystemPlugin plugin, RankingSystemRankHandler.PanelData panelData) {
        super();
        this.plugin = plugin;
        this.panelData = panelData;

        loadPanel();
    }

    public void refreshPanel(RankingSystemRankHandler.PanelData panelData) {
        this.panelData = panelData;
        removeAll();
        loadPanel();
    }

    public void loadPanel() {
        RankingSystemPanel panel = new RankingSystemPanel(this, panelData);

        setLayout(new BorderLayout(5, 5));

        add(panel, BorderLayout.CENTER);
        updateUI();
    }
}
