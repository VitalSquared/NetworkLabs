package ru.nsu.spirin.async.view;

import static ru.nsu.spirin.async.utils.APIRequestGenerator.*;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

public final class AddressListPanel extends JPanel {
    private final JPanel mainPanel;
    private int currentButtonsNumber = 0;

    public AddressListPanel() {
        super(new BorderLayout());

        this.mainPanel = new JPanel(new GridLayout(1, 1, 0, 10));
        JScrollPane scrollPane = new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void addAddress(Component component) {
        if (this.currentButtonsNumber >= MAX_ADDRESSES_NUMBER) {
            return;
        }

        this.mainPanel.add(component);
        this.currentButtonsNumber++;
        this.mainPanel.setLayout(new GridLayout(this.currentButtonsNumber, 1, 0, 10));
    }

    public void clearAll() {
        this.mainPanel.removeAll();
        this.currentButtonsNumber = 0;
        this.mainPanel.revalidate();
        this.revalidate();
    }
}
