using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;
using System.Security.Claims;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/users/{userId}/settings")]
[Authorize]
[EnableRateLimiting("general")]
public sealed class UserSettingsController(
    FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetSettings(
        string userId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId))
            return ValidationProblem("UserId is required.");

        var authenticatedUserId =
            User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(authenticatedUserId))
            return Unauthorized();

        if (!string.Equals(
                userId,
                authenticatedUserId,
                StringComparison.Ordinal))
        {
            return Forbid();
        }

        return Ok(
            await database.GetUserSettingsAsync(
                authenticatedUserId,
                cancellationToken));
    }

    [HttpPut]
    public async Task<IActionResult> SetSettings(
        string userId,
        [FromBody] UserSettingsDto settings,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId))
            return ValidationProblem("UserId is required.");

        var authenticatedUserId =
            User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(authenticatedUserId))
            return Unauthorized();

        if (!string.Equals(
                userId,
                authenticatedUserId,
                StringComparison.Ordinal))
        {
            return Forbid();
        }

        return Ok(
            await database.SetUserSettingsAsync(
                authenticatedUserId,
                settings,
                cancellationToken));
    }
}
