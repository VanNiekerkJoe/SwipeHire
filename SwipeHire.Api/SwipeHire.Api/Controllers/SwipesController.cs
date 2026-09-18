using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/swipes")]
public class SwipesController : ControllerBase
{
    [HttpPost]
    public IActionResult RecordSwipe([FromBody] SwipeRequest request)
    {
        bool isMatch = request.IsLike;
        string? matchId = isMatch ? $"m_{Guid.NewGuid().ToString()[..6]}" : null;

        return Ok(new SwipeResponse(isMatch, matchId));
    }
}