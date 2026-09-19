using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/users/{userId}/saved")]
public sealed class SavedController(FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetSaved(string userId, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId)) return ValidationProblem("UserId is required.");
        return Ok(await database.GetSavedItemsAsync(userId, cancellationToken));
    }

    [HttpPut("{kind}/{itemId}")]
    public async Task<IActionResult> SetSaved(
        string userId,
        string kind,
        string itemId,
        [FromBody] SetSavedItemRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId) || string.IsNullOrWhiteSpace(itemId))
            return ValidationProblem("UserId and itemId are required.");

        try
        {
            return Ok(await database.SetSavedItemAsync(userId, kind, itemId, request.Saved, cancellationToken));
        }
        catch (ArgumentOutOfRangeException exception)
        {
            return ValidationProblem(exception.Message);
        }
    }

}
