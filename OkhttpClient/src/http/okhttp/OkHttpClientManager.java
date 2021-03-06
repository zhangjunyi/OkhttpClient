package http.okhttp;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import http.okhttp.callback.ResultCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class OkHttpClientManager {

	private static OkHttpClientManager mInstance;
	private OkHttpClient mOkHttpClient;
	// 用handler分发事件实现异步方式
	private Handler mDelivery;
	private static final long CACHE_SIZE = 1024 * 1024 * 10;
	// 谷歌的gson的解析
	private Gson mGson;
	@SuppressWarnings("unused")
	private OkHttpClientManager() throws IOException {
		mOkHttpClient = new OkHttpClient();
		/*mOkHttpClient.setCookieHandler(new CookieManager(null,
				CookiePolicy.ACCEPT_ORIGINAL_SERVER));*/
		mDelivery = new Handler(Looper.getMainLooper());
		// 默认缓存10M
		mOkHttpClient.setConnectTimeout(8, TimeUnit.SECONDS);
		mOkHttpClient.setWriteTimeout(10, TimeUnit.SECONDS);
		mOkHttpClient.setReadTimeout(30, TimeUnit.SECONDS);
		final int sdk = Build.VERSION.SDK_INT;
		// 版本在5.0以后内嵌了gson
		if (sdk >= 23) {
			GsonBuilder gsonBuilder = new GsonBuilder()
					.excludeFieldsWithModifiers(Modifier.FINAL,
							Modifier.TRANSIENT, Modifier.STATIC);
			mGson = gsonBuilder.create();
		} else {
			mGson = new Gson();
		}

		if (false) {
			mOkHttpClient.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}

	}

	/**
	 * 设置缓存
	 * 
	 * @param context
	 * @param maxCacheSize
	 *            缓存大小
	 * @throws IOException
	 */
	public void setCache(Context context, long maxCacheSize) throws IOException {
		File cacheDir = getDiskCacheDir(context, "yagouCache");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		Cache cache = new Cache(cacheDir, maxCacheSize <= 0 ? CACHE_SIZE
				: maxCacheSize);
		if (mOkHttpClient != null) {
			mOkHttpClient.setCache(cache);
		}
	}

	public static File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		//	Log.i("huoying", "cachePath:" + cachePath);
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * 单例模式的懒汉模式
	 * 
	 * @return
	 */
	public static OkHttpClientManager getInstance() {
		if (mInstance == null) {
			synchronized (OkHttpClientManager.class) {
				if (mInstance == null) {
					try {
						mInstance = new OkHttpClientManager();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return mInstance;
	}

	public Handler getDelivery() {
		return mDelivery;
	}

	public OkHttpClient getOkHttpClient() {
		return mOkHttpClient;
	}

	/**
	 * 执行请求
	 * 
	 * @param request
	 * @param callback
	 */
	public void execute(final Request request, ResultCallback callback) {
		if (callback == null)
			callback = ResultCallback.DEFAULT_RESULT_CALLBACK;
		final ResultCallback resCallBack = callback;
		resCallBack.onBefore(request);
		mOkHttpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(final Request request, final IOException e) {
				// TODO when cancel , should do?
				sendFailResultCallback(request, e, resCallBack);
			}

			@Override
			public void onResponse(final Response response) {
				if (response.code() >= 400 && response.code() <= 599) {
					try {
						sendFailResultCallback(request, new RuntimeException(
								response.body().string()), resCallBack);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;
				}

				try {
					final String string = response.body().StringContent();
					if (resCallBack.mType == String.class) {
						sendSuccessResultCallback(string, resCallBack,response.headers());
					} else {
						// 其他类型的让他返回object的
						Object o = mGson.fromJson(string, resCallBack.mType);
						sendSuccessResultCallback(o, resCallBack,response.headers());
					}
				} catch (IOException e) {
					sendFailResultCallback(response.request(), e, resCallBack);
				} catch (com.google.gson.JsonParseException e)// Json解析的错误
				{
					sendFailResultCallback(response.request(), e, resCallBack);
				}

			}
		});
	}

	/**
	 * 直接返回结果对象
	 * 
	 * @param request
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	public <T> T execute(Request request, Class<T> clazz) throws IOException {
		Call call = mOkHttpClient.newCall(request);
		Response execute = call.execute();
		String respStr = execute.body().string();
		return mGson.fromJson(respStr, clazz);
	}

	/**
	 * 异步分发错误信息到接口
	 * 
	 * @param request
	 * @param e
	 * @param callback
	 */
	public void sendFailResultCallback(final Request request,
			final Exception e, final ResultCallback callback) {
		if (callback == null)
			return;

		mDelivery.post(new Runnable() {
			@Override
			public void run() {
				callback.onError(request, e);
				callback.onAfter();
			}
		});
	}

	/**
	 * 异步分发成功信息到接口
	 * 
	 * @param request
	 * @param e
	 * @param callback
	 */
	public void sendSuccessResultCallback(final Object object,
			final ResultCallback callback,final Headers headers) {
		if (callback == null)
			return;
		mDelivery.post(new Runnable() {
			@Override
			public void run() {
				callback.onResponse(object,headers);
				callback.onAfter();
			}
		});
	}

	/**
	 * 取消任务
	 * 
	 * @param tag
	 */
	public void cancelTag(Object tag) {
		mOkHttpClient.cancel(tag);
	}

	public void setCertificates(InputStream... certificates) {
		setCertificates(certificates, null, null);
	}

	/**
	 * 证书单向验证
	 * 
	 * @param certificates
	 * @return
	 */
	private TrustManager[] prepareTrustManager(InputStream... certificates) {
		if (certificates == null || certificates.length <= 0)
			return null;
		try {

			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);
			int index = 0;
			for (InputStream certificate : certificates) {
				String certificateAlias = Integer.toString(index++);
				keyStore.setCertificateEntry(certificateAlias,
						certificateFactory.generateCertificate(certificate));
				try {
					if (certificate != null)
						certificate.close();
				} catch (IOException e)

				{
				}
			}
			TrustManagerFactory trustManagerFactory = null;

			trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			TrustManager[] trustManagers = trustManagerFactory
					.getTrustManagers();

			return trustManagers;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * 证书双向验证
	 * 
	 * @param bksFile
	 * @param password
	 * @return
	 */
	private KeyManager[] prepareKeyManager(InputStream bksFile, String password) {
		try {
			if (bksFile == null || password == null)
				return null;

			KeyStore clientKeyStore = KeyStore.getInstance("BKS");
			clientKeyStore.load(bksFile, password.toCharArray());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(clientKeyStore, password.toCharArray());
			return keyManagerFactory.getKeyManagers();

		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setCertificates(InputStream[] certificates,
			InputStream bksFile, String password) {
		try {
			TrustManager[] trustManagers = prepareTrustManager(certificates);
			KeyManager[] keyManagers = prepareKeyManager(bksFile, password);
			SSLContext sslContext = SSLContext.getInstance("TLS");

			sslContext.init(keyManagers,
					new TrustManager[] { new MyTrustManager(
							chooseTrustManager(trustManagers)) },
					new SecureRandom());
			mOkHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
	}

	private X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
		for (TrustManager trustManager : trustManagers) {
			if (trustManager instanceof X509TrustManager) {
				return (X509TrustManager) trustManager;
			}
		}
		return null;
	}

	private class MyTrustManager implements X509TrustManager {
		private X509TrustManager defaultTrustManager;
		private X509TrustManager localTrustManager;

		public MyTrustManager(X509TrustManager localTrustManager)
				throws NoSuchAlgorithmException, KeyStoreException {
			TrustManagerFactory var4 = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			var4.init((KeyStore) null);
			defaultTrustManager = chooseTrustManager(var4.getTrustManagers());
			this.localTrustManager = localTrustManager;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {

		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			try {
				defaultTrustManager.checkServerTrusted(chain, authType);
			} catch (CertificateException ce) {
				localTrustManager.checkServerTrusted(chain, authType);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}

}
