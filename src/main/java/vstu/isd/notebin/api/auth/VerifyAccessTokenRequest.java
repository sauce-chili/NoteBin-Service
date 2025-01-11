package vstu.isd.notebin.api.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerifyAccessTokenRequest {
    @NotNull
    private String accessToken;
}
