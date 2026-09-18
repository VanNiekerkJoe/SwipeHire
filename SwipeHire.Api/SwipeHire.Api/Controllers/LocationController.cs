using Microsoft.AspNetCore.Mvc;
using SwipeHire.Api.DTOs;

namespace SwipeHire.Api.Controllers;

[ApiController]
[Route("api/location")]
public class LocationController : ControllerBase
{
    [HttpPost("geocode")]
    public IActionResult GeocodeAddress([FromBody] GeocodeRequest request)
    {
        double latitude = -26.1076;
        double longitude = 28.0567;

        return Ok(new GeocodeResponse(latitude, longitude));
    }
}