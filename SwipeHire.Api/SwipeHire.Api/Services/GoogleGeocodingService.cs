using System.Globalization;
using System.Text.Json;

namespace SwipeHire.Api.Services;

public sealed class GoogleGeocodingService(HttpClient httpClient, IConfiguration configuration, ILogger<GoogleGeocodingService> logger)
{
    private static readonly Dictionary<string, (double Latitude, double Longitude)> PrototypeLocations =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ["123 Rivonia Road, Sandton"] = (-26.1076, 28.0567),
            ["12 Bree Street, Cape Town"] = (-33.9249, 18.4241),
            ["45 Ontdekkers Road, Roodepoort"] = (-26.1006, 27.8563),
            ["88 Lynnwood Road, Pretoria"] = (-25.7479, 28.2293)
        };

    public async Task<(double Latitude, double Longitude)?> GeocodeAsync(string address, CancellationToken cancellationToken)
    {
        var apiKey = configuration["GoogleMaps:ApiKey"];
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            logger.LogWarning("GoogleMaps:ApiKey is not configured; using prototype coordinates for known demo addresses.");
            return PrototypeLocations.GetValueOrDefault(address.Trim());
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
