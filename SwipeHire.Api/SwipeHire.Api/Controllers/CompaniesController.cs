using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public sealed class CompaniesController(FirestoreDataService database) : ControllerBase
    {
        [HttpPost]
        public async Task<IActionResult> CreateCompany([FromBody] CreateCompanyDto dto, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(dto.Name) || string.IsNullOrWhiteSpace(dto.Industry))
                return ValidationProblem("Company name and industry are required.");

            var newId = await database.UpsertCompanyAsync(null, dto, cancellationToken);
            return CreatedAtAction(nameof(CreateCompany), new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Company profile created successfully."
            });
        }

        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateCompany(string id, [FromBody] CreateCompanyDto dto, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(dto.Name) || string.IsNullOrWhiteSpace(dto.Industry))
                return ValidationProblem("Company name and industry are required.");

            await database.UpsertCompanyAsync(id, dto, cancellationToken);
            return Ok(new ProfileResponseDto { Id = id, Success = true, Message = "Company profile updated successfully." });
        }
    }
}