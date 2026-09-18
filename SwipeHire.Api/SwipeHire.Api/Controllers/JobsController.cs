using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class JobsController : ControllerBase
    {
        [HttpGet]
        public IActionResult GetJobs()
        {
            // Returns job feed (can be hooked up to EF Core DB Context or Firestore Admin SDK)
            var jobs = new List<JobPostingDto>
            {
                new JobPostingDto
                {
                    Id = "j1",
                    Company = "Nexora Fintech",
                    Role = "Junior Backend Developer",
                    Location = "Sandton, Johannesburg · Hybrid",
                    WorkAddress = "123 Rivonia Road, Sandton",
                    Latitude = -26.1076,
                    Longitude = 28.0567,
                    Tags = new List<string> { "C#", ".NET", "SQL" },
                    Blurb = "Join our payments team building secure APIs.",
                    LogoInitials = "NX",
                    RemoteType = "HYBRID",
                    SalaryRange = "R18k – R24k / month"
                },
                new JobPostingDto
                {
                    Id = "j2",
                    Company = "Kestrel Cloud",
                    Role = "Graduate Software Engineer",
                    Location = "Cape Town · Remote",
                    WorkAddress = "12 Bree Street, Cape Town",
                    Latitude = -33.9249,
                    Longitude = 18.4241,
                    Tags = new List<string> { "Kotlin", "Android", "Azure" },
                    Blurb = "Ship native Android features for a fast-growing platform.",
                    LogoInitials = "KC",
                    RemoteType = "REMOTE",
                    SalaryRange = "R22k – R28k / month"
                }
            };

            return Ok(jobs);
        }

        [HttpDelete("{id}")]
        public IActionResult DeleteJob(string id)
        {
            // Remove the job posting record from your database/Firestore context here

            return Ok(new ProfileResponseDto
            {
                Id = id,
                Success = true,
                Message = $"Job posting '{id}' successfully deleted."
            });
        }

        [HttpPost]
        public IActionResult CreateJob([FromBody] CreateJobPostingDto dto)
        {
            var newJobId = $"j_{Guid.NewGuid().ToString()[..8]}";

            // Persist the new job posting to your database/Firestore context here

            return CreatedAtAction(nameof(GetJobs), new ProfileResponseDto
            {
                Id = newJobId,
                Success = true,
                Message = "Job posting created successfully."
            });
        }
    }
}