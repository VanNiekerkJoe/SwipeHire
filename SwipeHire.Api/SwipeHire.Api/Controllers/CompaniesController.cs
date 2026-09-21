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
public sealed class CompaniesController(FirestoreDataService database) : ControllerBase
{
    [HttpGet("{companyId}/jobs")]
    [AllowAnonymous]
    [EnableRateLimiting("public")]
    public async Task<IActionResult> GetJobs(
        string companyId,
        CancellationToken cancellationToken)
    {
        var jobs = await database.GetCompanyJobsAsync(companyId, cancellationToken);
        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        return jobs is null
            ? Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Company jobs temporarily unavailable",
                detail: "The database request did not complete. Please retry.")
            : Ok(jobs);
    }

    [HttpPost]
    public async Task<IActionResult> CreateCompany(
        [FromBody] CreateCompanyDto dto,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(dto.Name) ||
            string.IsNullOrWhiteSpace(dto.Industry))
        {
            return ValidationProblem(
                "Company name and industry are required.");
        }

        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        var newId = await database.UpsertCompanyAsync(
            userId,
            dto,
            cancellationToken);

        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        if (newId is null)
            return Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Company profile temporarily unavailable",
                detail: "The database request did not complete. Please retry.");

        return CreatedAtAction(
            nameof(CreateCompany),
            new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Company profile created successfully."
            });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateCompany(
        string id,
        [FromBody] CreateCompanyDto dto,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(dto.Name) ||
            string.IsNullOrWhiteSpace(dto.Industry))
        {
            return ValidationProblem(
                "Company name and industry are required.");
        }

        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        if (!string.Equals(id, userId, StringComparison.Ordinal))
            return Forbid();

        var updatedId = await database.UpsertCompanyAsync(
            userId,
            dto,
            cancellationToken);

        if (cancellationToken.IsCancellationRequested) return new EmptyResult();
        if (updatedId is null)
            return Problem(statusCode: StatusCodes.Status503ServiceUnavailable,
                title: "Company profile temporarily unavailable",
                detail: "The database request did not complete. Please retry.");

        return Ok(
            new ProfileResponseDto
            {
                Id = userId,
                Success = true,
                Message = "Company profile updated successfully."
            });
    }
}
