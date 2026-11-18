package edu.uiuc.weatherlens.weatherinsights;

/**
 * Callback contract for async Weather Insights requests.
 *
 * @param <T> payload type returned on success
 */
public interface WeatherInsightsCallback<T> {

    /**
     * Called when an async Weather Insights operation succeeds.
     */
    void onSuccess(T result);

    /**
     * Called when an async Weather Insights operation fails in a controlled way.
     */
    void onError(WeatherInsightsException exception);
}

