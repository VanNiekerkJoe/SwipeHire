using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.OpenApi;
using Grpc.Core;
using SwipeHire.Api.Authentication;
using System.Diagnostics;
using System.Globalization;
using System.Security.Claims;
using System.Threading.RateLimiting;

namespace SwipeHire.Api
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);


            builder.Logging.ClearProviders();
            builder.Logging.AddConsole();

            // Rate limiting
            builder.Services.AddRateLimiter(options =>
            {
                options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
                options.OnRejected = async (context, cancellationToken) =>
                {
                    if (context.Lease.TryGetMetadata(MetadataName.RetryAfter, out var retryAfter))
                    {
                        context.HttpContext.Response.Headers.RetryAfter =
                            Math.Max(1, (int)Math.Ceiling(retryAfter.TotalSeconds))
                                .ToString(CultureInfo.InvariantCulture);
                    }

                    context.HttpContext.Response.StatusCode = StatusCodes.Status429TooManyRequests;
                    await context.HttpContext.Response.WriteAsJsonAsync(
                        new { message = "Too many requests. Please try again later." },
                        cancellationToken);
                };

                // Allow normal feed refreshes and swipe bursts while still bounding
                // the amount of work one signed-in user can send to the API.
                options.AddPolicy("general", httpContext =>
                    RateLimitPartition.GetFixedWindowLimiter(
                        partitionKey: GetUserRateLimitKey(httpContext),
                        factory: _ => new FixedWindowRateLimiterOptions
                        {
                            PermitLimit = 120,
                            Window = TimeSpan.FromMinutes(1),
                            QueueLimit = 0,
                            AutoReplenishment = true
                        }));

                // Anonymous feed requests must not all compete for one shared
                // bucket. The client address is the partition for public reads.
                options.AddPolicy("public", httpContext =>
                    RateLimitPartition.GetFixedWindowLimiter(
                        partitionKey: httpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown",
                        factory: _ => new FixedWindowRateLimiterOptions
                        {
                            PermitLimit = 60,
                            Window = TimeSpan.FromMinutes(1),
                            QueueLimit = 0,
                            AutoReplenishment = true
                        }));

                // Geocoding limit:
                // Allow a few address corrections during a demo. The shared
                // Firestore daily quota still bounds paid Google requests.
                options.AddPolicy("geocoding", httpContext =>
                    RateLimitPartition.GetFixedWindowLimiter(
                        partitionKey: GetUserRateLimitKey(httpContext),
                        factory: _ => new FixedWindowRateLimiterOptions
                        {
                            PermitLimit = 5,
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
            builder.Services.AddSingleton<Services.FirebaseAuthenticationService>();
            builder.Services.AddSingleton<Services.FirestoreDataService>();

            builder.Services.AddSingleton<Services.ApiUsageLimitService>();

            builder.Services.AddHttpClient<Services.GoogleGeocodingService>();

            // Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
            builder.Services.AddOpenApi();

            var app = builder.Build();

            app.Use(async (context, next) =>
            {
                var elapsed = Stopwatch.StartNew();
                try
                {
                    await next();
                }
                catch (OperationCanceledException) when (context.RequestAborted.IsCancellationRequested)
                {
                    app.Logger.LogInformation("The client disconnected during a {Method} request.", context.Request.Method);
                }
                catch (RpcException exception) when (exception.StatusCode == StatusCode.Cancelled)
                {
                    if (context.RequestAborted.IsCancellationRequested)
                    {
                        app.Logger.LogInformation("The client disconnected during a Firestore {Method} request.", context.Request.Method);
                        return;
                    }

                    app.Logger.LogError(exception, "Firestore cancelled a {Method} request while the client was connected.", context.Request.Method);
                    if (context.Response.HasStarted) throw;

                    await Results.Problem(
                        statusCode: StatusCodes.Status503ServiceUnavailable,
                        title: "Firestore request cancelled",
                        detail: "The database request did not complete. Please retry."
                    ).ExecuteAsync(context);
                }
                catch (InvalidOperationException exception) when (
                    exception.Message.Contains("default credentials", StringComparison.OrdinalIgnoreCase))
                {
                    app.Logger.LogError("Firestore credentials are not configured for the API host.");
                    context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
                    await Results.Problem(
                        statusCode: StatusCodes.Status503ServiceUnavailable,
                        title: "Firestore credentials are not configured",
                        detail: "Set GoogleCredentials__Json, GOOGLE_APPLICATION_CREDENTIALS, or FIRESTORE_EMULATOR_HOST before starting the API."
                    ).ExecuteAsync(context);
                }
                finally
                {
                    elapsed.Stop();
                    if (elapsed.Elapsed >= TimeSpan.FromSeconds(5))
                    {
                        app.Logger.LogWarning(
                            "Slow {Method} {Endpoint}: {ElapsedMs} ms, status {StatusCode}, client cancelled: {ClientCancelled}.",
                            context.Request.Method,
                            context.GetEndpoint()?.DisplayName ?? "unknown endpoint",
                            elapsed.ElapsedMilliseconds,
                            context.Response.StatusCode,
                            context.RequestAborted.IsCancellationRequested);
                    }
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
