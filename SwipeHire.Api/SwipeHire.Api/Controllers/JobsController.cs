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

public sealed class JobsController(FirestoreDataService database) : ControllerBase
{
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> GetJobs(
        CancellationToken cancellationToken) =>
        Ok(await database.GetJobsAsync(cancellationToken));

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteJob(
        string id,
        CancellationToken cancellationToken)
    {
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        if (!await database.DeleteJobAsync(
                id,
                userId,
                cancellationToken))
        {
            return NotFound(new ProfileResponseDto
            {
                Id = id,
                Success = false,
                Message = $"Job posting '{id}' was not found."
            });
        }

        return Ok(new ProfileResponseDto
        {
            Id = id,
            Success = true,
            Message = "Job posting deleted."
        });
    }

    [HttpPost]
    public async Task<IActionResult> CreateJob(
        [FromBody] CreateJobPostingDto dto,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(dto.Company) ||
            string.IsNullOrWhiteSpace(dto.Role) ||
            string.IsNullOrWhiteSpace(dto.WorkAddress) ||
            dto.Latitude is < -90 or > 90 ||
            dto.Longitude is < -180 or > 180)
        {
            return ValidationProblem(
                "Company, role, a valid address and valid coordinates are required.");
        }

        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        dto.CompanyId = userId;

        var newJobId = await database.CreateJobAsync(
            dto,
            cancellationToken);

        return CreatedAtAction(
            nameof(GetJobs),
            new ProfileResponseDto
            {
                Id = newJobId,
                Success = true,
                Message = "Job posting created successfully."
            });
    }

}
