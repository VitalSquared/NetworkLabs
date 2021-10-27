package ru.nsu.spirin.async.view;

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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SwingView {
    private static final Logger logger = Logger.getLogger(SwingView.class);

    private static final String WINDOW_TITLE = "Location Viewer";
    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(960, 540);

    private final JFrame gameFrame;

    private final AddressListPanel addressListPanel;
    private final WeatherPanel weatherPanel;
    private final FeaturesPanel featuresPanel;

    public SwingView() {
        this.gameFrame = new JFrame(WINDOW_TITLE);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.addressListPanel = new AddressListPanel();
        this.weatherPanel = new WeatherPanel();
        this.featuresPanel = new FeaturesPanel();

        JPanel mainPanel = new JPanel(new BorderLayout(25, 0));
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        this.gameFrame.add(mainPanel);
        this.gameFrame.setVisible(true);
    }

    private JPanel createLeftPanel() {
        AddressInputPanel addressInputPanel = new AddressInputPanel(this);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 25));
        leftPanel.add(addressInputPanel, BorderLayout.NORTH);
        leftPanel.add(this.addressListPanel, BorderLayout.CENTER);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(0, 25));
        rightPanel.add(this.weatherPanel, BorderLayout.NORTH);
        rightPanel.add(this.featuresPanel, BorderLayout.CENTER);
        return rightPanel;
    }

    public void startAddressesSearchHttpRequest(String address, int maxAddressesNumber, int maxFeaturesNumber, int radius) {
        CompletableFuture.supplyAsync(() -> {
            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createAddressesRequest(address, maxAddressesNumber));

            if (null == response) {
                return null;
            }

            try (response) {
                String jsonString = Objects.requireNonNull(response.body()).string();
                return JsonParserWrapper.parse(jsonString, AddressList.class).getAddresses();
            }
            catch (Exception exception) {
                logger.error(exception.getLocalizedMessage());
                return null;
            }
        })
        .thenAcceptAsync(addresses -> {
            SwingUtilities.invokeLater(() -> {
                addressListPanel.clearAll();
                weatherPanel.updateWeather(null);
                featuresPanel.updateFeatures(null);
                gameFrame.revalidate();


                if (null == addresses || addresses.isEmpty()) {
                    addressListPanel.addAddress(new JLabel("Failed to get any possible addresses"));
                }
                else {
                    for (var entry : addresses) {
                        JButton button = new JButton(constructAddressButtonText(entry));
                        button.setHorizontalAlignment(SwingConstants.CENTER);
                        button.addActionListener(e -> {
                            startWeatherSearchHttpRequest(entry.getPosition());
                            startFeaturesSearchHttpRequest(entry.getPosition(), maxFeaturesNumber, radius);
                        });
                        addressListPanel.addAddress(button);
                    }
                }
                gameFrame.revalidate();
            });
        });
    }

    private void startWeatherSearchHttpRequest(GeoPosition position) {
        CompletableFuture.supplyAsync(() -> {
            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createWeatherRequest(position));

            if (null == response) {
                return null;
            }

            try (response) {
                String jsonString = Objects.requireNonNull(response.body()).string();
                return JsonParserWrapper.parse(jsonString, Weather.class);
            }
            catch (Exception exception) {
                logger.error(exception.getLocalizedMessage());
                return null;
            }
        })
        .thenAcceptAsync(weather -> {
            SwingUtilities.invokeLater(() -> {
                weatherPanel.updateWeather(weather);
                gameFrame.revalidate();
            });
        });
    }

    private void startFeaturesSearchHttpRequest(GeoPosition position, int maxFeaturesNumber, int radius) {
        CompletableFuture.supplyAsync(() -> {
            Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createFeaturesRequest(position, radius, maxFeaturesNumber));

            if (null == response) {
                return null;
            }

            try (response) {
                String jsonString = Objects.requireNonNull(response.body()).string();
                return JsonParserWrapper.parse(jsonString, new TypeReference<List<Feature>>() {});
            }
            catch (Exception exception) {
                logger.error(exception.getLocalizedMessage());
                return null;
            }
        })
        .thenAcceptAsync(features -> {
            SwingUtilities.invokeLater(() -> {
                featuresPanel.updateFeatures(features);
                gameFrame.revalidate();
            });

            if (null == features) {
                return;
            }

            for (var feature : features) {
                CompletableFuture.supplyAsync(() -> {
                    Response response = APIRequestGenerator.createResponse(APIRequestGenerator.createFeatureDescriptionRequest(feature.getXid()));

                    FeatureDescription nullDescription = new FeatureDescription();
                    nullDescription.setNull(true);
                    nullDescription.setXid(feature.getXid());

                    if (null == response) {
                        return nullDescription;
                    }

                    try (response) {
                        String jsonString = Objects.requireNonNull(response.body()).string();
                        return JsonParserWrapper.parse(jsonString, FeatureDescription.class);
                    }
                    catch (Exception exception) {
                        logger.error(exception.getLocalizedMessage());
                        return nullDescription;
                    }
                })
                .thenAcceptAsync(description -> {
                    featuresPanel.updateDescription(description.getXid(), description.isNull() ? null : description);
                    gameFrame.revalidate();
                });
            }
        });
    }

    private String constructAddressButtonText(AddressList.Address address) {
        return String.format(
                "<html><body style='text-align: center'>%s<br>%s</body></html>",
                address.getName(),
                Stream.of(
                       address.getCountry(),
                       address.getState(),
                       address.getCity(),
                       address.getStreet(),
                       address.getHouseNumber()
                    ).filter(s -> (null != s) && !s.isEmpty()).collect(Collectors.joining(", "))
        );
    }
}
