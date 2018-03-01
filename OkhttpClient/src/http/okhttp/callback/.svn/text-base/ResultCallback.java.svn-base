package http.okhttp.callback;

import com.google.gson.internal.$Gson$Types;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class ResultCallback<T> {
	public Type mType;

	public ResultCallback() {
		mType = getSuperclassTypeParameter(getClass());
	}
/**
 * 得到result的泛型的类型
 * @param subclass
 * @return
 */
	static Type getSuperclassTypeParameter(Class<?> subclass) {
		Type superclass = subclass.getGenericSuperclass();
		if (superclass instanceof Class) {
			throw new RuntimeException("Missing type parameter.");
		}
		ParameterizedType parameterized = (ParameterizedType) superclass;
		return $Gson$Types
				.canonicalize(parameterized.getActualTypeArguments()[0]);
	}

	/**
	 * 在与服务器连接之前需要处理的事情
	 * 
	 * @param request
	 */
	public void onBefore(Request request) {
	}

	/**
	 * 在与服务器连接之后需要处理的事情
	 */
	public void onAfter() {
	}

	/**
	 * 进度条处理
	 * 
	 * @param progress
	 */
	public void inProgress(float progress) {

	}

	/**
	 * 错误信息
	 * 
	 * @param request
	 * @param e
	 */
	public abstract void onError(Request request, Exception e);

	/**
	 * 返回的结果信息
	 * 
	 * @param response
	 */
	public abstract void onResponse(T response,Headers headers);

	/**
	 * 如果不设置回调方法默认采用返回为String的方法
	 */
	public static final ResultCallback<String> DEFAULT_RESULT_CALLBACK = new ResultCallback<String>() {
		@Override
		public void onError(Request request, Exception e) {

		}

		@Override
		public void onResponse(String response,Headers headers) {

		}
	};
}