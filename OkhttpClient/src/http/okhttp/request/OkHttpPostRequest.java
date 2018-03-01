package http.okhttp.request;

import android.text.TextUtils;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import http.okhttp.callback.ResultCallback;

import java.io.File;
import java.util.Map;

/**
 * post方式
 * 
 * @author Administrator
 * 
 */
public class OkHttpPostRequest extends OkHttpRequest {
	private String content;
	private byte[] bytes;
	private File file;

	private int type = 0;
	private static final int TYPE_PARAMS = 1;
	private static final int TYPE_STRING = 2;
	private static final int TYPE_BYTES = 3;
	private static final int TYPE_FILE = 4;

	private final MediaType MEDIA_TYPE_STREAM = MediaType
			.parse("application/octet-stream;charset=utf-8");
	private final MediaType MEDIA_TYPE_STRING = MediaType
			.parse("text/plain;charset=utf-8");

	protected OkHttpPostRequest(String url, String tag,
			Map<String, String> params, Map<String, String> headers,
			String content, byte[] bytes, File file,boolean isStore,CacheControl  cacheControl) {
		super(url, tag, params, headers,isStore,cacheControl,null);
		this.content = content;
		this.bytes = bytes;
		this.file = file;
	}

	protected void validParams() {
		int count = 0;
		if (params != null && !params.isEmpty()) {
			type = TYPE_PARAMS;
			count++;
		}
		if (content != null) {
			type = TYPE_STRING;
			count++;
		}
		if (bytes != null) {
			type = TYPE_BYTES;
			count++;
		}
		if (file != null) {
			type = TYPE_FILE;
			count++;
		}

		if (count <= 0 || count > 1) {
			throw new IllegalArgumentException(
					"the params , content , file , bytes must has one and only one .");
		}
	}

	@Override
	protected Request buildRequest() {
		if (TextUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url can not be empty!");
		}
		Request.Builder builder = new Request.Builder();
		appendHeaders(builder, headers);
		if(cacheControl!=null)
		builder.cacheControl(cacheControl);
		builder.url(url).tag(tag).isStore(isStore).post(requestBody);
		return builder.build();
	}

	@Override
	protected RequestBody buildRequestBody() {
		validParams();
		RequestBody requestBody = null;
		switch (type) {
		//表格数据建造
		case TYPE_PARAMS:
			FormEncodingBuilder builder = new FormEncodingBuilder();
			addParams(builder, params);
			requestBody = builder.build();
			break;
			//字节参数建造
		case TYPE_BYTES:
			requestBody = RequestBody.create(MEDIA_TYPE_STREAM, bytes);
			break;
		case TYPE_FILE:
			//流方式建造
			requestBody = RequestBody.create(MEDIA_TYPE_STREAM, file);
			break;
		case TYPE_STRING:
			requestBody = RequestBody.create(MEDIA_TYPE_STRING, content);
			break;
		}
		return requestBody;
	}

	/**
	 * 进度条监控
	 */
	protected RequestBody wrapRequestBody(RequestBody requestBody,
			final ResultCallback callback) {
		CountingRequestBody countingRequestBody = new CountingRequestBody(
				requestBody, new CountingRequestBody.Listener() {
					@Override
					public void onRequestProgress(final long bytesWritten,
							final long contentLength) {

						mOkHttpClientManager.getDelivery().post(new Runnable() {
							@Override
							public void run() {
								callback.inProgress(bytesWritten * 1.0f
										/ contentLength);
							}
						});

					}
				});
		return countingRequestBody;
	}

	/**
	 * 添加表单数据 FormEncodingBuilder okhttp专门用于封装键值对的
	 * 
	 * @param builder
	 * @param params
	 */
	private void addParams(FormEncodingBuilder builder,
			Map<String, String> params) {
		if (builder == null) {
			throw new IllegalArgumentException("builder can not be null .");
		}

		if (params != null && !params.isEmpty()) {
			for (String key : params.keySet()) {
				builder.add(key, params.get(key));
			}
		}
	}
}
