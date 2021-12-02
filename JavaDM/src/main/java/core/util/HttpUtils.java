package core.util;

import core.HttpClient;
import core.exceptions.ConnectionException;
import core.exceptions.InvalidResponseException;
import core.exceptions.RedirectException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.regex.Pattern;

// A class that defines utility functions related to HTTP
public class HttpUtils {
    public static boolean validUrl(String url) {
        String urlRegex = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        return Pattern.matches(urlRegex, url);
    }

    public static boolean supportsAcceptRanges(String url) {
        Response response = HttpUtils.getResponse(url, "HEAD");

        String respHeader = response.header("accept-ranges");

        if (respHeader == null || respHeader.equals("none"))
            return false;
        if (respHeader.equals("bytes"))
            return true;
        throw new InvalidResponseException("The server returned an unknown value for accept-ranges");
    }

    public static String processRedirects(String url, int maxTries) {
        final String locationHeader = "Location";
        final int permanentRedirectCode = 301;
        final int temporaryRedirectCode = 302;

        OkHttpClient client = new OkHttpClient();

        while (true) {
            if (maxTries == 0) {
                throw new RedirectException("The server redirected too many times");
            }
            Request req = new Request.Builder()
                    .url(url)
                    .head()
                    .build();

            Response resp;
            try {
                resp = client.newCall(req).execute();
            } catch (IOException ex) {
                throw new ConnectionException("Could not establish connection with server");
            }
            if (resp.code() == permanentRedirectCode || resp.code() == temporaryRedirectCode) {
                url = resp.header(locationHeader);
                if (url == null) {
                    throw new InvalidResponseException("The server did not specify a Location header while redirecting");
                }
            } else
                break;
            --maxTries;
        }

        return url;
    }

    public static Response getResponse(String url, @NotNull String method) {
        OkHttpClient client = HttpClient.getInstance();
        Request.Builder b = new Request.Builder().url(url);
        Request req;
        switch (method) {
            case "GET":
                req = b.get().build();
                break;
            case "HEAD":
                req = b.head().build();
                break;
            default:
                return null;
        }
        try {
            return client.newCall(req).execute();
        } catch (IOException ex) {
            throw new ConnectionException("Could not establish connection with server");
        }
    }

    public static String getExtension(String url) {
        Response resp = getResponse(url, "HEAD");

        try {
            return resp.body().contentType().subtype();
        } catch(NullPointerException ne) {
            return "txt";
        }
    }
}
