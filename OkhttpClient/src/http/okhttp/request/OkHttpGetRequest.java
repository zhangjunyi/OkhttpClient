package http.okhttp.request;

import android.text.TextUtils;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import http.okhttp.callback.ReQuestCache;

import java.util.Map;

/**
 * okhttpget方式封装
 * 
 * @author Administrator
 * 
 */
public class OkHttpGetRequest extends OkHttpRequest {
	protected OkHttpGetRequest(String url, String tag,
			Map<String, String> params, Map<String, String> headers,boolean isStore,CacheControl  cacheControl,ReQuestCache reQuestCache) {
		super(url, tag, params, headers, isStore,cacheControl,reQuestCache);
	}
/**
 * 初始化okhttp的Requst参数
 */
	@Override
	protected Request buildRequest() {
		if (TextUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url can not be empty!");
		}
		url = appendParams(url, params);
		Request.Builder builder = new Request.Builder();
		// add headers , if necessary
		appendHeaders(builder, headers);
		if(cacheControl!=null)
		builder.cacheControl(cacheControl);
		if(reQuestCache!=null)
			builder.ReQuestCache(reQuestCache);
		builder.url(url).tag(tag).isStore(isStore);
		return builder.build();
	}

	@Override
	protected RequestBody buildRequestBody() {
		return null;
	}
/**
 * get方式添加参数
 * @param url
 * @param params
 * @return
 */
	private String appendParams(String url, Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append(url + "?");
		if (params != null && !params.isEmpty()) {
			for (String key : params.keySet()) {
				sb.append(key).append("=").append(params.get(key)).append("&");
			}
		}

		sb = sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
