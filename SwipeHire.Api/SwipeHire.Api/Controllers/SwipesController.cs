using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/swipes")]
public sealed class SwipesController(FirestoreDataService database) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> RecordSwipe([FromBody] SwipeRequest request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.UserId) || string.IsNullOrWhiteSpace(request.TargetId))
            return ValidationProblem("UserId and TargetId are required.");

        return Ok(await database.RecordSwipeAsync(request, cancellationToken));
    }
}