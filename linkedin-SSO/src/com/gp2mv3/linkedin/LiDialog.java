package com.gp2mv3.linkedin;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceException;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;

/**
 * This class is based on the TwDialog of the twitter-android-sdk of sugree.
 * It provides an access to LinkedIn with a simple authentication window.
 * @author gp2mv3
 */

public class LiDialog extends Dialog {
	public static final String TAG = "Linked In";
	
    public static final String OAUTH_CALLBACK_SCHEME = "x-oauthflow-linkedin";
    public static final String OAUTH_CALLBACK_HOST = "callback";
    public static final String CALLBACK_URI = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
    private static final String CANCEL_URI = OAUTH_CALLBACK_HOST + "://cancel";
    
    
    static final int LI_BLUE = 0xFF449DC5;
    static final float[] DIMENSIONS_LANDSCAPE = {460, 260};
    static final float[] DIMENSIONS_PORTRAIT = {280, 420};
    static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    static final int MARGIN = 4;
    static final int PADDING = 2;
    
	private int mIcon;
    private String mUrl;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;
    private Handler mHandler;

    private DialogListener mListener;
    private LinkedInOAuthService mOauthServ;
    private LinkedInRequestToken mRequest;


    public LiDialog(Context context,final LinkedInOAuthService oauthService, LinkedInRequestToken requestToken, DialogListener listener, int icon)
    {
        super(context);
        mOauthServ = oauthService;
        mRequest = requestToken;
        mListener = listener;
		mIcon = icon;
		mHandler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");
        
        mContent = new LinearLayout(getContext());
        mContent.setOrientation(LinearLayout.VERTICAL);
        setUpTitle();
        setUpWebView();
                
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        final float scale = getContext().getResources().getDisplayMetrics().density;
        float[] dimensions = display.getWidth() < display.getHeight() ?
        		DIMENSIONS_PORTRAIT : DIMENSIONS_LANDSCAPE;
        addContentView(mContent, new FrameLayout.LayoutParams(
        		(int) (dimensions[0] * scale + 0.5f),
        		(int) (dimensions[1] * scale + 0.5f)));

        retrieveRequestToken();
    }

    @Override
	public void show() {
		super.show();
		mSpinner.show();
	}

	private void setUpTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable icon = getContext().getResources().getDrawable(mIcon);
        mTitle = new TextView(getContext());
        mTitle.setText("Linked In");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(LI_BLUE);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        mContent.addView(mTitle);
    }
    
    private void retrieveRequestToken() {
        mSpinner.show();
        new Thread() {
        	@Override
        	public void run() {
        			mUrl = mRequest.getAuthorizationUrl();
        	    	mWebView.loadUrl(mUrl);
        	    	Log.i("URL", mUrl);
        	}
        }.start();
    }
    
    private void retrieveAccessToken(final String url) {
        mSpinner.show();
    	new Thread() {
    		@Override
    		public void run() {
    			Uri uri = Uri.parse(url);
    			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
    			try
    			{
    				LinkedInAccessToken accessToken = mOauthServ.getOAuthAccessToken(mRequest, verifier);
    				mListener.onComplete(accessToken);
    			}
    			catch(LinkedInOAuthServiceException e) { mListener.onError(new DialogError(e.toString(), -1)); }

    			mHandler.post(new Runnable() {
    				@Override
    				public void run() {
    					mSpinner.dismiss();
    					LiDialog.this.dismiss();
    				}					
    			});
    		}
    	}.start();
    }
    
    private void setUpWebView() {
        mWebView = new WebView(getContext());
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setHorizontalScrollBarEnabled(true);
        mWebView.setWebViewClient(new LiDialog.TwWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(false);

        mWebView.setLayoutParams(FILL);
        //mWebView.setOverScrollMode();
        mContent.addView(mWebView);
    }

    private class TwWebViewClient extends WebViewClient {
		@Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirect URL: " + url);
            if (url.startsWith(CALLBACK_URI)) {
            	retrieveAccessToken(url);
            } else if (url.startsWith(CANCEL_URI)) {
                mListener.onCancel();
                LiDialog.this.dismiss();
            }
            else
            	mWebView.loadUrl(url);
            
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(new DialogError(description, errorCode));
            LiDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "WebView loading URL: " + url);
            super.onPageStarted(view, url, favicon);
            if (mSpinner.isShowing()) {
            	mSpinner.dismiss();
            }
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }
            
            mSpinner.dismiss();
        }   
        
    }
    
	public static interface DialogListener {
		public void onComplete(LinkedInAccessToken accessToken);
		public void onLinkedInError(LinkedInError e);
		public void onError(DialogError e);
		public void onCancel();
	}
}
