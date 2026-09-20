using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/swipes")]
public sealed class SwipesController(FirestoreDataService database) : ControllerBase
{
    [HttpGet("{userId}")]
    public async Task<ActionResult<IReadOnlyList<string>>> GetSwipedTargets(
        string userId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId)) return ValidationProblem("UserId is required.");
        return Ok(await database.GetSwipedTargetIdsAsync(userId, cancellationToken));
    }

    [HttpPost]
    public async Task<IActionResult> RecordSwipe([FromBody] SwipeRequest request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.UserId) || string.IsNullOrWhiteSpace(request.TargetId) ||
            string.IsNullOrWhiteSpace(request.TargetUserId))
            return ValidationProblem("UserId, TargetId and TargetUserId are required.");

        return Ok(await database.RecordSwipeAsync(request, cancellationToken));
    }

    [HttpDelete]
    public async Task<IActionResult> UndoSwipe(
        [FromQuery] string userId,
        [FromQuery] string targetId,
        [FromQuery] string targetUserId,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(userId) || string.IsNullOrWhiteSpace(targetId) ||
            string.IsNullOrWhiteSpace(targetUserId))
            return ValidationProblem("UserId, TargetId and TargetUserId are required.");

        await database.DeleteSwipeAsync(userId, targetId, targetUserId, cancellationToken);
        return NoContent();
    }
}