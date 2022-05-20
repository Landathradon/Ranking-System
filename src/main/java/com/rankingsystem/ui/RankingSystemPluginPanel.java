package com.rankingsystem.ui;

import com.rankingsystem.RankingSystemPlugin;

import java.awt.BorderLayout;

import com.rankingsystem.classes.PanelData;
import lombok.Getter;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;

public class RankingSystemPluginPanel extends PluginPanel {
    @Getter
    private final RankingSystemPlugin plugin;
    private PanelData panelData;

    public RankingSystemPluginPanel(RankingSystemPlugin plugin, PanelData panelData) {
        super();
        this.plugin = plugin;
        this.panelData = panelData;

        loadPanel();
    }

    public void refreshPanel(PanelData panelData) {
        this.panelData = panelData;
        removeAll();
        loadPanel();
    }

    public void loadPanel() {
        SwingUtilities.invokeLater(() ->{
            RankingSystemPanel panel = new RankingSystemPanel(this, panelData);

            setLayout(new BorderLayout(5, 5));

            add(panel, BorderLayout.CENTER);
            updateUI();
        });
    }
}
