using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;
using SwipeHire.Api.Services;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/location")]
public sealed class LocationController(GoogleGeocodingService geocodingService) : ControllerBase
{
    [HttpPost("geocode")]
    public async Task<IActionResult> GeocodeAddress([FromBody] GeocodeRequest request, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(request.Address)) return ValidationProblem("Address is required.");

        var result = await geocodingService.GeocodeAsync(request.Address, cancellationToken);
        if (result is null)
            return NotFound(new { message = "The address could not be geocoded. Configure GoogleMaps:ApiKey for arbitrary addresses." });

        return Ok(new GeocodeResponse(result.Value.Latitude, result.Value.Longitude));
    }
}