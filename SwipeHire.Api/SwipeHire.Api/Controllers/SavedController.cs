using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;
using System.Security.Claims;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/users/{userId}/saved")]
[Authorize]
[EnableRateLimiting("general")]
public sealed class SavedController(FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetSaved(
        string userId,
        CancellationToken cancellationToken)
    {
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
            await database.GetSavedItemsAsync(
                authenticatedUserId,
                cancellationToken));
    }

    [HttpPut("{kind}/{itemId}")]
    public async Task<IActionResult> SetSaved(
        string userId,
        string kind,
        string itemId,
        [FromBody] SetSavedItemRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(itemId))
            return ValidationProblem("ItemId is required.");

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

        try
        {
            return Ok(
                await database.SetSavedItemAsync(
                    authenticatedUserId,
                    kind,
                    itemId,
                    request.Saved,
                    cancellationToken));
        }
        catch (ArgumentOutOfRangeException exception)
        {
            return ValidationProblem(exception.Message);
        }
    }
}
