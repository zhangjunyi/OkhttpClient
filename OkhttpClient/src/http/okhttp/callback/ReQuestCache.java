package http.okhttp.callback;

import com.squareup.okhttp.Response;

public interface ReQuestCache {
boolean isRequestCache(Response response);
}
