import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fixed local, text-free health check. Does not print response bodies or diagnostics. */
public final class FeedbackHealth {
    public static void main(String[] args) {
        try {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/actuator/health"))
                .timeout(Duration.ofSeconds(4)).GET().build();
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
            System.exit(response.statusCode() == 200 && response.body().strip().equals("{\"status\":\"UP\"}") ? 0 : 1);
        } catch (Exception unavailable) { System.exit(1); }
    }
}
