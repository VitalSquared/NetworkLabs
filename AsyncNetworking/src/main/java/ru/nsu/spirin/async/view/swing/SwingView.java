package ru.nsu.spirin.async.view.swing;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.Response;
import org.apache.log4j.Logger;
import ru.nsu.spirin.async.containers.AddressList;
import ru.nsu.spirin.async.containers.Feature;
import ru.nsu.spirin.async.containers.FeatureDescription;
import ru.nsu.spirin.async.containers.Weather;
import ru.nsu.spirin.async.utils.APIRequestGenerator;
import ru.nsu.spirin.async.utils.GeoPosition;
import ru.nsu.spirin.async.utils.JsonParserWrapper;
import ru.nsu.spirin.async.view.View;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SwingView extends View {
    private static final Logger logger = Logger.getLogger(SwingView.class);

    private static final String WINDOW_TITLE = "Location Viewer";
    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(960, 540);

    private final JFrame gameFrame;

    private final JPanel addressListPanel;
    private final WeatherPanel weatherPanel;
    private final FeaturesPanel featuresPanel;

    public SwingView() {
        this.gameFrame = new JFrame(WINDOW_TITLE);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));

        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new BorderLayout());

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        AddressInputPanel addressInputPanel = new AddressInputPanel(this);
        this.addressListPanel = new JPanel(new GridLayout(5, 1));
        this.weatherPanel = new WeatherPanel(this);

        JPanel leftBottomPanel = new JPanel(new GridLayout(2, 1));

        leftBottomPanel.add(this.addressListPanel);
        leftBottomPanel.add(this.weatherPanel);

        leftPanel.add(addressInputPanel, BorderLayout.NORTH);
        leftPanel.add(leftBottomPanel, BorderLayout.CENTER);

        this.featuresPanel = new FeaturesPanel(this);

        rightPanel.add(this.featuresPanel);

        this.gameFrame.add(mainPanel);
        this.gameFrame.setVisible(true);
    }

    @Override
    public void startAddressesSearchHttpRequest(String address) {
        CompletableFuture.supplyAsync(() -> {
            addressListPanel.removeAll();
            weatherPanel.updateWeather(null);
            featuresPanel.updateFeatures(null);
            gameFrame.invalidate();
            gameFrame.validate();

            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createAddressesRequest(address, 5));

            if (200 != response.code() || null == response.body()) {
                response.close();
                return null;
            }

            try (response) {
                String jsonString = response.body().string();
                return JsonParserWrapper.parse(jsonString, AddressList.class).getAddresses();
            }
            catch (IOException e) {
                logger.error(e.getLocalizedMessage());
                return null;
            }
        })
        .thenAccept(addresses -> {
            if (null == addresses || 0 == addresses.size()) {
                addressListPanel.add(new JLabel("Failed to get any possible addresses"));
            }
            else {
                for (var entry : addresses) {
                    JButton button = new JButton(generateNameForAddressButton(entry));
                    button.addActionListener(e -> {
                        weatherPanel.updateWeather(null);
                        featuresPanel.updateFeatures(null);
                        gameFrame.invalidate();
                        gameFrame.validate();
                        startWeatherSearchHttpRequest(entry.getPosition());
                        startFeaturesSearchHttpRequest(entry.getPosition());
                    });
                    addressListPanel.add(button);
                }
            }
            gameFrame.validate();
        });
    }

    private void startWeatherSearchHttpRequest(GeoPosition position) {
        CompletableFuture.supplyAsync(() -> {
            gameFrame.invalidate();
            gameFrame.validate();

            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createWeatherRequest(position));

            if (200 != response.code() || null == response.body()) {
                response.close();
                return null;
            }

            try (response) {
                String jsonString = response.body().string();
                return JsonParserWrapper.parse(jsonString, Weather.class);
            }
            catch (IOException e) {
                logger.error(e.getLocalizedMessage());
                return null;
            }
        })
        .thenAccept(weather -> {
            weatherPanel.updateWeather(weather);
            gameFrame.invalidate();
            gameFrame.validate();
        });
    }

    private void startFeaturesSearchHttpRequest(GeoPosition position) {
        CompletableFuture.supplyAsync(() -> {
            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createFeaturesRequest(position, 1000, 10));

            if (200 != response.code() || null == response.body()) {
                response.close();
                return null;
            }

            try (response) {
                String jsonString = response.body().string();
                return JsonParserWrapper.parse(jsonString, new TypeReference<List<Feature>>() {});
            }
            catch (IOException e) {
                logger.error(e.getLocalizedMessage());
                return null;
            }
        })
        .thenApply(features -> {
            featuresPanel.updateFeatures(features);
            gameFrame.invalidate();
            gameFrame.validate();
            return features;
        })
        .thenAccept(features -> {
            for (var feature : features) {
                CompletableFuture.supplyAsync(() -> {
                    Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createFeatureDescriptionRequest(feature.getXid()));

                    FeatureDescription nullDesc = new FeatureDescription();
                    nullDesc.setName(null);
                    nullDesc.setXid(feature.getXid());

                    if (200 != response.code() || null == response.body()) {
                        response.close();
                        return nullDesc;
                    }

                    try (response) {
                        String jsonString = response.body().string();
                        return JsonParserWrapper.parse(jsonString, FeatureDescription.class);
                    }
                    catch (IOException e) {
                        logger.error(e.getLocalizedMessage());
                        return nullDesc;
                    }
                })
                .thenAccept(description -> {
                    featuresPanel.updateDescription(description.getXid(), description);
                    gameFrame.invalidate();
                    gameFrame.validate();
                });
            }
        });
    }

    private String generateNameForAddressButton(AddressList.Address address) {
        return address.getName() + ", " + address.getCountry();
    }
}
