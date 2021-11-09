package core.util;

import core.exceptions.InvalidResponseException;
import core.exceptions.RedirectException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Headers;

import java.io.IOException;
import java.util.regex.*;

// A class that defines utility functions related to HTTP
public class HttpUtils {
    public static boolean validUrl(String url) {
        String urlRegex = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        return Pattern.matches(urlRegex, url);
    }
    public static boolean supportsAcceptRanges(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .head()
                .build();

        Response response = client.newCall(request).execute();
        String respHeader = response.header("accept-ranges");

        if(respHeader == null || respHeader.equals("none"))
            return false;
        if(respHeader.equals("bytes"))
            return true;
        throw new InvalidResponseException("The server returned an unknown value for accept-ranges");
    }
    public static String processRedirects(String url, int maxTries) throws IOException {
        final String locationHeader = "Location";
        final int permanentRedirectCode = 301;
        final int temporaryRedirectCode = 302;

        OkHttpClient client = new OkHttpClient();

        while(true) {
            if(maxTries == 0) {
                throw new RedirectException("The server redirected too many times");
            }
            Request req = new Request.Builder()
                    .url(url)
                    .head()
                    .build();
            Response resp = client.newCall(req).execute();
            if(resp.code() == permanentRedirectCode || resp.code() == temporaryRedirectCode) {
                url = resp.header(locationHeader);
                if(url == null) {
                    throw new InvalidResponseException("The server did not specify a Location header while redirecting");
                }
            }
            else
                break;
            --maxTries;
        }

        return url;
    }
}
