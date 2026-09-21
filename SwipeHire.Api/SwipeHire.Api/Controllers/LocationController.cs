using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/location")]
[Authorize]
public sealed class LocationController(
    GoogleGeocodingService geocodingService) : ControllerBase
{
    [HttpPost("geocode")]
    [EnableRateLimiting("geocoding")]
    public async Task<IActionResult> GeocodeAddress(
        [FromBody] GeocodeRequest request,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.Address))
        {
            return ValidationProblem(
                "Address is required.");
        }

        var userId =
            User.FindFirstValue(
                ClaimTypes.NameIdentifier);

        if (string.IsNullOrWhiteSpace(userId))
            return Unauthorized();

        try
        {
            var result =
                await geocodingService.GeocodeAsync(
                    request.Address,
                    userId,
                    cancellationToken);

            if (result is null)
            {
                return NotFound(new
                {
                    message =
                        "The address could not be geocoded."
                });
            }

            return Ok(
                new GeocodeResponse(
                    result.Value.Latitude,
                    result.Value.Longitude));
        }
        catch (GeocodingLimitExceededException)
        {
            return StatusCode(
                StatusCodes.Status429TooManyRequests,
                new
                {
                    message =
                        "Geocoding usage limit reached. " +
                        "Please try again later."
                });
        }
    }
}
