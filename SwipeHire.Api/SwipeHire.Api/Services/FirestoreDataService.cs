using System.Security.Cryptography;
using System.Text;
using Google.Api.Gax;
using Google.Cloud.Firestore;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Services;


public sealed class FirestoreDataService
{
    private readonly Lazy<FirestoreDb> _database;

    public FirestoreDataService(IConfiguration configuration)
    {
        var projectId = configuration["Firebase:ProjectId"];
        if (string.IsNullOrWhiteSpace(projectId))
            throw new InvalidOperationException("Firebase:ProjectId must be configured.");

        _database = new Lazy<FirestoreDb>(() =>
        {
            var builder = new FirestoreDbBuilder
            {
                ProjectId = projectId,
                EmulatorDetection = EmulatorDetection.EmulatorOrProduction
            };

            return builder.Build();
        });
    }

    private FirestoreDb Database => _database.Value;

    public async Task<IReadOnlyList<JobPostingDto>> GetJobsAsync(CancellationToken cancellationToken)
    {
        var snapshot = await Database.Collection("jobs").GetSnapshotAsync(cancellationToken);
        return snapshot.Documents
            .Select(document => document.ConvertTo<FirestoreJobDocument>().ToDto(document.Id))
            .OrderByDescending(job => job.Id)
            .ToList();
    }

    public async Task<string> CreateJobAsync(CreateJobPostingDto job, CancellationToken cancellationToken)
    {
        var document = Database.Collection("jobs").Document();
        await document.SetAsync(FirestoreJobDocument.From(job), cancellationToken: cancellationToken);
        return document.Id;
    }

    public async Task<bool> DeleteJobAsync(string id, CancellationToken cancellationToken)
    {
        var document = Database.Collection("jobs").Document(id);
        var snapshot = await document.GetSnapshotAsync(cancellationToken);
        if (!snapshot.Exists) return false;
        await document.DeleteAsync(cancellationToken: cancellationToken);
        return true;
    }

    public async Task<IReadOnlyList<StudentProfileDto>> GetStudentsAsync(CancellationToken cancellationToken)
    {
        var snapshot = await Database.Collection("students").GetSnapshotAsync(cancellationToken);
        return snapshot.Documents
            .Select(document => document.ConvertTo<FirestoreStudentDocument>().ToDto(document.Id))
            .OrderBy(student => student.Name)
            .ToList();
    }

    public async Task<string> UpsertStudentAsync(string? id, CreateStudentDto student, CancellationToken cancellationToken)
    {
        var document = string.IsNullOrWhiteSpace(id)
            ? Database.Collection("students").Document()
            : Database.Collection("students").Document(id);
        await document.SetAsync(FirestoreStudentDocument.From(student), SetOptions.MergeAll, cancellationToken);
        return document.Id;
    }

    public async Task<string> UpsertCompanyAsync(string? id, CreateCompanyDto company, CancellationToken cancellationToken)
    {
        var document = string.IsNullOrWhiteSpace(id)
            ? Database.Collection("companies").Document()
            : Database.Collection("companies").Document(id);
        await document.SetAsync(FirestoreCompanyDocument.From(company), SetOptions.MergeAll, cancellationToken);
        return document.Id;
    }

    public async Task<SwipeResponse> RecordSwipeAsync(SwipeRequest swipe, CancellationToken cancellationToken)
    {
        var swipeId = StableDocumentId($"{swipe.UserId}:{swipe.TargetId}");
        await Database.Collection("swipes").Document(swipeId).SetAsync(new Dictionary<string, object>
        {
            ["userId"] = swipe.UserId,
            ["targetId"] = swipe.TargetId,
            ["isLike"] = swipe.IsLike,
            ["createdAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);

        if (!swipe.IsLike) return new SwipeResponse(false, null);

        var matchId = StableDocumentId($"match:{swipe.UserId}:{swipe.TargetId}");
        await Database.Collection("matches").Document(matchId).SetAsync(new Dictionary<string, object>
        {
            ["id"] = matchId,
            ["participantIds"] = new[] { swipe.UserId, swipe.TargetId },
            ["targetId"] = swipe.TargetId,
            ["lastMessage"] = "You matched - start the conversation!",
            ["unread"] = true,
            ["createdAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);

        return new SwipeResponse(true, matchId);
    }

    public async Task<SavedItemsDto> GetSavedItemsAsync(string userId, CancellationToken cancellationToken)
    {
        var snapshot = await Database.Collection("users").Document(userId).GetSnapshotAsync(cancellationToken);
        if (!snapshot.Exists) return new SavedItemsDto([], []);

        return new SavedItemsDto(
            ReadStringList(snapshot, "savedJobIds"),
            ReadStringList(snapshot, "savedStudentIds")
        );
    }

    public async Task<SavedItemsDto> SetSavedItemAsync(
        string userId,
        string kind,
        string itemId,
        bool saved,
        CancellationToken cancellationToken)
    {
        var field = kind.ToLowerInvariant() switch
        {
            "job" or "jobs" => "savedJobIds",
            "student" or "students" or "candidate" or "candidates" => "savedStudentIds",
            _ => throw new ArgumentOutOfRangeException(nameof(kind), "Kind must be 'job' or 'student'.")
        };

        var update = saved ? FieldValue.ArrayUnion(itemId) : FieldValue.ArrayRemove(itemId);
        await Database.Collection("users").Document(userId).SetAsync(
            new Dictionary<string, object> { [field] = update, ["updatedAt"] = FieldValue.ServerTimestamp },
            SetOptions.MergeAll,
            cancellationToken
        );
        return await GetSavedItemsAsync(userId, cancellationToken);
    }

    public async Task<UserSettingsDto> GetUserSettingsAsync(string userId, CancellationToken cancellationToken)
    {
        var snapshot = await Database.Collection("users").Document(userId).GetSnapshotAsync(cancellationToken);
        if (!snapshot.Exists || !snapshot.TryGetValue<Dictionary<string, object>>("settings", out var settings))
            return new UserSettingsDto();

        return new UserSettingsDto(
            ReadBoolean(settings, "pushNotifications", true),
            ReadBoolean(settings, "matchAlerts", true),
            ReadBoolean(settings, "messageAlerts", true),
            ReadBoolean(settings, "profileVisible", true)
        );
    }

    public async Task<UserSettingsDto> SetUserSettingsAsync(
        string userId,
        UserSettingsDto settings,
        CancellationToken cancellationToken)
    {
        await Database.Collection("users").Document(userId).SetAsync(new Dictionary<string, object>
        {
            ["settings"] = new Dictionary<string, object>
            {
                ["pushNotifications"] = settings.PushNotifications,
                ["matchAlerts"] = settings.MatchAlerts,
                ["messageAlerts"] = settings.MessageAlerts,
                ["profileVisible"] = settings.ProfileVisible
            },
            ["updatedAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);
        return settings;
    }

    private static IReadOnlyList<string> ReadStringList(DocumentSnapshot snapshot, string field)
    {
        return snapshot.TryGetValue<List<string>>(field, out var values)
            ? values
            : [];
    }

    private static bool ReadBoolean(IReadOnlyDictionary<string, object> values, string field, bool defaultValue) =>
        values.TryGetValue(field, out var value) && value is bool boolean ? boolean : defaultValue;

    private static string StableDocumentId(string value)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(value));
        return Convert.ToHexString(hash).ToLowerInvariant()[..24];
    }
}

[FirestoreData]
public sealed class FirestoreJobDocument
{
    [FirestoreProperty("companyId")] public string CompanyId { get; set; } = "";
    [FirestoreProperty("company")] public string Company { get; set; } = "";
    [FirestoreProperty("role")] public string Role { get; set; } = "";
    [FirestoreProperty("location")] public string Location { get; set; } = "";
    [FirestoreProperty("workAddress")] public string WorkAddress { get; set; } = "";
    [FirestoreProperty("latitude")] public double Latitude { get; set; }
    [FirestoreProperty("longitude")] public double Longitude { get; set; }
    [FirestoreProperty("tags")] public List<string> Tags { get; set; } = [];
    [FirestoreProperty("blurb")] public string Blurb { get; set; } = "";
    [FirestoreProperty("logoInitials")] public string LogoInitials { get; set; } = "";
    [FirestoreProperty("remoteType")] public string RemoteType { get; set; } = "ON_SITE";
    [FirestoreProperty("salaryRange")] public string SalaryRange { get; set; } = "";
    [FirestoreProperty("createdAt")] public Timestamp? CreatedAt { get; set; }

    public JobPostingDto ToDto(string id) => new()
    {
        Id = id, Company = Company, Role = Role, Location = Location, WorkAddress = WorkAddress,
        Latitude = Latitude, Longitude = Longitude, Tags = Tags.ToList(), Blurb = Blurb,
        LogoInitials = LogoInitials, RemoteType = RemoteType, SalaryRange = SalaryRange
    };

    public static FirestoreJobDocument From(CreateJobPostingDto job) => new()
    {
        Company = job.Company.Trim(), Role = job.Role.Trim(), Location = job.Location.Trim(),
        WorkAddress = job.WorkAddress.Trim(), Latitude = job.Latitude, Longitude = job.Longitude,
        Tags = job.Tags.ToList(), Blurb = job.Blurb.Trim(), LogoInitials = job.LogoInitials.Trim(),
        RemoteType = job.RemoteType.Trim().ToUpperInvariant(), SalaryRange = job.SalaryRange.Trim(),
        CreatedAt = Timestamp.GetCurrentTimestamp()
    };
}

[FirestoreData]
public sealed class FirestoreStudentDocument
{
    [FirestoreProperty("name")] public string Name { get; set; } = "";
    [FirestoreProperty("course")] public string Course { get; set; } = "";
    [FirestoreProperty("year")] public string Year { get; set; } = "";
    [FirestoreProperty("skills")] public List<string> Skills { get; set; } = [];
    [FirestoreProperty("blurb")] public string Blurb { get; set; } = "";
    [FirestoreProperty("avatarInitials")] public string AvatarInitials { get; set; } = "";
    [FirestoreProperty("updatedAt")] public Timestamp? UpdatedAt { get; set; }

    public StudentProfileDto ToDto(string id) => new()
    {
        Id = id, Name = Name, Course = Course, Year = Year, Skills = Skills.ToList(),
        Blurb = Blurb, AvatarInitials = AvatarInitials
    };

    public static FirestoreStudentDocument From(CreateStudentDto student) => new()
    {
        Name = student.Name.Trim(), Course = student.Course.Trim(), Year = student.Year.Trim(),
        Skills = student.Skills.ToList(), Blurb = student.Blurb.Trim(), AvatarInitials = student.AvatarInitials.Trim(),
        UpdatedAt = Timestamp.GetCurrentTimestamp()
    };
}

[FirestoreData]
public sealed class FirestoreCompanyDocument
{
    [FirestoreProperty("name")] public string Name { get; set; } = "";
    [FirestoreProperty("industry")] public string Industry { get; set; } = "";
    [FirestoreProperty("location")] public string Location { get; set; } = "";
    [FirestoreProperty("description")] public string Description { get; set; } = "";
    [FirestoreProperty("logoInitials")] public string LogoInitials { get; set; } = "";
    [FirestoreProperty("updatedAt")] public Timestamp? UpdatedAt { get; set; }

    public static FirestoreCompanyDocument From(CreateCompanyDto company) => new()
    {
        Name = company.Name.Trim(), Industry = company.Industry.Trim(), Location = company.Location.Trim(),
        Description = company.Description.Trim(), LogoInitials = company.LogoInitials.Trim(),
        UpdatedAt = Timestamp.GetCurrentTimestamp()
    };
}
