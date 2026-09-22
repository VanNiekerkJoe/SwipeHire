namespace SwipeHire.Api.DTOs;

public record SwipeRequest(string UserId, string TargetId, string TargetUserId, bool IsLike);
public record SwipeResponse(bool IsMatch, string? MatchId);

public record GeocodeRequest(string Address);
public record GeocodeResponse(double Latitude, double Longitude);