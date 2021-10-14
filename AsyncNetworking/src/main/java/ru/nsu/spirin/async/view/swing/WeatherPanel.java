package ru.nsu.spirin.async.view.swing;

import ru.nsu.spirin.async.containers.Weather;
import ru.nsu.spirin.async.view.View;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridLayout;

public final class WeatherPanel extends JPanel {

    private final View view;
    private final JLabel tempLabel;
    private final JLabel humidityLabel;
    private final JLabel windSpeedLabel;
    private final JLabel windDirLabel;

    public WeatherPanel(View view) {
        super(new GridLayout(4, 1));
        this.setBackground(Color.CYAN);
        this.view = view;

        this.tempLabel = new JLabel("");
        this.humidityLabel = new JLabel("");
        this.windSpeedLabel = new JLabel("");
        this.windDirLabel = new JLabel("");

        add(this.tempLabel);
        add(this.humidityLabel);
        add(this.windSpeedLabel);
        add(this.windDirLabel);
    }

    public void updateWeather(Weather weather) {
        if (null == weather) {
            this.tempLabel.setText("");
            this.humidityLabel.setText("");
            this.windSpeedLabel.setText("");
            this.windDirLabel.setText("");
        }
        else {
            this.tempLabel.setText("Temperature: " + weather.getParameters().getTemperature() + "");
            this.humidityLabel.setText("Humidity: " + weather.getParameters().getHumidity() + "");
            this.windSpeedLabel.setText("Wind speed: " + weather.getWind().getSpeed() + "");
            this.windDirLabel.setText("Wind direction: " + weather.getWind().getDirection().toString());
        }
        this.invalidate();
        this.validate();
    }
}
