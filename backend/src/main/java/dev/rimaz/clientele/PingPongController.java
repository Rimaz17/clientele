package dev.rimaz.clientele;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ping", description = "Check that the API is running")
@RestController
public class PingPongController {

    private static int COUNTER = 0;

    record PingPong(String result){}

    @Operation(summary = "Ping the API", description = "Answers with a counter of pings since startup.")
    @SecurityRequirements
    @GetMapping("/ping")
    public PingPong getPingPong() {
        return new PingPong("Pong: %s".formatted(++COUNTER));
    }
}
