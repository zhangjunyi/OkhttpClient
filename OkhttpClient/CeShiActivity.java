package com.yagou.yggx.android.activity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.example.ygsm.Constant;
import com.example.ygsm.R;
import com.example.ygsm.modle.CopyOfLoginInterface;
import com.example.ygsm.modle.L;
import com.example.ygsm.presenter.CopyOfLoginPresenter;
import com.example.ygsm.presenter.LodingPresenterActivity;

public class CeShiActivity extends
		LodingPresenterActivity<CopyOfLoginInterface, CopyOfLoginPresenter> implements
		CopyOfLoginInterface {
	private CopyOfLoginPresenter l;

	@Override
	public void loadData() {
		// TODO Auto-generated method stub
		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				l.getData(
						"http://apis.baidu.com/tngou/drug/search",
						0);
			}
		}, 10000);
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(View.inflate(this, R.layout.loginpsd_alter, null));
	}

	


	@Override
	public void onFail(int requestcode, Exception exception) {
		// TODO Auto-generated method stub
		changeShow(Constant.NO_WIFI);
	}

	@Override
	public void onSuceed(int requestcode, L data) {
		// TODO Auto-generated method stub
		changeShow(Constant.NO_DATA);

	}

	@Override
	protected CopyOfLoginPresenter createPresenter() {
		// TODO Auto-generated method stub
		return (l=new CopyOfLoginPresenter());
	}

}
