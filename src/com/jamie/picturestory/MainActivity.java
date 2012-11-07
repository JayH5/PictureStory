package com.jamie.picturestory;

import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";
	
	private static final String IMAGE_CACHE_DIR = "images";
	
	private static final String SAVE_URIS_KEY = "Uris";
	private static final String SAVE_AUDIO_PATH_KEY = "Audio path";
	private static final String SAVE_MODE_KEY = "Mode";
	private static final String SAVE_TRANSITIONS_KEY = "Transitions";
	private static final String SAVE_CAMERA_URI = "Camera Uri";
	private static final String SAVE_HAS_RECORDED_KEY = "Has recorded";
	
	private static final int IMAGE_PICK = 0;
	
	// Viewpager and associated objects
	private ViewPager mPager;
	private ListPagerAdapter mAdapter;
	private ImageResizer mImageWorker;
	
	// Records whether we're in image editing or recording mode
	private int mMode;
	
	// UI objects
	private LinearLayout mImagesButtonBar;
	private ImageButton mCompleteImagesButton;
	private ImageButton mAddImageButton;
	private ImageButton mRemoveImageButton;
	
	private LinearLayout mAudioButtonBar;
	private ImageButton mEditImagesButton;
	private ImageButton mPlayButton;
	private ImageButton mRecordButton;
	
	private ProgressBar mRecordIndicator;
	private ProgressBar mPlayIndicator;
	
	// Story transition recording
	private ArrayList<StoryTransition> mStoryTransitions;
	private MilliTimer mTimer;
	private Handler mHandler;
	
	// Audio record class variables
	private String mAudioFilePath;
	
    private boolean mStartRecording = true;
    private MediaRecorder mRecorder = null;

    private boolean mStartPlaying = true;
    private MediaPlayer mPlayer = null;
    
    private boolean mHasRecorded = false;
    
    // Need to keep track of uris provided to camera because it doesn't return them
    private String mLastCameraUri = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // Remove action bar / title
        setContentView(R.layout.main);
		
        // Get the display size so we can resize images
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        
        // Create the image worker that fetches and processes images
        mImageWorker = new ImageResizer(this, height, width);
        mImageWorker.setImageCache(ImageCache.findOrCreateCache(this, IMAGE_CACHE_DIR));
        
        // Check for any saved instance state data so we can restore the app after
        // configuration change (like rotation).
        if (savedInstanceState != null) {
        	ArrayList<Uri> pageUris = savedInstanceState.getParcelableArrayList(SAVE_URIS_KEY);
        	mAdapter = new ListPagerAdapter(pageUris, getSupportFragmentManager());
        	mAudioFilePath = savedInstanceState.getString(SAVE_AUDIO_PATH_KEY);
        	mMode = savedInstanceState.getInt(SAVE_MODE_KEY);
        	mStoryTransitions = savedInstanceState.getParcelableArrayList(SAVE_TRANSITIONS_KEY);
        	mLastCameraUri = savedInstanceState.getString(SAVE_CAMERA_URI);
        	mHasRecorded = savedInstanceState.getBoolean(SAVE_HAS_RECORDED_KEY);
        } else {
        	mAdapter = new ListPagerAdapter(getSupportFragmentManager());
        	mAudioFilePath = Utils.getOutputAudioFilePath(getApplicationContext());
        	mMode = 0;
        }
        
        // Create the viewpager and set its adapter
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.pager_margin));
        mPager.setOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {
        	
        	@Override
        	public void onPageSelected(int position) {
        		if (!mStartRecording) { // If we're recording
        			mStoryTransitions.add(new StoryTransition(position, mTimer.getTime()));
        		}
            	//Log.d(TAG, "Pager position: " + position + ", at time: " + System.currentTimeMillis());
            }
        });
        
        // ALL the UI elements
        mImagesButtonBar = (LinearLayout) findViewById(R.id.images_button_bar);
        mAudioButtonBar = (LinearLayout) findViewById(R.id.audio_button_bar);
        
        switch(mMode) {
        case 0:
        	mImagesButtonBar.setVisibility(View.VISIBLE);
        	mAudioButtonBar.setVisibility(View.INVISIBLE);
        	break;
        case 1:
        	mImagesButtonBar.setVisibility(View.INVISIBLE);
        	mAudioButtonBar.setVisibility(View.VISIBLE);
        	break;
        default:
        	// Shouldn't happen
        	mMode = 0;
        }
        
        // Add images mode
        mCompleteImagesButton = (ImageButton) findViewById(R.id.complete_images_button);
        mCompleteImagesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toggleMode();
			}
		});
        
        mAddImageButton = (ImageButton) findViewById(R.id.add_image_button);
        mAddImageButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				openImageIntent();
			}
		});
        
        mRemoveImageButton = (ImageButton) findViewById(R.id.remove_image_button);
        mRemoveImageButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				removeCurrentItem();				
			}
		});
        
        if (mAdapter.getCount() == 0) {
        	mRemoveImageButton.setEnabled(false);
        }
        
        // Record mode buttons        
        mEditImagesButton = (ImageButton) findViewById(R.id.edit_images_button);
        mEditImagesButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mHasRecorded) {
					confirmModeChange();
				} else {
					toggleMode();
				}
			}
		});
        
        mRecordButton = (ImageButton) findViewById(R.id.record_button);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toggleRecording();	
			}
		});
        
        mPlayButton = (ImageButton) findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				togglePlayback();
			}
		});
        if (mHasRecorded) {
        	mPlayButton.setEnabled(true);
        }
                  
        mRecordIndicator = (ProgressBar) findViewById(R.id.record_indicator);
        mPlayIndicator = (ProgressBar) findViewById(R.id.play_indicator);
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
    	super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

    	if (requestCode == IMAGE_PICK) {
        	if(resultCode == RESULT_OK) {
        		int newPosition = 0;
        		if (mAdapter.getCount() > 0) {
        			newPosition = mPager.getCurrentItem() + 1;
        		}
        		Log.d(TAG, "Insert new item position: " + newPosition);
        		if (imageReturnedIntent != null) {
        			Uri selectedImage = imageReturnedIntent.getData();
	       			mAdapter.addItem(newPosition, selectedImage);
        		} else { // The camera is silly and returns a null URI
        			mAdapter.addItem(newPosition, Uri.parse(mLastCameraUri));
        		}
        		mPager.setCurrentItem(newPosition, true);
        		mRemoveImageButton.setEnabled(true);
        		mCompleteImagesButton.setEnabled(true);
        	} else if (resultCode == RESULT_CANCELED) {
        		// Do nothing?
        	}
    	}
	}
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putParcelableArrayList(SAVE_URIS_KEY, mAdapter.getPageUris());
    	outState.putString(SAVE_AUDIO_PATH_KEY, mAudioFilePath);
    	outState.putInt(SAVE_MODE_KEY, mMode);
    	outState.putParcelableArrayList(SAVE_TRANSITIONS_KEY, mStoryTransitions);
    	outState.putString(SAVE_CAMERA_URI, mLastCameraUri);
    	outState.putBoolean(SAVE_HAS_RECORDED_KEY, mHasRecorded);
        super.onSaveInstanceState(outState);
    }
    
    // We must release the recorder and the player upon exiting the app 
    // Also, remove all runnables in the handler
    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mHandler != null) {
        	mHandler.removeCallbacks(null);
        }
    }
    
    private void removeCurrentItem() {
    	int position = mPager.getCurrentItem();
    	if (position > 0) {
    		mPager.setCurrentItem(position - 1, true);
    	}
    	mAdapter.removeItem(position);
    	if (mAdapter.getCount() == 0) {
    		mRemoveImageButton.setEnabled(false);
    		mCompleteImagesButton.setEnabled(false);
    	}
    }
    
    private void toggleMode() {
    	switch(mMode) {
        case 0:
        	mImagesButtonBar.setVisibility(View.INVISIBLE);
        	mAudioButtonBar.setVisibility(View.VISIBLE);
        	mMode = 1;
        	break;
        case 1:
        	mImagesButtonBar.setVisibility(View.VISIBLE);
        	mAudioButtonBar.setVisibility(View.INVISIBLE);
        	mMode = 0;
        	mHasRecorded = false;
        	break;
        default:
        	// Shouldn't happen
        	mMode = 0;
        }
    }
    
    
    
    public ImageWorker getImageWorker() {
    	return mImageWorker;
    }

    private void toggleRecording() {
        if (mStartRecording) {
            startRecording();
        } else {
            stopRecording();
        }
        mStartRecording = !mStartRecording;
    }

    private void togglePlayback() {
        if (mStartPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
        mStartPlaying = !mStartPlaying;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mAudioFilePath);
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					togglePlayback();				
				}
			});
            mPlayer.prepare();
            mPlayer.start();
            
            startTransitions();
            
            mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
            mPlayButton.setContentDescription(getResources().getString(R.string.started_play_button_text));
            
            mPlayIndicator.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }
    
    private void startTransitions() {
    	// Move to the first page
    	int firstPage = mStoryTransitions.get(0).getPosition();
    	mPager.setCurrentItem(firstPage, true);
    	
    	// Queue all the transitions in a handler
    	mHandler = new Handler();
        for (int i = 1; i < mStoryTransitions.size() - 1; i++) {
        	final StoryTransition transition = mStoryTransitions.get(i);
        	Runnable runner = new Runnable() {
        		public void run() {
        			mPager.setCurrentItem(transition.getPosition(), true);
        		}
        	};
        	mHandler.postDelayed(runner, transition.getTime());
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        
        // Stop any transitions
        if (mHandler != null) {
        	mHandler.removeCallbacks(null);
        }
        
        mPlayButton.setImageDrawable(getResources().getDrawable(R.drawable.av_play));
        mPlayButton.setContentDescription(getResources().getString(R.string.stopped_play_button_text));
        
        mPlayIndicator.setVisibility(View.INVISIBLE);
    }

    private void startRecording() {
    	mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mAudioFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
        
        mTimer = new MilliTimer();
        mStoryTransitions = new ArrayList<StoryTransition>();
        mStoryTransitions.add(new StoryTransition(mPager.getCurrentItem(), 0));
        
        mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.stop));
        mRecordButton.setContentDescription(getResources().getString(R.string.started_record_button_text));
        
        mRecordIndicator.setVisibility(View.VISIBLE);
        
        mPlayButton.setEnabled(false);
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        
        mStoryTransitions.add(new StoryTransition(-1, mTimer.getTime()));
        
        mRecordButton.setImageDrawable(getResources().getDrawable(R.drawable.device_access_mic));
        mRecordButton.setContentDescription(getResources().getString(R.string.stopped_record_button_text));
        
        mRecordIndicator.setVisibility(View.INVISIBLE);
        
        mPlayButton.setEnabled(true);
        mHasRecorded = true;
    }
    
    private void openImageIntent() {
        // Camera intent
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoUri = Utils.getOutputPhotoFileUri(); // create a file to save the image
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        mLastCameraUri = photoUri.toString();
        
        // Gallery intent
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
        // Create chooser intent with gallery options, then add camera options
        Intent chooserIntent = Intent.createChooser(galleryIntent, "Select source");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { cameraIntent });
        
        // Launch the intent
        startActivityForResult(chooserIntent, IMAGE_PICK);
    }
    
    private void confirmModeChange() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.dialog_message)
    			.setTitle(R.string.dialog_title)
    			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				toggleMode();
			}
		})
    			.setNegativeButton(R.string.cancel, null);
    	builder.show();
    }
}