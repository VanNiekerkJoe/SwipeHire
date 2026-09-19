using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public sealed class JobsController(FirestoreDataService database) : ControllerBase
    {
        [HttpGet]
        public async Task<IActionResult> GetJobs(CancellationToken cancellationToken) =>
            Ok(await database.GetJobsAsync(cancellationToken));

        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteJob(string id, CancellationToken cancellationToken)
        {
            if (!await database.DeleteJobAsync(id, cancellationToken))
            {
                return NotFound(new ProfileResponseDto
                {
                    Id = id,
                    Success = false,
                    Message = $"Job posting '{id}' was not found."
                });
            }

            return Ok(new ProfileResponseDto { Id = id, Success = true, Message = "Job posting deleted." });
        }

        [HttpPost]
        public async Task<IActionResult> CreateJob([FromBody] CreateJobPostingDto dto, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(dto.Company) || string.IsNullOrWhiteSpace(dto.Role) ||
                string.IsNullOrWhiteSpace(dto.WorkAddress) || dto.Latitude is < -90 or > 90 ||
                dto.Longitude is < -180 or > 180)
            {
                return ValidationProblem("Company, role, a valid address and valid coordinates are required.");
            }

            var newJobId = await database.CreateJobAsync(dto, cancellationToken);
            return CreatedAtAction(nameof(GetJobs), new ProfileResponseDto
            {
                Id = newJobId,
                Success = true,
                Message = "Job posting created successfully."
            });
        }
    }
}