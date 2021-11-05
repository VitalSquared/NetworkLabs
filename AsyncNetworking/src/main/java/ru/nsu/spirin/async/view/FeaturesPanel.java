package ru.nsu.spirin.async.view;

import lombok.Getter;
import ru.nsu.spirin.async.containers.Feature;
import ru.nsu.spirin.async.containers.FeatureDescription;
import static ru.nsu.spirin.async.utils.APIRequestGenerator.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeaturesPanel {
    private static final Dimension DESC_WINDOW_SIZE = new Dimension(960, 540);

    private final JPanel mainPanel;
    private final @Getter JScrollPane scrollPane;

    private final JPanel[] backgrounds = new JPanel[MAX_FEATURES_NUMBER];
    private final Map<String, JButton> xidToDescButtonMap = new HashMap<>();

    public FeaturesPanel() {
        this.mainPanel = new JPanel(SwingUtils.createVerticalGridLayout(MAX_FEATURES_NUMBER));
        this.scrollPane = new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        for (int i = 0; i < MAX_FEATURES_NUMBER; i++) {
            this.backgrounds[i] = new JPanel(new BorderLayout());
            this.backgrounds[i].setBackground(Color.LIGHT_GRAY);
            this.backgrounds[i].setVisible(false);
            mainPanel.add(this.backgrounds[i]);
        }
    }

    public void updateFeatures(List<Feature> features) {
        for (int i = 0; i < MAX_FEATURES_NUMBER; i++) {
            this.backgrounds[i].removeAll();
            this.backgrounds[i].setVisible(false);
            this.backgrounds[i].revalidate();
        }
        this.xidToDescButtonMap.clear();

        if (null == features) {
            return;
        }

        int i = 0;
        for (var entry : features) {
            if (null == entry.getName() || entry.getName().isEmpty()) {
                continue;
            }

            JLabel name = new JLabel(entry.getName());
            JButton button = new JButton("View description");

            this.backgrounds[i].setVisible(true);
            this.backgrounds[i].add(name, BorderLayout.CENTER);
            this.backgrounds[i].add(button, BorderLayout.EAST);
            this.backgrounds[i].revalidate();

            this.xidToDescButtonMap.put(entry.getXid(), button);

            button.setVisible(false);

            i++;
            if (i >= MAX_FEATURES_NUMBER) {
                break;
            }
        }

        this.mainPanel.revalidate();
    }

    public void updateDescription(String xid, FeatureDescription description) {
        JButton xidButton;
        synchronized (this) {
            if (!this.xidToDescButtonMap.containsKey(xid)) {
                return;
            }
            xidButton = this.xidToDescButtonMap.get(xid);
        }

        xidButton.setVisible(true);

        if (null == description) {
            xidButton.addActionListener(e -> JOptionPane.showMessageDialog(null, "Error occurred trying to get description!"));
        }
        else if (null == description.getInfo() || null == description.getInfo().getDescription()) {
            xidButton.addActionListener(e -> JOptionPane.showMessageDialog(null, "No description available!", description.getName(), JOptionPane.PLAIN_MESSAGE));
        }
        else {
            xidButton.addActionListener(e -> {
                JOptionPane pane = new JOptionPane("<html>" + description.getInfo().getDescription() + "</html>");
                JDialog dialog = pane.createDialog(this.mainPanel.getRootPane(), description.getName());
                dialog.setSize(DESC_WINDOW_SIZE);
                dialog.setVisible(true);
            });
        }

        this.mainPanel.revalidate();
    }
}
