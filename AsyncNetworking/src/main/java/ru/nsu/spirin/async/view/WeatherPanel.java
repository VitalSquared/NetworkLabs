package ru.nsu.spirin.async.view;

import ru.nsu.spirin.async.containers.Weather;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridLayout;

public final class WeatherPanel extends JPanel {

    private final JLabel tempLabel;
    private final JLabel humidityLabel;
    private final JLabel windSpeedLabel;
    private final JLabel windDirLabel;

    public WeatherPanel() {
        super(new GridLayout(4, 1));
        this.setBackground(Color.CYAN);

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
            this.tempLabel.setText("Temperature: " + weather.getParameters().getTemperature() + " °C");
            this.humidityLabel.setText("Humidity: " + weather.getParameters().getHumidity() + "");
            this.windSpeedLabel.setText("Wind speed: " + weather.getWind().getSpeed() + " m/s");
            this.windDirLabel.setText("Wind direction: " + weather.getWind().getDirection().toString());
        }
        this.revalidate();
    }
}
