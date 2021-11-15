package core;
import okhttp3.OkHttpClient;

public class HttpClient {
    private static final OkHttpClient client = new OkHttpClient();

    private HttpClient() {}

    public static OkHttpClient getInstance() {
        return client;
    }
}
