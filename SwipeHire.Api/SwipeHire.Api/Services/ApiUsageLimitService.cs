using Google.Api.Gax;
using Google.Cloud.Firestore;

namespace SwipeHire.Api.Services;

public sealed class ApiUsageLimitService
{
    private readonly FirestoreDb _database;
    private readonly ILogger<ApiUsageLimitService> _logger;

    private const int GeocodingDailyUserLimit = 50;
    private const int GeocodingDailyGlobalLimit = 100;
    private const int GeocodingMonthlyGlobalLimit = 1000;

    public ApiUsageLimitService(
        IConfiguration configuration,
        ILogger<ApiUsageLimitService> logger)
    {
        _logger = logger;

        var projectId = configuration["Firebase:ProjectId"];

        if (string.IsNullOrWhiteSpace(projectId))
        {
            throw new InvalidOperationException(
                "Firebase:ProjectId must be configured.");
        }

        var builder = new FirestoreDbBuilder
        {
            ProjectId = projectId,
            EmulatorDetection = EmulatorDetection.EmulatorOrProduction
        };

        builder.GoogleCredential = GoogleCredentialConfiguration.GetConfigured(configuration);

        _database = builder.Build();
    }

    public async Task<GeocodingQuotaResult> ConsumeGeocodingAsync(
        string userId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId))
            return GeocodingQuotaResult.Unavailable;

        var now = DateTimeOffset.UtcNow;

        var dayKey = now.ToString("yyyy-MM-dd");
        var monthKey = now.ToString("yyyy-MM");

        var userDailyReference = _database
            .Collection("apiUsage")
            .Document("geocoding_users")
            .Collection("daily")
            .Document($"{dayKey}_{userId}");

        var globalDailyReference = _database
            .Collection("apiUsage")
            .Document("geocoding_global")
            .Collection("daily")
            .Document(dayKey);

        var globalMonthlyReference = _database
            .Collection("apiUsage")
            .Document("geocoding_global")
            .Collection("monthly")
            .Document(monthKey);

        try
        {
            return await _database.RunTransactionAsync(
                async transaction =>
                {
                    var userSnapshot =
                        await transaction.GetSnapshotAsync(
                            userDailyReference);

                    var globalDailySnapshot =
                        await transaction.GetSnapshotAsync(
                            globalDailyReference);

                    var globalMonthlySnapshot =
                        await transaction.GetSnapshotAsync(
                            globalMonthlyReference);

                    var userCount =
                        GetCount(userSnapshot);

                    var globalDailyCount =
                        GetCount(globalDailySnapshot);

                    var globalMonthlyCount =
                        GetCount(globalMonthlySnapshot);

                    // Per-user daily limit.
                    if (userCount >= GeocodingDailyUserLimit)
                    {
                        _logger.LogWarning(
                            "User {UserId} reached the daily geocoding limit.",
                            userId);

                        return GeocodingQuotaResult.LimitExceeded;
                    }

                    // Global daily limit.
                    if (globalDailyCount >= GeocodingDailyGlobalLimit)
                    {
                        _logger.LogWarning(
                            "Global daily geocoding limit reached.");

                        return GeocodingQuotaResult.LimitExceeded;
                    }

                    // Global monthly limit.
                    if (globalMonthlyCount >= GeocodingMonthlyGlobalLimit)
                    {
                        _logger.LogWarning(
                            "Global monthly geocoding limit reached.");

                        return GeocodingQuotaResult.LimitExceeded;
                    }

                    transaction.Set(
                        userDailyReference,
                        new Dictionary<string, object>
                        {
                            ["count"] = userCount + 1,
                            ["updatedAt"] =
                                FieldValue.ServerTimestamp
                        },
                        SetOptions.MergeAll);

                    transaction.Set(
                        globalDailyReference,
                        new Dictionary<string, object>
                        {
                            ["count"] = globalDailyCount + 1,
                            ["updatedAt"] =
                                FieldValue.ServerTimestamp
                        },
                        SetOptions.MergeAll);

                    transaction.Set(
                        globalMonthlyReference,
                        new Dictionary<string, object>
                        {
                            ["count"] = globalMonthlyCount + 1,
                            ["updatedAt"] =
                                FieldValue.ServerTimestamp
                        },
                        SetOptions.MergeAll);

                    return GeocodingQuotaResult.Allowed;
                });
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception exception)
        {
            _logger.LogError(
                exception,
                "Unable to verify geocoding usage limits.");

            return GeocodingQuotaResult.Unavailable;
        }
    }

    private static long GetCount(DocumentSnapshot snapshot)
    {
        if (!snapshot.Exists)
            return 0;

        return snapshot.TryGetValue<long>("count", out var count)
            ? count
            : 0;
    }
}

public enum GeocodingQuotaResult
{
    Allowed,
    LimitExceeded,
    Unavailable
}
