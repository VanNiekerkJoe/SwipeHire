using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;

var repositoryRoot = FindRepositoryRoot(AppContext.BaseDirectory);
var firebaseConfigPath = Path.Combine(repositoryRoot, "app", "google-services.json");
var seedPath = Path.Combine(repositoryRoot, "seed-data.local.json");

if (!File.Exists(firebaseConfigPath) || !File.Exists(seedPath))
    throw new InvalidOperationException("app/google-services.json and seed-data.local.json are required.");

using var firebaseJson = JsonDocument.Parse(await File.ReadAllTextAsync(firebaseConfigPath));
var root = firebaseJson.RootElement;
var projectId = root.GetProperty("project_info").GetProperty("project_id").GetString()
    ?? throw new InvalidOperationException("Firebase project_id is missing.");
var apiKey = root.GetProperty("client")[0].GetProperty("api_key")[0].GetProperty("current_key").GetString()
    ?? throw new InvalidOperationException("Firebase API key is missing.");
var seed = JsonSerializer.Deserialize<SeedData>(
    await File.ReadAllTextAsync(seedPath),
    new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
) ?? throw new InvalidOperationException("Seed data could not be parsed.");

using var http = new HttpClient { Timeout = TimeSpan.FromSeconds(30) };
var profileCount = 0;
var jobCount = 0;

foreach (var student in seed.Students)
{
    var auth = await EnsureUserAsync(student.Email, student.Password, student.Name);
    await PutDocumentAsync(projectId, "users", auth.UserId, auth.IdToken, new()
    {
        ["role"] = StringValue("STUDENT"),
        ["email"] = StringValue(student.Email),
        ["savedJobIds"] = ArrayValue([]),
        ["savedStudentIds"] = ArrayValue([]),
        ["settings"] = MapValue(new()
        {
            ["pushNotifications"] = BooleanValue(true),
            ["matchAlerts"] = BooleanValue(true),
            ["messageAlerts"] = BooleanValue(true),
            ["profileVisible"] = BooleanValue(true)
        }),
        ["updatedAt"] = TimestampValue()
    });
    await PutDocumentAsync(projectId, "students", auth.UserId, auth.IdToken, new()
    {
        ["userId"] = StringValue(auth.UserId),
        ["name"] = StringValue(student.Name),
        ["course"] = StringValue(student.Course),
        ["year"] = StringValue(student.Year),
        ["skills"] = ArrayValue(student.Skills.Select(StringValue)),
        ["blurb"] = StringValue(student.Blurb),
        ["avatarInitials"] = StringValue(Initials(student.Name)),
        ["updatedAt"] = TimestampValue()
    });
    profileCount++;
}

foreach (var company in seed.Companies)
{
    var auth = await EnsureUserAsync(company.Email, company.Password, company.Name);
    await PutDocumentAsync(projectId, "users", auth.UserId, auth.IdToken, new()
    {
        ["role"] = StringValue("COMPANY"),
        ["email"] = StringValue(company.Email),
        ["savedJobIds"] = ArrayValue([]),
        ["savedStudentIds"] = ArrayValue([]),
        ["settings"] = MapValue(new()
        {
            ["pushNotifications"] = BooleanValue(true),
            ["matchAlerts"] = BooleanValue(true),
            ["messageAlerts"] = BooleanValue(true),
            ["profileVisible"] = BooleanValue(true)
        }),
        ["updatedAt"] = TimestampValue()
    });
    await PutDocumentAsync(projectId, "companies", auth.UserId, auth.IdToken, new()
    {
        ["userId"] = StringValue(auth.UserId),
        ["name"] = StringValue(company.Name),
        ["industry"] = StringValue(company.Industry),
        ["location"] = StringValue(company.Location),
        ["description"] = StringValue(company.Description),
        ["logoInitials"] = StringValue(Initials(company.Name)),
        ["hiringFor"] = ArrayValue(company.HiringFor.Select(StringValue)),
        ["updatedAt"] = TimestampValue()
    });
    profileCount++;

    for (var index = 0; index < company.Jobs.Count; index++)
    {
        var job = company.Jobs[index];
        var jobId = $"seed-{Slug(company.Name)}-{index + 1}";
        await PutDocumentAsync(projectId, "jobs", jobId, auth.IdToken, new()
        {
            ["companyId"] = StringValue(auth.UserId),
            ["company"] = StringValue(company.Name),
            ["role"] = StringValue(job.Role),
            ["location"] = StringValue($"{job.Location} - {RemoteLabel(job.RemoteType)}"),
            ["workAddress"] = StringValue(job.WorkAddress),
            ["latitude"] = DoubleValue(job.Latitude),
            ["longitude"] = DoubleValue(job.Longitude),
            ["tags"] = ArrayValue(job.Tags.Select(StringValue)),
            ["blurb"] = StringValue(job.Blurb),
            ["logoInitials"] = StringValue(Initials(company.Name)),
            ["remoteType"] = StringValue(job.RemoteType),
            ["salaryRange"] = StringValue(job.SalaryRange),
            ["createdAt"] = TimestampValue()
        });
        jobCount++;
    }
}

Console.WriteLine($"Seed complete: {profileCount} Firebase accounts/profiles and {jobCount} company-owned jobs.");
Console.WriteLine($"Credentials are stored locally at: {seedPath}");
return;

async Task<AuthSession> EnsureUserAsync(string email, string password, string displayName)
{
    var signUp = await PostIdentityAsync("signUp", new { email, password, returnSecureToken = true });
    if (signUp.Success) return signUp.Session!;
    if (signUp.ErrorCode != "EMAIL_EXISTS")
        throw new InvalidOperationException($"Could not create {email}: {signUp.ErrorCode}");

    var signIn = await PostIdentityAsync("signInWithPassword", new { email, password, returnSecureToken = true });
    if (!signIn.Success)
        throw new InvalidOperationException($"Account {email} exists but could not sign in: {signIn.ErrorCode}");
    return signIn.Session!;

    async Task<IdentityResult> PostIdentityAsync(string operation, object body)
    {
        using var response = await http.PostAsJsonAsync(
            $"https://identitytoolkit.googleapis.com/v1/accounts:{operation}?key={Uri.EscapeDataString(apiKey)}",
            body
        );
        var responseText = await response.Content.ReadAsStringAsync();
        using var json = JsonDocument.Parse(responseText);
        if (response.IsSuccessStatusCode)
        {
            return new(true, new AuthSession(
                json.RootElement.GetProperty("localId").GetString()!,
                json.RootElement.GetProperty("idToken").GetString()!
            ), null);
        }
        var code = json.RootElement.GetProperty("error").GetProperty("message").GetString() ?? "UNKNOWN_ERROR";
        return new(false, null, code.Split(" : ")[0]);
    }
}

async Task PutDocumentAsync(
    string firebaseProjectId,
    string collection,
    string documentId,
    string idToken,
    Dictionary<string, object> fields)
{
    var url = $"https://firestore.googleapis.com/v1/projects/{Uri.EscapeDataString(firebaseProjectId)}/databases/(default)/documents/{collection}/{Uri.EscapeDataString(documentId)}";
    using var request = new HttpRequestMessage(HttpMethod.Patch, url);
    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", idToken);
    request.Content = new StringContent(JsonSerializer.Serialize(new { fields }), Encoding.UTF8, "application/json");
    using var response = await http.SendAsync(request);
    if (response.IsSuccessStatusCode) return;
    var details = await response.Content.ReadAsStringAsync();
    throw new InvalidOperationException($"Firestore rejected {collection}/{documentId}: {(int)response.StatusCode} {SafeFirestoreError(details)}");
}

static object StringValue(string value) => new { stringValue = value };
static object BooleanValue(bool value) => new { booleanValue = value };
static object DoubleValue(double value) => new { doubleValue = value };
static object TimestampValue() => new { timestampValue = DateTimeOffset.UtcNow.ToString("O") };
static object ArrayValue(IEnumerable<object> values) => new { arrayValue = new { values = values.ToArray() } };
static object MapValue(Dictionary<string, object> fields) => new { mapValue = new { fields } };
static string Initials(string value) => string.Concat(value.Split(' ', StringSplitOptions.RemoveEmptyEntries).Take(2).Select(x => char.ToUpperInvariant(x[0])));
static string Slug(string value) => string.Join('-', value.ToLowerInvariant().Split(' ', StringSplitOptions.RemoveEmptyEntries));
static string RemoteLabel(string value) => value switch { "REMOTE" => "Remote", "HYBRID" => "Hybrid", _ => "On-site" };
static string SafeFirestoreError(string json)
{
    try { return JsonDocument.Parse(json).RootElement.GetProperty("error").GetProperty("message").GetString() ?? "Unknown error"; }
    catch { return "Unknown Firestore error"; }
}
static string FindRepositoryRoot(string start)
{
    var directory = new DirectoryInfo(start);
    while (directory != null)
    {
        if (File.Exists(Path.Combine(directory.FullName, "settings.gradle.kts"))) return directory.FullName;
        directory = directory.Parent;
    }
    throw new DirectoryNotFoundException("SwipeHire repository root was not found.");
}

sealed record AuthSession(string UserId, string IdToken);
sealed record IdentityResult(bool Success, AuthSession? Session, string? ErrorCode);
sealed record SeedData(List<StudentSeed> Students, List<CompanySeed> Companies);
sealed record StudentSeed(string Email, string Password, string Name, string Course, string Year, List<string> Skills, string Blurb);
sealed record CompanySeed(string Email, string Password, string Name, string Industry, string Location, string Description, List<string> HiringFor, List<JobSeed> Jobs);
sealed record JobSeed(string Role, string Location, string WorkAddress, double Latitude, double Longitude, List<string> Tags, string Blurb, string RemoteType, string SalaryRange);
