using System.Security.Cryptography;
using System.Text;
using Google.Api.Gax;
using Google.Cloud.Firestore;
using Grpc.Core;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Services;

public sealed class FirestoreDataService
{
    private readonly Lazy<FirestoreDb> _database;
    private readonly ILogger<FirestoreDataService> _logger;

    public FirestoreDataService(
        IConfiguration configuration,
        ILogger<FirestoreDataService> logger)
    {
        _logger = logger;
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
        try
        {
            var snapshot = await Database.Collection("jobs").GetSnapshotAsync(cancellationToken);
            return snapshot.Documents
                .Select(document => (Document: document, Data: document.ConvertTo<FirestoreJobDocument>()))
                .Where(item => item.Data.ProfileVisible)
                .Select(item => item.Data.ToDto(item.Document.Id))
                .Where(job => !string.IsNullOrWhiteSpace(job.CompanyId))
                .OrderByDescending(job => job.Id)
                .ToList();
        }
        catch (Exception exception) when (IsExpectedCancellation(exception, cancellationToken))
        {
            return [];
        }
    }

    public async Task<IReadOnlyList<JobPostingDto>> GetCompanyJobsAsync(string companyId, CancellationToken cancellationToken)
    {
        try
        {
            var snapshot = await Database.Collection("jobs")
                .WhereEqualTo("companyId", companyId)
                .GetSnapshotAsync(cancellationToken);
            return snapshot.Documents
                .Select(document => document.ConvertTo<FirestoreJobDocument>().ToDto(document.Id))
                .ToList();
        }
        catch (Exception exception) when (IsExpectedCancellation(exception, cancellationToken))
        {
            return [];
        }
    }

    public async Task<string> CreateJobAsync(CreateJobPostingDto job, CancellationToken cancellationToken)
    {
        var document = Database.Collection("jobs").Document();
        var settings = await GetUserSettingsAsync(job.CompanyId, cancellationToken);
        var jobDocument = FirestoreJobDocument.From(job);
        jobDocument.ProfileVisible = settings.ProfileVisible;
        await document.SetAsync(jobDocument, cancellationToken: cancellationToken);
        return document.Id;
    }

    public async Task<bool> DeleteJobAsync(string id, string companyId, CancellationToken cancellationToken)
    {
        var document = Database.Collection("jobs").Document(id);
        var snapshot = await document.GetSnapshotAsync(cancellationToken);
        if (!snapshot.Exists || snapshot.GetValue<string>("companyId") != companyId) return false;
        await document.DeleteAsync(cancellationToken: cancellationToken);
        return true;
    }

public async Task<IReadOnlyList<StudentProfileDto>> GetStudentsAsync(
    CancellationToken cancellationToken)
{
    var snapshot = await Database
        .Collection("students")
        .GetSnapshotAsync(cancellationToken);

    var students = new List<StudentProfileDto>();

    foreach (var document in snapshot.Documents)
    {
        try
        {
            var data = document.ConvertTo<FirestoreStudentDocument>();

            _logger.LogInformation(
                "Student {Id}: UserId={UserId}, ProfileVisible={ProfileVisible}",
                document.Id,
                data.UserId,
                data.ProfileVisible);

            if (!data.ProfileVisible)
                continue;

            students.Add(data.ToDto(document.Id));
        }
        catch (Exception exception)
        {
            _logger.LogError(
                exception,
                "Failed to deserialize student document {StudentId}",
                document.Id);
        }
    }

    return students
        .OrderBy(student => student.Name)
        .ToList();
}


    public async Task<string> UpsertStudentAsync(string? id, CreateStudentDto student, CancellationToken cancellationToken)
    {
        var document = string.IsNullOrWhiteSpace(id)
            ? Database.Collection("students").Document()
            : Database.Collection("students").Document(id);
        var studentDocument = FirestoreStudentDocument.From(student);
        studentDocument.UserId = document.Id;
        studentDocument.ProfileVisible = (await GetUserSettingsAsync(document.Id, cancellationToken)).ProfileVisible;
        await document.SetAsync(studentDocument, SetOptions.MergeAll, cancellationToken);
        return document.Id;
    }

    public async Task<string> UpsertCompanyAsync(string? id, CreateCompanyDto company, CancellationToken cancellationToken)
    {
        var document = string.IsNullOrWhiteSpace(id)
            ? Database.Collection("companies").Document()
            : Database.Collection("companies").Document(id);
        var companyDocument = FirestoreCompanyDocument.From(company);
        companyDocument.UserId = document.Id;
        companyDocument.ProfileVisible = (await GetUserSettingsAsync(document.Id, cancellationToken)).ProfileVisible;
        await document.SetAsync(companyDocument, SetOptions.MergeAll, cancellationToken);
        return document.Id;
    }

    public async Task<SwipeResponse> RecordSwipeAsync(SwipeRequest swipe, CancellationToken cancellationToken)
    {
        var swipeId = StableDocumentId($"{swipe.UserId}:{swipe.TargetId}");
        await Database.Collection("swipes").Document(swipeId).SetAsync(new Dictionary<string, object>
        {
            ["userId"] = swipe.UserId,
            ["targetId"] = swipe.TargetId,
            ["targetUserId"] = swipe.TargetUserId,
            ["isLike"] = swipe.IsLike,
            ["createdAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);

        if (swipe.UserId == swipe.TargetUserId) return new SwipeResponse(false, null);

        var participants = new[] { swipe.UserId, swipe.TargetUserId }.Order().ToArray();
        var pairId = string.Join("__", participants);
        var pairReference = Database.Collection("swipePairs").Document(pairId);
        var hasActiveLike = await HasLikeTowardAsync(swipe.UserId, swipe.TargetUserId, cancellationToken);
        await pairReference.SetAsync(new Dictionary<string, object>
        {
            ["participantIds"] = participants,
            ["likedBy"] = hasActiveLike
                ? FieldValue.ArrayUnion(swipe.UserId)
                : FieldValue.ArrayRemove(swipe.UserId),
            ["updatedAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);

        if (await HasLikeTowardAsync(swipe.TargetUserId, swipe.UserId, cancellationToken))
        {
            await pairReference.SetAsync(new Dictionary<string, object>
            {
                ["likedBy"] = FieldValue.ArrayUnion(swipe.TargetUserId)
            }, SetOptions.MergeAll, cancellationToken);
        }

        if (!hasActiveLike) return new SwipeResponse(false, null);

        var pairSnapshot = await pairReference.GetSnapshotAsync(cancellationToken);
        var likedBy = ReadStringList(pairSnapshot, "likedBy");
        if (!participants.All(likedBy.Contains)) return new SwipeResponse(false, null);

        var matchId = $"match_{pairId}";
        var matchReference = Database.Collection("matches").Document(matchId);
        if (!(await matchReference.GetSnapshotAsync(cancellationToken)).Exists)
        {
            await matchReference.SetAsync(new Dictionary<string, object>
            {
                ["id"] = matchId,
                ["pairId"] = pairId,
                ["mutual"] = true,
                ["participantIds"] = participants,
                ["targetId"] = swipe.TargetId,
                ["lastMessage"] = "You matched - start the conversation!",
                ["unreadBy"] = new[] { swipe.TargetUserId },
                ["createdAt"] = FieldValue.ServerTimestamp
            }, cancellationToken: cancellationToken);
        }

        using var artifactTimeout = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        try
        {
            await CreateMatchArtifactsAsync(participants, matchId, artifactTimeout.Token);
        }
        catch (Exception exception) when (IsExpectedCancellation(exception, artifactTimeout.Token))
        {
            _logger.LogWarning(exception, "Timed out creating supporting records for match {MatchId}.", matchId);
        }
        catch (Exception exception)
        {
            _logger.LogError(exception, "Could not create supporting records for match {MatchId}.", matchId);
        }

        return new SwipeResponse(true, matchId);
    }

    private async Task CreateMatchArtifactsAsync(
        IReadOnlyList<string> participants,
        string matchId,
        CancellationToken cancellationToken)
    {
        string? studentId = null;
        foreach (var participantId in participants)
        {
            if ((await Database.Collection("students").Document(participantId)
                    .GetSnapshotAsync(cancellationToken)).Exists)
            {
                studentId = participantId;
                break;
            }
        }

        if (studentId is not null)
        {
            var companyId = participants.FirstOrDefault(id => id != studentId);
            if (!string.IsNullOrWhiteSpace(companyId))
            {
                var accessReference = Database.Collection("cvAccess").Document(studentId)
                    .Collection("companies").Document(companyId);
                if (!(await accessReference.GetSnapshotAsync(cancellationToken)).Exists)
                {
                    await accessReference.SetAsync(new Dictionary<string, object>
                    {
                        ["studentId"] = studentId,
                        ["companyId"] = companyId,
                        ["matchId"] = matchId,
                        ["createdAt"] = FieldValue.ServerTimestamp
                    }, cancellationToken: cancellationToken);
                }
            }
        }

        foreach (var recipientId in participants)
        {
            var otherId = participants.First(id => id != recipientId);
            var otherName = await GetDisplayNameAsync(otherId, cancellationToken);
            var notificationReference = Database.Collection("notifications")
                .Document($"{matchId}_{recipientId}");
            if (!(await notificationReference.GetSnapshotAsync(cancellationToken)).Exists)
            {
                await notificationReference.SetAsync(new Dictionary<string, object>
                {
                    ["userId"] = recipientId,
                    ["actorId"] = otherId,
                    ["type"] = "MATCH",
                    ["title"] = "New match",
                    ["body"] = $"You matched with {otherName}.",
                    ["matchId"] = matchId,
                    ["createdAt"] = FieldValue.ServerTimestamp,
                    ["read"] = false
                },
                cancellationToken: cancellationToken);
            }
        }
    }

    private async Task<string> GetDisplayNameAsync(string userId, CancellationToken cancellationToken)
    {
        var student = await Database.Collection("students").Document(userId).GetSnapshotAsync(cancellationToken);
        if (student.Exists && student.TryGetValue<string>("name", out var studentName) &&
            !string.IsNullOrWhiteSpace(studentName)) return studentName;

        var company = await Database.Collection("companies").Document(userId).GetSnapshotAsync(cancellationToken);
        return company.Exists && company.TryGetValue<string>("name", out var companyName) &&
               !string.IsNullOrWhiteSpace(companyName)
            ? companyName
            : "SwipeHire user";
    }

    public async Task<IReadOnlyList<string>> GetSwipedTargetIdsAsync(
    string userId,
    CancellationToken cancellationToken)
    {
        try
        {
            var snapshot = await Database.Collection("swipes")
                .WhereEqualTo("userId", userId)
                .GetSnapshotAsync(cancellationToken);

            return snapshot.Documents
                .Select(document =>
                    document.TryGetValue<string>("targetId", out var targetId)
                        ? targetId
                        : "")
                .Where(targetId => !string.IsNullOrWhiteSpace(targetId))
                .Distinct()
                .ToList();
        }
        catch (Exception exception)
            when (IsExpectedCancellation(exception, cancellationToken))
        {
            return [];
        }
    }

    public async Task DeleteSwipeAsync(
        string userId,
        string targetId,
        string targetUserId,
        CancellationToken cancellationToken)
    {
        var swipeId = StableDocumentId($"{userId}:{targetId}");
        await Database.Collection("swipes").Document(swipeId).DeleteAsync(cancellationToken: cancellationToken);

        if (userId == targetUserId) return;
        var participants = new[] { userId, targetUserId }.Order().ToArray();
        var pairId = string.Join("__", participants);
        var hasActiveLike = await HasLikeTowardAsync(userId, targetUserId, cancellationToken);
        await Database.Collection("swipePairs").Document(pairId).SetAsync(
            new Dictionary<string, object>
            {
                ["participantIds"] = participants,
                ["likedBy"] = hasActiveLike
                    ? FieldValue.ArrayUnion(userId)
                    : FieldValue.ArrayRemove(userId),
                ["updatedAt"] = FieldValue.ServerTimestamp
            },
            SetOptions.MergeAll,
            cancellationToken
        );
    }

    private async Task<bool> HasLikeTowardAsync(
        string userId,
        string targetUserId,
        CancellationToken cancellationToken)
    {
        var snapshot = await Database.Collection("swipes")
            .WhereEqualTo("userId", userId)
            .GetSnapshotAsync(cancellationToken);
        return snapshot.Documents.Any(document =>
            document.TryGetValue<string>("targetUserId", out var otherId) && otherId == targetUserId &&
            document.TryGetValue<bool>("isLike", out var isLike) && isLike);
    }

    public async Task<SavedItemsDto> GetSavedItemsAsync(string userId, CancellationToken cancellationToken)
    {
        try
        {
            var snapshot = await Database.Collection("users").Document(userId).GetSnapshotAsync(cancellationToken);
            if (!snapshot.Exists) return new SavedItemsDto([], []);

            return new SavedItemsDto(
                ReadStringList(snapshot, "savedJobIds"),
                ReadStringList(snapshot, "savedStudentIds")
            );
        }
        catch (Exception exception) when (IsExpectedCancellation(exception, cancellationToken))
        {
            return new SavedItemsDto([], []);
        }
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
        try
        {
            var snapshot = await Database.Collection("users").Document(userId).GetSnapshotAsync(cancellationToken);
            if (!snapshot.Exists || !snapshot.TryGetValue<Dictionary<string, object>>("settings", out var settings))
                return new UserSettingsDto();

            return new UserSettingsDto(
                ReadBoolean(settings, "pushNotifications", true),
                ReadBoolean(settings, "matchAlerts", true),
                ReadBoolean(settings, "messageAlerts", true),
                ReadBoolean(settings, "profileVisible", true),
                settings.TryGetValue("language", out var language) ? language?.ToString() ?? "en" : "en"
            );
        }
        catch (Exception exception) when (IsExpectedCancellation(exception, cancellationToken))
        {
            return new UserSettingsDto();
        }
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
                ["profileVisible"] = settings.ProfileVisible,
                ["language"] = settings.Language
            },
            ["updatedAt"] = FieldValue.ServerTimestamp
        }, SetOptions.MergeAll, cancellationToken);

        var userSnapshot = await Database.Collection("users").Document(userId).GetSnapshotAsync(cancellationToken);
        var role = userSnapshot.Exists && userSnapshot.TryGetValue<string>("role", out var storedRole) ? storedRole : "";
        if (string.Equals(role, "STUDENT", StringComparison.OrdinalIgnoreCase))
        {
            await Database.Collection("students").Document(userId).SetAsync(
                new Dictionary<string, object> { ["profileVisible"] = settings.ProfileVisible },
                SetOptions.MergeAll,
                cancellationToken
            );
        }
        else if (string.Equals(role, "COMPANY", StringComparison.OrdinalIgnoreCase))
        {
            await Database.Collection("companies").Document(userId).SetAsync(
                new Dictionary<string, object> { ["profileVisible"] = settings.ProfileVisible },
                SetOptions.MergeAll,
                cancellationToken
            );
            var jobs = await Database.Collection("jobs").WhereEqualTo("companyId", userId).GetSnapshotAsync(cancellationToken);
            var batch = Database.StartBatch();
            foreach (var job in jobs.Documents)
                batch.Update(job.Reference, "profileVisible", settings.ProfileVisible);
            if (jobs.Count > 0) await batch.CommitAsync(cancellationToken);
        }
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

    private static bool IsExpectedCancellation(Exception exception, CancellationToken cancellationToken) =>
        exception is OperationCanceledException ||
        exception is RpcException rpcException &&
        (rpcException.StatusCode == StatusCode.Cancelled ||
         cancellationToken.IsCancellationRequested ||
         rpcException.Status.Detail.Contains("canceled", StringComparison.OrdinalIgnoreCase));

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
    [FirestoreProperty("profileVisible")] public bool ProfileVisible { get; set; } = true;
    [FirestoreProperty("createdAt")] public Timestamp? CreatedAt { get; set; }

    public JobPostingDto ToDto(string id) => new()
    {
        Id = id,
        CompanyId = CompanyId,
        Company = Company,
        Role = Role,
        Location = Location,
        WorkAddress = WorkAddress,
        Latitude = Latitude,
        Longitude = Longitude,
        Tags = Tags.ToList(),
        Blurb = Blurb,
        LogoInitials = LogoInitials,
        RemoteType = RemoteType,
        SalaryRange = SalaryRange
    };

    public static FirestoreJobDocument From(CreateJobPostingDto job) => new()
    {
        CompanyId = job.CompanyId.Trim(),
        Company = job.Company.Trim(),
        Role = job.Role.Trim(),
        Location = job.Location.Trim(),
        WorkAddress = job.WorkAddress.Trim(),
        Latitude = job.Latitude,
        Longitude = job.Longitude,
        Tags = job.Tags.ToList(),
        Blurb = job.Blurb.Trim(),
        LogoInitials = job.LogoInitials.Trim(),
        RemoteType = job.RemoteType.Trim().ToUpperInvariant(),
        SalaryRange = job.SalaryRange.Trim(),
        CreatedAt = Timestamp.GetCurrentTimestamp()
    };
}

[FirestoreData]
public sealed class FirestoreStudentDocument
{
    [FirestoreProperty("userId")] public string UserId { get; set; } = "";
    [FirestoreProperty("name")] public string Name { get; set; } = "";
    [FirestoreProperty("course")] public string Course { get; set; } = "";
    [FirestoreProperty("year")] public string Year { get; set; } = "";
    [FirestoreProperty("skills")] public List<string> Skills { get; set; } = [];
    [FirestoreProperty("blurb")] public string Blurb { get; set; } = "";
    [FirestoreProperty("avatarInitials")] public string AvatarInitials { get; set; } = "";
    [FirestoreProperty("profileVisible")] public bool ProfileVisible { get; set; } = true;
    [FirestoreProperty("updatedAt")] public Timestamp? UpdatedAt { get; set; }

    public StudentProfileDto ToDto(string id) => new()
    {
        Id = id,
        Name = Name,
        Course = Course,
        Year = Year,
        Skills = Skills.ToList(),
        Blurb = Blurb,
        AvatarInitials = AvatarInitials
    };

    public static FirestoreStudentDocument From(CreateStudentDto student) => new()
    {
        Name = student.Name.Trim(),
        Course = student.Course.Trim(),
        Year = student.Year.Trim(),
        Skills = student.Skills.ToList(),
        Blurb = student.Blurb.Trim(),
        AvatarInitials = student.AvatarInitials.Trim(),
        UpdatedAt = Timestamp.GetCurrentTimestamp()
    };
}

[FirestoreData]
public sealed class FirestoreCompanyDocument
{
    [FirestoreProperty("userId")] public string UserId { get; set; } = "";
    [FirestoreProperty("name")] public string Name { get; set; } = "";
    [FirestoreProperty("industry")] public string Industry { get; set; } = "";
    [FirestoreProperty("location")] public string Location { get; set; } = "";
    [FirestoreProperty("description")] public string Description { get; set; } = "";
    [FirestoreProperty("logoInitials")] public string LogoInitials { get; set; } = "";
    [FirestoreProperty("hiringFor")] public List<string> HiringFor { get; set; } = [];
    [FirestoreProperty("profileVisible")] public bool ProfileVisible { get; set; } = true;
    [FirestoreProperty("updatedAt")] public Timestamp? UpdatedAt { get; set; }

    public static FirestoreCompanyDocument From(CreateCompanyDto company) => new()
    {
        Name = company.Name.Trim(),
        Industry = company.Industry.Trim(),
        Location = company.Location.Trim(),
        Description = company.Description.Trim(),
        LogoInitials = company.LogoInitials.Trim(),
        HiringFor = company.HiringFor.ToList(),
        UpdatedAt = Timestamp.GetCurrentTimestamp()
    };
}
