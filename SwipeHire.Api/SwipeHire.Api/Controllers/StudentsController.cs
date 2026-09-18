using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class StudentsController : ControllerBase
    {
        [HttpGet]
        public IActionResult GetStudents()
        {
            var students = new List<StudentProfileDto>
            {
                new StudentProfileDto
                {
                    Id = "s1",
                    Name = "Amahle Dlamini",
                    Course = "BSc Computer Science",
                    Year = "Final year",
                    Skills = new List<string> { "Java", "Kotlin", "REST APIs" },
                    Blurb = "Built two published Android apps.",
                    AvatarInitials = "AD"
                },
                new StudentProfileDto
                {
                    Id = "s2",
                    Name = "Sipho Nkosi",
                    Course = "BCAD Application Development",
                    Year = "Final year",
                    Skills = new List<string> { "C#", ".NET", "Azure" },
                    Blurb = "Interned on a cloud-hosted event management platform.",
                    AvatarInitials = "SN"
                }
            };

            return Ok(students);
        }

        [HttpPost]
        public IActionResult CreateStudent([FromBody] CreateStudentDto dto)
        {
            var newId = $"s_{Guid.NewGuid().ToString()[..8]}";

            // Persist to database/Firestore context here

            return CreatedAtAction(nameof(GetStudents), new ProfileResponseDto
            {
                Id = newId,
                Success = true,
                Message = "Student profile created successfully."
            });
        }

        [HttpPut("{id}")]
        public IActionResult UpdateStudent(string id, [FromBody] UpdateStudentDto dto)
        {
            // Update record in database/Firestore context here

            return Ok(new ProfileResponseDto
            {
                Id = id,
                Success = true,
                Message = "Student profile updated successfully."
            });
        }
    }
}