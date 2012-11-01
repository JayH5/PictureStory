package com.jamie.picturestory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";
	private static final String IMAGE_CACHE_DIR = "images";
	private static final String SAVE_STATE_KEY = "Uris";
	private static final int IMAGE_PICK = 0;
	
	// Viepager and image class variables
	private ViewPager mPager;
	private ListPagerAdapter mAdapter;
	private ImageResizer mImageWorker;
	
	private Button mButton;
	
	// Audio record class variables
    private static String mFileName = null;

    private Button mRecordButton;
    private boolean mStartRecording = true;
    private MediaRecorder mRecorder = null;

    private Button mPlayButton;
    private boolean mStartPlaying = true;
    private MediaPlayer mPlayer = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
		
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        
        mImageWorker = new ImageResizer(this, height, width);
        mImageWorker.setImageCache(ImageCache.findOrCreateCache(this, IMAGE_CACHE_DIR));
        
        if (savedInstanceState != null) {
        	ArrayList<Uri> pageUris = savedInstanceState.getParcelableArrayList(SAVE_STATE_KEY);
        	mAdapter = new ListPagerAdapter(pageUris, getSupportFragmentManager());
        } else {
        	mAdapter = new ListPagerAdapter(getSupportFragmentManager());
        }
        
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.pager_margin));
        
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openImageIntent();
			}
		});
        
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
        
        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onPlay(mStartPlaying);
                if (mStartPlaying) {
                    mPlayButton.setText("Stop playing");
                } else {
                    mPlayButton.setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
			}
		});
        
        mRecordButton = (Button) findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onRecord(mStartRecording);
                if (mStartRecording) {
                    mRecordButton.setText("Stop recording");
                } else {
                    mRecordButton.setText("Start recording");
                }
                mStartRecording = !mStartRecording;				
			}
		});
        
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
    	super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

    	if (requestCode == IMAGE_PICK) {
        	if(resultCode == RESULT_OK) {
        		Uri selectedImage = imageReturnedIntent.getData();
	
        	    Log.d(TAG, "Intent returned uri: " + selectedImage);

        	    mAdapter.addItem(mAdapter.getCount(), selectedImage);
        	    mAdapter.notifyDataSetChanged();
        	} else if (resultCode == RESULT_CANCELED) {
        		// Do nothing?
        	}
    	}
    	
	}
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putParcelableArrayList(SAVE_STATE_KEY, mAdapter.getPageUris());
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release(); // Must release the recorder
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public ImageWorker getImageWorker() {
    	return mImageWorker;
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {    	
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }
    
    private void openImageIntent() {
        // Camera intent
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri fileUri = getOutputPhotoFileUri(); // create a file to save the image
        Log.d(TAG, "Camera intent uri: " + fileUri);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        
        // Gallery intent
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
        // Create chooser intent with gallery options, then add camera options
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { cameraIntent });
        
        // Launch the intent
        startActivityForResult(chooserIntent, IMAGE_PICK);
    }

    /** Create a File for saving an image or video */
    private static Uri getOutputPhotoFileUri(){
    	// TODO: Check that the SDCard is mounted using Environment.getExternalStorageState().

    	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
    				Environment.DIRECTORY_PICTURES), "PictureStory");
    	
    	// Create the storage directory if it does not exist
    	if (!mediaStorageDir.exists()) {
    		if (!mediaStorageDir.mkdirs()) { // Media storage directory made -here-
    			Log.e(TAG, "failed to create directory");
    			return null;
    		}
    	}

    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    	File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

    	return Uri.fromFile(mediaFile);
    }
    
    private class ListPagerAdapter extends FragmentStatePagerAdapter {
		private ArrayList<Uri> mPageUris;
		
		ListPagerAdapter(FragmentManager fm) {
			super(fm);
			mPageUris = new ArrayList<Uri>();
		}
		
		ListPagerAdapter(ArrayList<Uri> pageUris, FragmentManager fm) {
			super(fm);
			mPageUris = pageUris;
		}
		
		@Override
		public Fragment getItem(int position) {
			return ImageFragment.newInstance(mPageUris.get(position));
		}
		
		@Override
		public int getCount() {
			return mPageUris.size();
		}
		
		public void addItem(int position, Uri pageUri) {
			mPageUris.add(position, pageUri);
		}
		
		@Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // Cancel any ongoing work on the fragment
			final ImageFragment fragment = (ImageFragment) object;
            fragment.cancelWork();
            
            super.destroyItem(container, position, object);
            
            // Remove from arraylist
            mPageUris.remove(position);
        }
		
		public ArrayList<Uri> getPageUris() {
			return mPageUris;
		}
	}
}
