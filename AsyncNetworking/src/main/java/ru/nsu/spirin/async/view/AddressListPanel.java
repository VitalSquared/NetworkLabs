package ru.nsu.spirin.async.view;

import lombok.Getter;
import static ru.nsu.spirin.async.utils.APIRequestGenerator.*;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Component;

public final class AddressListPanel {
    private final JPanel mainPanel;
    private final @Getter JScrollPane scrollPane;
    private int currentButtonsNumber = 0;

    public AddressListPanel() {
        this.mainPanel = new JPanel(SwingUtils.createVerticalGridLayout(1));
        this.scrollPane = new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void addAddress(Component component) {
        if (this.currentButtonsNumber >= MAX_ADDRESSES_NUMBER) {
            return;
        }

        this.mainPanel.add(component);
        this.currentButtonsNumber++;
        this.mainPanel.setLayout(SwingUtils.createVerticalGridLayout(this.currentButtonsNumber));
        this.mainPanel.revalidate();
    }

    public void clearAll() {
        this.mainPanel.removeAll();
        this.currentButtonsNumber = 0;
        this.mainPanel.revalidate();
    }
}
