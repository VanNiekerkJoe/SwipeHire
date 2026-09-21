using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Apis.Auth.OAuth2;

namespace SwipeHire.Api.Services;

public sealed class FirebaseAuthenticationService
{
    private readonly Lazy<FirebaseAuth> _auth;

    public FirebaseAuthenticationService(IConfiguration configuration)
    {
        var projectId = configuration["Firebase:ProjectId"];

        if (string.IsNullOrWhiteSpace(projectId))
            throw new InvalidOperationException(
                "Firebase:ProjectId must be configured.");

        _auth = new Lazy<FirebaseAuth>(() =>
        {
            if (FirebaseApp.DefaultInstance is null)
            {
                FirebaseApp.Create(new AppOptions
                {
                    ProjectId = projectId,
                    Credential = GoogleCredentialConfiguration.GetConfigured(configuration)
                        ?? GoogleCredential.GetApplicationDefault()
                });
            }

            return FirebaseAuth.DefaultInstance;
        });
    }

    public async Task<FirebaseToken> VerifyTokenAsync(string idToken)
    {
        return await _auth.Value.VerifyIdTokenAsync(idToken);
    }
}
