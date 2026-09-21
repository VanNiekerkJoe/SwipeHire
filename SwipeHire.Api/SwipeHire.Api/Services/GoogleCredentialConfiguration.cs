using Google.Apis.Auth.OAuth2;

namespace SwipeHire.Api.Services;

internal static class GoogleCredentialConfiguration
{
    public static GoogleCredential? GetConfigured(IConfiguration configuration)
    {
        var json = configuration["GoogleCredentials:Json"];
        if (string.IsNullOrWhiteSpace(json))
            return null;

        return CredentialFactory.FromJson<ServiceAccountCredential>(json)
            .ToGoogleCredential();
    }
}
