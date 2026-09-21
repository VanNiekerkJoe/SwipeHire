using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;
using System.Security.Claims;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/swipes")]
[Authorize]
[EnableRateLimiting("general")]
public sealed class SwipesController(FirestoreDataService database) : ControllerBase
{
    [HttpGet("{userId}")]
    public async Task<ActionResult<IReadOnlyList<string>>> GetSwipedTargets(
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
            await database.GetSwipedTargetIdsAsync(
                authenticatedUserId,
                cancellationToken));
    }

    [HttpPost]
    public async Task<IActionResult> RecordSwipe(
        [FromBody] SwipeRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.UserId) ||
            string.IsNullOrWhiteSpace(request.TargetId) ||
            string.IsNullOrWhiteSpace(request.TargetUserId))
        {
            return ValidationProblem(
                "UserId, TargetId and TargetUserId are required.");
        }

        var authenticatedUserId =
            User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(authenticatedUserId))
            return Unauthorized();

        if (!string.Equals(
                request.UserId,
                authenticatedUserId,
                StringComparison.Ordinal))
        {
            return Forbid();
        }

        return Ok(
            await database.RecordSwipeAsync(
                request,
                cancellationToken));
    }

    [HttpDelete]
    public async Task<IActionResult> UndoSwipe(
        [FromQuery] string userId,
        [FromQuery] string targetId,
        [FromQuery] string targetUserId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId) ||
            string.IsNullOrWhiteSpace(targetId) ||
            string.IsNullOrWhiteSpace(targetUserId))
        {
            return ValidationProblem(
                "UserId, TargetId and TargetUserId are required.");
        }

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

        await database.DeleteSwipeAsync(
            authenticatedUserId,
            targetId,
            targetUserId,
            cancellationToken);

        return NoContent();
    }
}
