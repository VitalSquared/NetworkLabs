package ru.nsu.spirin.async.view.swing;

import ru.nsu.spirin.async.containers.Feature;
import ru.nsu.spirin.async.containers.FeatureDescription;
import ru.nsu.spirin.async.view.View;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeaturesPanel extends JPanel {
    private final View view;

    private final JPanel[] backgrounds = new JPanel[10];

    private final Map<String, JLabel> xidToNameMap = new HashMap<>();
    private final Map<String, JButton> xidToDescButtonMap = new HashMap<>();

    public FeaturesPanel(View view) {
        super(new GridLayout(10, 1));
        this.view = view;

        for (int i = 0; i < 10; i++) {
            backgrounds[i] = new JPanel(new GridLayout(2, 1));
            backgrounds[i].setBackground(i % 2 == 0 ?
                    Color.GRAY :
                    Color.WHITE);
            add(backgrounds[i]);
        }
    }

    public void updateFeatures(List<Feature> features) {
        if (null == features || 0 == features.size()) {
            for (int i = 0; i < 10; i++) {
                backgrounds[i].removeAll();
            }
            xidToNameMap.clear();
            xidToDescButtonMap.clear();
        }
        else {
            for (int i = 0; i < Math.min(10, features.size()); i++) {
                var entry = features.get(i);
                if (null == entry.getName() || "".equals(entry.getName())) {
                    continue;
                }

                JLabel name = new JLabel(entry.getName());
                JButton button = new JButton("View description");

                backgrounds[i].add(name);
                backgrounds[i].add(button);

                xidToNameMap.put(entry.getXid(), name);
                xidToDescButtonMap.put(entry.getXid(), button);
            }
        }
        this.invalidate();
        this.validate();
    }

    public void updateDescription(String xid, FeatureDescription description) {
        if (xidToDescButtonMap.containsKey(xid)) {
            if (null == description || null == description.getName()) {
                xidToDescButtonMap.get(xid).addActionListener(e -> {
                    JOptionPane.showMessageDialog(null, "Error occurred trying to get description!");
                });
            }
            else if (null == description.getInfo() || null == description.getInfo().getDescription()) {
                xidToDescButtonMap.get(xid).addActionListener(e -> {
                    JOptionPane.showMessageDialog(null, "No description available!");
                });
            }
            else {
                xidToDescButtonMap.get(xid).addActionListener(e -> {
                    JOptionPane pane = new JOptionPane("<html>" + description.getInfo().getDescription() + "</html>");
                    JDialog dialog = pane.createDialog(null, "Description");
                    dialog.setSize(new Dimension(960, 540));
                    dialog.setVisible(true);
                });
            }
        }
        this.invalidate();
        this.validate();
    }
}
