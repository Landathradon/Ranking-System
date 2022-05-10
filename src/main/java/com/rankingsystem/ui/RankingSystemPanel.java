package com.rankingsystem.ui;

import com.rankingsystem.RankingSystemRankHandler;
import net.runelite.client.ui.ColorScheme;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;


@Singleton
public class RankingSystemPanel extends JPanel {
    private static RankingSystemPluginPanel pluginPanel;

    @Inject
    public RankingSystemPanel(RankingSystemPluginPanel pluginPanel, RankingSystemRankHandler.PanelData panelData) {
        RankingSystemPanel.pluginPanel = pluginPanel;

        //TODO: Make a tutorial on how to use this inside the panel
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(1, 0, 10, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel titleLabel = createLabel("Ranking System", Color.orange, -1);
        JLabel clanLabel = createLabel("Clan: " + panelData.clanSettings.getName(), null, JLabel.LEFT);
        JLabel clanRankLabel = createLabel("Current rank: " + panelData.clanRank, null, JLabel.LEFT);

        JPanel rankList = new JPanel();
        rankList.setLayout(new GridLayout(panelData.rankEligibility.size(), 2));
        rankList.setBorder(new EmptyBorder(5, 0, panelData.rankEligibility.size() * 5, 0));

        for (Map.Entry<Integer, RankingSystemRankHandler.RankData> entry : panelData.rankEligibility.entrySet()) {
            RankingSystemRankHandler.RankData rankData = entry.getValue();

            BufferedImage skillIcon;
            try {
                URL url = new URL("https://oldschool.runescape.wiki/images/Clan_icon_-_" + rankData.RankIcon + ".png");
                skillIcon = ImageIO.read(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JLabel rankLabel = new JLabel(new ImageIcon(skillIcon));
            rankLabel.setText("Rank: " + rankData.RankName);
            rankLabel.setForeground(rankData.RankRequirements ? Color.green : Color.red);
            rankList.add(rankLabel);
        }

        JButton searchButton = new JButton();
        searchButton.setText("Check account");
        searchButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
        searchButton.setToolTipText("Look for current rank");
        searchButton.addActionListener(e -> triggerSearch());
        searchButton.setHorizontalAlignment(JLabel.CENTER);

        JLabel loadedBossData = createLabel("Boss data loaded ? " + panelData.loadedBossData, panelData.loadedBossData ? Color.green : Color.red, -1);
        JLabel loadedCombatAchievementData = createLabel("CA data loaded ? " + panelData.loadedCombatTasks, panelData.loadedCombatTasks ? Color.green : Color.red, -1);


        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        add(titleLabel, c);
        c.gridy++;
        add(clanLabel, c);
        c.gridy++;
        add(clanRankLabel, c);
        c.gridy++;
        add(rankList, c);
        c.gridy++;
        add(searchButton, c);
        c.gridy++;
        add(loadedBossData, c);
        c.gridy++;
        add(loadedCombatAchievementData, c);
        c.gridy++;

    }

    public JLabel createLabel(String text, Color color, int jLabelPosition) {
        JLabel jLabel = new JLabel(text);
        jLabel.setForeground((color != null ? color : Color.white));
        jLabel.setHorizontalAlignment(jLabelPosition >= 0 ? jLabelPosition : JLabel.CENTER);
        jLabel.setBorder(new EmptyBorder(2, 0, 2, 0));

        return jLabel;
    }

    private void triggerSearch() {
        pluginPanel.getPlugin().getRankingSearch().triggerSearch(pluginPanel.getPlugin().getClient());
    }

}