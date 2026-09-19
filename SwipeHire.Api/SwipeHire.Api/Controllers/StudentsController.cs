using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public sealed class StudentsController(FirestoreDataService database) : ControllerBase
    {
        [HttpGet]
        public async Task<IActionResult> GetStudents(CancellationToken cancellationToken) =>
            Ok(await database.GetStudentsAsync(cancellationToken));

        [HttpPost]
        public async Task<IActionResult> CreateStudent([FromBody] CreateStudentDto dto, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(dto.Name) || string.IsNullOrWhiteSpace(dto.Course))
                return ValidationProblem("Name and course are required.");

            var newId = await database.UpsertStudentAsync(null, dto, cancellationToken);
            return CreatedAtAction(nameof(GetStudents), new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Student profile created successfully."
            });
        }

        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateStudent(string id, [FromBody] CreateStudentDto dto, CancellationToken cancellationToken)
        {
            if (string.IsNullOrWhiteSpace(dto.Name) || string.IsNullOrWhiteSpace(dto.Course))
                return ValidationProblem("Name and course are required.");

            await database.UpsertStudentAsync(id, dto, cancellationToken);
            return Ok(new ProfileResponseDto { Id = id, Success = true, Message = "Student profile updated successfully." });
        }
    }
}