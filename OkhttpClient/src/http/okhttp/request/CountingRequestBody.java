package http.okhttp.request;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 上传进度条
 * 
 * @author Administrator
 * 
 */
public class CountingRequestBody extends RequestBody {

	protected RequestBody delegate;
	protected Listener listener;

	protected CountingSink countingSink;

	public CountingRequestBody(RequestBody delegate, Listener listener) {
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public MediaType contentType() {
		return delegate.contentType();
	}

	@Override
	public long contentLength() {
		try {
			return delegate.contentLength();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException {
		BufferedSink bufferedSink;

		countingSink = new CountingSink(sink);
		bufferedSink = Okio.buffer(countingSink);

		delegate.writeTo(bufferedSink);

		bufferedSink.flush();
	}

	/**
	 * 重写流任务监听写入变化好用进度条
	 * 
	 * @author Administrator
	 * 
	 */
	protected final class CountingSink extends ForwardingSink {

		private long bytesWritten = 0;

		public CountingSink(Sink delegate) {
			super(delegate);
		}

		@Override
		public void write(Buffer source, long byteCount) throws IOException {
			super.write(source, byteCount);

			bytesWritten += byteCount;
			listener.onRequestProgress(bytesWritten, contentLength());
		}

	}

	public static interface Listener {

		public void onRequestProgress(long bytesWritten, long contentLength);

	}

}