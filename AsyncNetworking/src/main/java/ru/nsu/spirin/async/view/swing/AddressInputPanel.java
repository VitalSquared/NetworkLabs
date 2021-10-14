package ru.nsu.spirin.async.view.swing;

import ru.nsu.spirin.async.view.View;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;

public final class AddressInputPanel extends JPanel {

    private final View view;
    private final JTextField textField;
    private final JButton startButton;

    public AddressInputPanel(View view) {
        super(new BorderLayout());

        this.view = view;
        this.textField = new JTextField("");
        this.startButton = new JButton("Search this address!");

        this.textField.setPreferredSize(new Dimension(200, 50));
        this.startButton.setEnabled(false);

        this.textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButton();
            }

            private void updateButton() {
                startButton.setEnabled(!("".equals(textField.getText())));
            }
        });

        this.startButton.addActionListener(e -> {
            view.startAddressesSearchHttpRequest(textField.getText());
        });

        add(this.textField, BorderLayout.CENTER);
        add(this.startButton, BorderLayout.EAST);
    }
}