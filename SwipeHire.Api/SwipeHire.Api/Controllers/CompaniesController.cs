using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class CompaniesController : ControllerBase
    {
        [HttpPost]
        public IActionResult CreateCompany([FromBody] CreateCompanyDto dto)
        {
            var newId = $"c_{Guid.NewGuid().ToString()[..8]}";

            return CreatedAtAction(nameof(CreateCompany), new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Company profile created successfully."
            });
        }

        [HttpPut("{id}")]
        public IActionResult UpdateCompany(string id, [FromBody] UpdateCompanyDto dto)
        {
            return Ok(new ProfileResponseDto
            {
                Id = id,
                Success = true,
                Message = "Company profile updated successfully."
            });
        }
    }
}