using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Apis.Auth.OAuth2;

namespace SwipeHire.Api.Services;

public sealed class FirebaseAuthenticationService
{
    private readonly FirebaseAuth _auth;

    public FirebaseAuthenticationService(IConfiguration configuration)
    {
        var projectId = configuration["Firebase:ProjectId"];

        if (string.IsNullOrWhiteSpace(projectId))
            throw new InvalidOperationException(
                "Firebase:ProjectId must be configured.");

        if (FirebaseApp.DefaultInstance is null)
        {
            FirebaseApp.Create(new AppOptions
            {
                ProjectId = projectId,
                Credential = GoogleCredential.GetApplicationDefault()
            });
        }

        _auth = FirebaseAuth.DefaultInstance;
    }

    public async Task<FirebaseToken> VerifyTokenAsync(string idToken)
    {
        return await _auth.VerifyIdTokenAsync(idToken);
    }
}
