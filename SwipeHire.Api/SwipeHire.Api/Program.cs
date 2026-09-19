
namespace SwipeHire.Api
{
    public class Program
    {
        public static void Main(string[] args)
        {
            var builder = WebApplication.CreateBuilder(args);

            builder.Logging.ClearProviders();
            builder.Logging.AddConsole();

            builder.Services.AddControllers();
            builder.Services.AddEndpointsApiExplorer();
            builder.Services.AddSwaggerGen();
            builder.Services.AddSingleton<Services.FirestoreDataService>();
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

            //app.UseHttpsRedirection();

            app.UseAuthorization();


            app.MapControllers();

            app.Run();
        }
    }
}
