using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/users/{userId}/settings")]
public sealed class UserSettingsController(FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetSettings(string userId, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId)) return ValidationProblem("UserId is required.");
        return Ok(await database.GetUserSettingsAsync(userId, cancellationToken));
    }

    [HttpPut]
    public async Task<IActionResult> SetSettings(
        string userId,
        [FromBody] UserSettingsDto settings,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId)) return ValidationProblem("UserId is required.");
        return Ok(await database.SetUserSettingsAsync(userId, settings, cancellationToken));
    }
}
