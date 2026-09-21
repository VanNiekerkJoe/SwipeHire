using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;
using System.Security.Claims;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
[EnableRateLimiting("general")]
public sealed class StudentsController(FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> GetStudents(
        CancellationToken cancellationToken) =>
        Ok(await database.GetStudentsAsync(cancellationToken));

    [HttpPost]
    public async Task<IActionResult> CreateStudent(
        [FromBody] CreateStudentDto dto,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(dto.Name) ||
            string.IsNullOrWhiteSpace(dto.Course))
        {
            return ValidationProblem("Name and course are required.");
        }

        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        var newId = await database.UpsertStudentAsync(
            userId,
            dto,
            cancellationToken);

        return CreatedAtAction(
            nameof(GetStudents),
            new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Student profile created successfully."
            });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateStudent(
        string id,
        [FromBody] CreateStudentDto dto,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(dto.Name) ||
            string.IsNullOrWhiteSpace(dto.Course))
        {
            return ValidationProblem("Name and course are required.");
        }

        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        if (!string.Equals(id, userId, StringComparison.Ordinal))
            return Forbid();

        await database.UpsertStudentAsync(
            userId,
            dto,
            cancellationToken);

        return Ok(
            new ProfileResponseDto
            {
                Id = userId,
                Success = true,
                Message = "Student profile updated successfully."
            });
    }

}
