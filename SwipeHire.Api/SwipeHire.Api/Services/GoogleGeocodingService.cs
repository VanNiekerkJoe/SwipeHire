using System.Text.Json;

namespace SwipeHire.Api.Services;

public sealed class GoogleGeocodingService(HttpClient httpClient, IConfiguration configuration, ILogger<GoogleGeocodingService> logger)
{
    public async Task<(double Latitude, double Longitude)?> GeocodeAsync(string address, CancellationToken cancellationToken)
    {
        var apiKey = configuration["GoogleMaps:ApiKey"];
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            logger.LogError("GoogleMaps:ApiKey is not configured; geocoding cannot run.");
            return null;
        }

        var url = $"https://maps.googleapis.com/maps/api/geocode/json?address={Uri.EscapeDataString(address)}&key={Uri.EscapeDataString(apiKey)}";
        using var response = await httpClient.GetAsync(url, cancellationToken);
        response.EnsureSuccessStatusCode();
        using var json = JsonDocument.Parse(await response.Content.ReadAsStreamAsync(cancellationToken));
        var results = json.RootElement.GetProperty("results");
        if (results.GetArrayLength() == 0) return null;
        var location = results[0].GetProperty("geometry").GetProperty("location");
        return (location.GetProperty("lat").GetDouble(), location.GetProperty("lng").GetDouble());
    }
}
