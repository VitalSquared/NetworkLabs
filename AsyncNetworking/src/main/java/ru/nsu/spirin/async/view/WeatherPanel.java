package ru.nsu.spirin.async.view;

import lombok.Getter;
import ru.nsu.spirin.async.containers.Weather;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

public final class WeatherPanel {
    private final @Getter JPanel parentPanel;

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
        parentPanel = new JPanel(SwingUtils.createTableGridLayout(5, 3));
        this.parentPanel.setBackground(Color.CYAN);

        this.parentPanel.add(this.tempLabel);
        this.parentPanel.add(this.tempFeelsLikeLabel);
        this.parentPanel.add(this.tempRangeLabel);

        this.parentPanel.add(this.windSpeedLabel);
        this.parentPanel.add(this.windDirLabel);
        this.parentPanel.add(this.windGustLabel);

        this.parentPanel.add(this.pressureLabel);
        this.parentPanel.add(this.groundLevelLabel);
        this.parentPanel.add(this.seaLevelLabel);

        this.parentPanel.add(this.mainLabel);
        this.parentPanel.add(this.descriptionLabel);
        this.parentPanel.add(this.cloudsLabel);

        this.parentPanel.add(this.humidityLabel);
        this.parentPanel.add(this.visibilityLabel);
    }

    public void updateWeather(Weather weather) {
        if (null == weather) {
            for (var comp : this.parentPanel.getComponents()) {
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    label.setText("");
                }
            }
        }
        else {
            this.tempLabel.setText("Temperature: " + weather.getParameters().getTemperature() + " °C");
            this.tempFeelsLikeLabel.setText(
                    "Feels like: " + weather.getParameters().getFeelsLikeTemperature() + " °C");
            this.tempRangeLabel.setText("Range: [" + weather.getParameters().getMinTemperature() + ", " +
                                        weather.getParameters().getMaxTemperature() + "] °C");

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
        this.parentPanel.revalidate();
    }
}
