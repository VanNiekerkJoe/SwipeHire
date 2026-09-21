using System.Collections.Concurrent;
using System.Text.Json;

namespace SwipeHire.Api.Services;

public sealed class GoogleGeocodingService(
    HttpClient httpClient,
    IConfiguration configuration,
    ILogger<GoogleGeocodingService> logger,
    ApiUsageLimitService usageLimitService)
{
    private sealed record CachedLocation(
        double Latitude,
        double Longitude,
        DateTimeOffset ExpiresAt);

    private static readonly ConcurrentDictionary<string, CachedLocation>
        Cache = new();

    private static readonly TimeSpan CacheDuration =
        TimeSpan.FromHours(24);

    public async Task<(double Latitude, double Longitude)?> GeocodeAsync(
        string address,
        string userId,
        CancellationToken cancellationToken)
    {
        var normalizedAddress = NormalizeAddress(address);

        // 1. CACHE CHECK

        if (Cache.TryGetValue(normalizedAddress, out var cached))
        {
            if (cached.ExpiresAt > DateTimeOffset.UtcNow)
            {
                logger.LogInformation(
                    "Geocoding cache hit.");

                return (
                    cached.Latitude,
                    cached.Longitude);
            }

            Cache.TryRemove(
                normalizedAddress,
                out _);
        }

        // 2. API KEY CHECK
        var apiKey =
            configuration["GoogleMaps:ApiKey"];

        if (string.IsNullOrWhiteSpace(apiKey))
        {
            logger.LogError(
                "GoogleMaps:ApiKey is not configured; " +
                "geocoding cannot run.");

            return null;
        }

        // 3. QUOTA CHECK
        var allowed =
            await usageLimitService.TryConsumeGeocodingAsync(
                userId,
                cancellationToken);

        if (!allowed)
        {
            throw new GeocodingLimitExceededException();
        }

        // 4. GOOGLE REQUEST

        var url =
            "https://maps.googleapis.com/maps/api/geocode/json" +
            $"?address={Uri.EscapeDataString(normalizedAddress)}" +
            $"&key={Uri.EscapeDataString(apiKey)}";

        using var response =
            await httpClient.GetAsync(
                url,
                cancellationToken);

        response.EnsureSuccessStatusCode();

        using var json =
            JsonDocument.Parse(
                await response.Content.ReadAsStreamAsync(
                    cancellationToken));

        var results =
            json.RootElement.GetProperty("results");

        if (results.GetArrayLength() == 0)
        {
            logger.LogInformation(
                "Google returned no geocoding result.");

            return null;
        }

        var location =
            results[0]
                .GetProperty("geometry")
                .GetProperty("location");

        var latitude =
            location.GetProperty("lat").GetDouble();

        var longitude =
            location.GetProperty("lng").GetDouble();

        // 5. CACHE SUCCESSFUL RESULT

        Cache[normalizedAddress] =
            new CachedLocation(
                latitude,
                longitude,
                DateTimeOffset.UtcNow.Add(CacheDuration));

        logger.LogInformation(
            "Geocoding successful and result cached.");

        return (latitude, longitude);
    }

    private static string NormalizeAddress(
        string address)
    {
        return string.Join(
            ' ',
            address
                .Trim()
                .Split(
                    (char[]?)null,
                    StringSplitOptions.RemoveEmptyEntries));
    }
}

public sealed class GeocodingLimitExceededException
    : Exception
{
}
