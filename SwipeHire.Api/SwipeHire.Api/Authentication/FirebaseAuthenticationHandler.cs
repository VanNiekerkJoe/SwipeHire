using System.Security.Claims;
using System.Text.Encodings.Web;
using FirebaseAdmin.Auth;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace SwipeHire.Api.Authentication;

public sealed class FirebaseAuthenticationHandler
    : AuthenticationHandler<AuthenticationSchemeOptions>
{
    public FirebaseAuthenticationHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder)
        : base(options, logger, encoder)
    {
    }

    protected override async Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        var authorization = Request.Headers.Authorization.ToString();

        if (string.IsNullOrWhiteSpace(authorization))
        {
            Logger.LogWarning("No Authorization header.");
            return AuthenticateResult.NoResult();
        }

        if (!authorization.StartsWith(
                "Bearer ",
                StringComparison.OrdinalIgnoreCase))
        {
         
            return AuthenticateResult.NoResult();
        }

        var token = authorization["Bearer ".Length..].Trim();

        if (string.IsNullOrWhiteSpace(token))
            return AuthenticateResult.Fail("Missing Firebase ID token.");

        try
        {
            var decodedToken =
                await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(token);

            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, decodedToken.Uid),
                new Claim("firebase_uid", decodedToken.Uid)
            };

            var identity = new ClaimsIdentity(
                claims,
                Scheme.Name);

            var principal = new ClaimsPrincipal(identity);

            return AuthenticateResult.Success(
                new AuthenticationTicket(principal, Scheme.Name));
        }
        catch (Exception exception)
        {
            Logger.LogWarning(
                exception,
                "Firebase ID token validation failed.");

            return AuthenticateResult.Fail("Invalid Firebase ID token.");
        }
    }
}
