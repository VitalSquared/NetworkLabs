package ru.nsu.spirin.async.view;

import static ru.nsu.spirin.async.utils.APIRequestGenerator.*;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public final class AddressInputPanel extends JPanel {
    private static final Dimension TEXT_FIELD_SIZE = new Dimension(200, 50);

    public AddressInputPanel(SwingView view) {
        super(new GridLayout(4, 1));
        JSlider addressSlider = createNumberSelectionPanel("Max number of addresses to show", MIN_ADDRESSES_NUMBER, MAX_ADDRESSES_NUMBER);
        JSlider featuresSlider = createNumberSelectionPanel("Max number of features to show", MIN_FEATURES_NUMBER, MAX_FEATURES_NUMBER);
        JSlider radiusSlider = createNumberSelectionPanel("Search radius(m) of features", MIN_RADIUS_METERS, MAX_RADIUS_METERS);
        createInputPanel(view, addressSlider, featuresSlider, radiusSlider);
    }

    private JSlider createNumberSelectionPanel(String text, int minValue, int maxValue) {
        JPanel panel = new JPanel(new GridLayout(1, 2));

        JSlider slider = new JSlider(SwingConstants.HORIZONTAL, minValue, maxValue, (minValue + maxValue) / 2);
        JLabel sliderInfo = new JLabel(slider.getValue() + " - " + text);

        slider.addChangeListener(e -> sliderInfo.setText(slider.getValue() + " - " + text));

        slider.setBackground(Color.LIGHT_GRAY);

        panel.add(slider);
        panel.add(sliderInfo);
        panel.setBackground(Color.LIGHT_GRAY);

        add(panel);

        return slider;
    }

    private void createInputPanel(SwingView view, JSlider addressSlider, JSlider featuresSlider, JSlider radiusSlider) {
        JPanel inputPanel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField("");
        JButton startButton = new JButton("Search this address!");

        textField.setPreferredSize(TEXT_FIELD_SIZE);
        startButton.setEnabled(false);

        textField.getDocument().addDocumentListener(new DocumentListener() {
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
                startButton.setEnabled(!textField.getText().isEmpty());
                startButton.revalidate();
            }
        });

        startButton.addActionListener(
                e -> view.startAddressesSearchHttpRequest(
                        textField.getText(),
                        addressSlider.getValue(),
                        featuresSlider.getValue(),
                        radiusSlider.getValue()
                    )
        );

        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(startButton, BorderLayout.EAST);

        add(inputPanel);
    }
}
