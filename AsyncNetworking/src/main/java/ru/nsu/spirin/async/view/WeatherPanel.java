package ru.nsu.spirin.async.view;

import ru.nsu.spirin.async.containers.Weather;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridLayout;

public final class WeatherPanel extends JPanel {
    private final JLabel tempLabel = new JLabel("");
    private final JLabel tempFeelsLikeLabel = new JLabel("");
    private final JLabel tempRangeLabel = new JLabel("");

    private final JLabel windSpeedLabel = new JLabel("");
    private final JLabel windDirLabel = new JLabel("");
    private final JLabel windGustLabel = new JLabel("");

    private final JLabel pressureLabel = new JLabel("");
    private final JLabel groundLevelLabel = new JLabel("");
    private final JLabel seaLevelLabel = new JLabel("");

    private final JLabel mainLabel = new JLabel("");
    private final JLabel descriptionLabel = new JLabel("");
    private final JLabel cloudsLabel = new JLabel("");

    private final JLabel humidityLabel = new JLabel("");
    private final JLabel visibilityLabel = new JLabel("");

    public WeatherPanel() {
        super(new GridLayout(5, 3, 0, 10));
        this.setBackground(Color.CYAN);

        add(this.tempLabel);
        add(this.tempFeelsLikeLabel);
        add(this.tempRangeLabel);

        add(this.windSpeedLabel);
        add(this.windDirLabel);
        add(this.windGustLabel);

        add(this.pressureLabel);
        add(this.groundLevelLabel);
        add(this.seaLevelLabel);

        add(this.mainLabel);
        add(this.descriptionLabel);
        add(this.cloudsLabel);

        add(this.humidityLabel);
        add(this.visibilityLabel);
    }

    public void updateWeather(Weather weather) {
        if (null == weather) {
            for (var comp : this.getComponents()) {
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel)comp;
                    label.setText("");
                }
            }
        }
        else {
            this.tempLabel.setText("Temperature: " + weather.getParameters().getTemperature() + " °C");
            this.tempFeelsLikeLabel.setText("Feels like: " + weather.getParameters().getFeelsLikeTemperature() + " °C");
            this.tempRangeLabel.setText("Range: [" + weather.getParameters().getMinTemperature() + ", " + weather.getParameters().getMaxTemperature() + "] °C");

            this.windDirLabel.setText("Wind direction: " + weather.getWind().getDirection().toString());
            this.windSpeedLabel.setText("Wind speed: " + weather.getWind().getSpeed() + " m/s");
            this.windGustLabel.setText("Wind gust: " + weather.getWind().getGust() + " m/s");

            this.pressureLabel.setText("Pressure: " + weather.getParameters().getPressure() + " hPa");
            this.groundLevelLabel.setText("Ground level: " + weather.getParameters().getGroundLevel() + " hPa");
            this.seaLevelLabel.setText("Sea level: " + weather.getParameters().getSeaLevel() + " hPa");

            this.mainLabel.setText(weather.getGeneral().get(0).getMain());
            this.descriptionLabel.setText(weather.getGeneral().get(0).getDescription());
            this.cloudsLabel.setText("Clouds: " + weather.getClouds().getClouds() + " %");

            this.humidityLabel.setText("Humidity: " + weather.getParameters().getHumidity() + "%");
            this.visibilityLabel.setText("Visibility: " + weather.getVisibility() + " m");
        }
        this.revalidate();
    }
}
