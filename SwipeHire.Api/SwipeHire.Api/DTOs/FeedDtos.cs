namespace SwipeHire.Api.DTOs
{
    public class JobPostingDto
    {
        public string Id { get; set; } = string.Empty;
        public string CompanyId { get; set; } = string.Empty;
        public string Company { get; set; } = string.Empty;
        public string Role { get; set; } = string.Empty;
        public string Location { get; set; } = string.Empty;
        public string WorkAddress { get; set; } = string.Empty;
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public List<string> Tags { get; set; } = new();
        public string Blurb { get; set; } = string.Empty;
        public string LogoInitials { get; set; } = string.Empty;
        public string RemoteType { get; set; } = string.Empty;
        public string SalaryRange { get; set; } = string.Empty;
    }

    public class StudentProfileDto
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Course { get; set; } = string.Empty;
        public string Year { get; set; } = string.Empty;
        public List<string> Skills { get; set; } = new();
        public string Blurb { get; set; } = string.Empty;
        public string AvatarInitials { get; set; } = string.Empty;
    }

    public class CreateJobPostingDto
    {
        public string CompanyId { get; set; } = string.Empty;
        public string Company { get; set; } = string.Empty;
        public string Role { get; set; } = string.Empty;
        public string Location { get; set; } = string.Empty;
        public string WorkAddress { get; set; } = string.Empty;
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public List<string> Tags { get; set; } = new();
        public string Blurb { get; set; } = string.Empty;
        public string LogoInitials { get; set; } = string.Empty;
        public string RemoteType { get; set; } = string.Empty;
        public string SalaryRange { get; set; } = string.Empty;
    }
}