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
    [EnableRateLimiting("public")]
    public async Task<IActionResult> GetStudents(
        CancellationToken cancellationToken)
    {
        var students = await database.GetStudentsAsync(cancellationToken);
        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        return students is null
            ? Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Students temporarily unavailable",
                detail: "The database request did not complete. Please retry.")
            : Ok(students);
    }

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

        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        if (newId is null)
            return Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Student profile temporarily unavailable",
                detail: "The database request did not complete. Please retry.");

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

        var updatedId = await database.UpsertStudentAsync(
            userId,
            dto,
            cancellationToken);

        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        if (updatedId is null)
            return Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Student profile temporarily unavailable",
                detail: "The database request did not complete. Please retry.");

        return Ok(
            new ProfileResponseDto
            {
                Id = userId,
                Success = true,
                Message = "Student profile updated successfully."
            });
    }

}
