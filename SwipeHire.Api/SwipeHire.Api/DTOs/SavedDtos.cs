namespace SwipeHire.Api.DTOs;

public record SavedItemsDto(IReadOnlyList<string> SavedJobIds, IReadOnlyList<string> SavedStudentIds);
public record SetSavedItemRequest(bool Saved);
public record UserSettingsDto(
    bool PushNotifications = true,
    bool MatchAlerts = true,
    bool MessageAlerts = true,
    bool ProfileVisible = true
);
