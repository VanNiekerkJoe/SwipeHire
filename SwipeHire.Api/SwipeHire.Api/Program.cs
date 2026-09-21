using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.OpenApi;
using SwipeHire.Api.Authentication;
using System.Security.Claims;
using System.Threading.RateLimiting;

namespace SwipeHire.Api
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            // Console logging works consistently in Visual Studio, CI, and standard-user shells.
            // Clearing defaults avoids Windows Event Log permission failures on lab machines.
            builder.Logging.ClearProviders();
            builder.Logging.AddConsole();

            // Rate limiting
            builder.Services.AddRateLimiter(options =>
            {
                options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;

                // General authenticated API limit:
                // 10 requests per Firebase user per minute.
                options.AddPolicy("general", httpContext =>
                    RateLimitPartition.GetFixedWindowLimiter(
                        partitionKey: GetUserRateLimitKey(httpContext),
                        factory: _ => new FixedWindowRateLimiterOptions
                        {
                            PermitLimit = 10,
                            Window = TimeSpan.FromMinutes(1),
                            QueueLimit = 0,
                            AutoReplenishment = true
                        }));

                // Geocoding limit:
                // 1 request per Firebase user per minute.
                options.AddPolicy("geocoding", httpContext =>
                    RateLimitPartition.GetFixedWindowLimiter(
                        partitionKey: GetUserRateLimitKey(httpContext),
                        factory: _ => new FixedWindowRateLimiterOptions
                        {
                            PermitLimit = 1,
                            Window = TimeSpan.FromMinutes(1),
                            QueueLimit = 0,
                            AutoReplenishment = true
                        }));
            });

            builder.Services
                .AddAuthentication("Firebase")
                .AddScheme<AuthenticationSchemeOptions, FirebaseAuthenticationHandler>(
                    "Firebase",
                    _ => { });

            builder.Services.AddAuthorization();

            builder.Services.AddControllers();
            builder.Services.AddEndpointsApiExplorer();
            builder.Services.AddSwaggerGen();
            builder.Services.AddSingleton<Services.FirestoreDataService>();

            builder.Services.AddSingleton<Services.ApiUsageLimitService>();

            builder.Services.AddHttpClient<Services.GoogleGeocodingService>();

            // Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
            builder.Services.AddOpenApi();

            var app = builder.Build();

            app.Use(async (context, next) =>
            {
                try
                {
                    await next();
                }
                catch (InvalidOperationException exception) when (
                    exception.Message.Contains("default credentials", StringComparison.OrdinalIgnoreCase))
                {
                    app.Logger.LogError("Firestore credentials are not configured for the API host.");
                    context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
                    await Results.Problem(
                        statusCode: StatusCodes.Status503ServiceUnavailable,
                        title: "Firestore credentials are not configured",
                        detail: "Set GOOGLE_APPLICATION_CREDENTIALS or FIRESTORE_EMULATOR_HOST before starting the API."
                    ).ExecuteAsync(context);
                }
            });

            if (app.Environment.IsDevelopment())
            {
                app.UseSwagger();
                app.UseSwaggerUI(c =>
                {
                    c.SwaggerEndpoint("/swagger/v1/swagger.json", "SwipeHire API v1");
                    c.RoutePrefix = "swagger"; // Serves Swagger UI at http://localhost:5000/swagger
                });
            }

            app.UseAuthentication();
            app.UseAuthorization();
            app.UseRateLimiter();

            app.MapControllers();

            app.Run();
        }

        private static string GetUserRateLimitKey(
            HttpContext context)
        {
            var userId = context.User.FindFirstValue(
                ClaimTypes.NameIdentifier);

            return string.IsNullOrWhiteSpace(userId)
                ? "anonymous"
                : $"user:{userId}";
        }
    }
}
