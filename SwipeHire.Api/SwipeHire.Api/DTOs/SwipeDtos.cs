namespace SwipeHire.Api.DTOs;

public record SwipeRequest(string UserId, string TargetId, bool IsLike);
public record SwipeResponse(bool IsMatch, string? MatchId);

public record GeocodeRequest(string Address);
public record GeocodeResponse(double Latitude, double Longitude);