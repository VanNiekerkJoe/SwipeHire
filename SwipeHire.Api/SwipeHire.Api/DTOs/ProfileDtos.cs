namespace SwipeHire.Api.DTOs
{
    public class CreateStudentDto
    {
        public string Name { get; set; } = string.Empty;
        public string Course { get; set; } = string.Empty;
        public string Year { get; set; } = string.Empty;
        public List<string> Skills { get; set; } = new();
        public string Blurb { get; set; } = string.Empty;
        public string AvatarInitials { get; set; } = string.Empty;
    }

    public class UpdateStudentDto : CreateStudentDto { }

    public class CreateCompanyDto
    {
        public string Name { get; set; } = string.Empty;
        public string Industry { get; set; } = string.Empty;
        public string Location { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string LogoInitials { get; set; } = string.Empty;
        public List<string> HiringFor { get; set; } = new();
    }

    public class UpdateCompanyDto : CreateCompanyDto { }

    public class ProfileResponseDto
    {
        public string Id { get; set; } = string.Empty;
        public string Message { get; set; } = string.Empty;
        public bool Success { get; set; }
    }
}