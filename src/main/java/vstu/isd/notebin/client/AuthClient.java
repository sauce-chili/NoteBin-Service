package vstu.isd.notebin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vstu.isd.notebin.dto.VerifyAccessTokenRequestDto;

@FeignClient(name = "auth-service", url = "http://localhost:8081/api/v1/auth")
public interface AuthClient {
    @PatchMapping("/verify-access")
    Boolean verifyAccessToken(@RequestBody VerifyAccessTokenRequestDto request);
}
