/*
 * Copyright © 2018. Dmitry Starkin Contacts: t0506803080@gmail.com
 *
 * This file is part of cam_capture
 *
 *     cam_capture is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *    cam_capture is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with cam_capture  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hplasplas.cam_capture.loaders;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;

import com.hplasplas.cam_capture.ThisApplication;
import com.hplasplas.cam_capture.util.MainHandler;
import com.starsoft.bmutil.BitmapTools;

import java.lang.ref.WeakReference;

import static com.hplasplas.cam_capture.setting.Constants.BITMAP_ROTATE_ANGLE;
import static com.hplasplas.cam_capture.setting.Constants.FILE_NAME_TO_LOAD;
import static com.hplasplas.cam_capture.setting.Constants.LIST_INDEX;
import static com.hplasplas.cam_capture.setting.Constants.MESSAGE_BITMAP_LOAD;
import static com.hplasplas.cam_capture.setting.Constants.NO_PICTURE_FILE_NAME;
import static com.hplasplas.cam_capture.setting.Constants.REQUESTED_ORIENTATION;
import static com.hplasplas.cam_capture.setting.Constants.REQUESTED_PICTURE_HEIGHT;
import static com.hplasplas.cam_capture.setting.Constants.REQUESTED_PICTURE_WIDTH;
import static com.hplasplas.cam_capture.setting.Constants.REQUESTED_SAMPLE_SIZE;

/**
 * Created by StarkinDG on 15.03.2017.
 */

public class BitmapInThreadLoader implements Runnable {
    
    private final String TAG = getClass().getSimpleName();
    
    private int mIndex;
    private int mRequestedHeight;
    private int mRequestedWidth;
    private int mSampleSize;
    private int requestedOrientation;
    private String mFileName;
    private WeakReference<BitmapLoaderListener> mListener;
    private Bitmap mBitmap;
    
    public BitmapInThreadLoader(BitmapLoaderListener listener, Bundle args) {
        
        mListener = new WeakReference<>(listener);
        mFileName = args.getString(FILE_NAME_TO_LOAD);
        mRequestedHeight = args.getInt(REQUESTED_PICTURE_HEIGHT);
        mRequestedWidth = args.getInt(REQUESTED_PICTURE_WIDTH);
        mSampleSize = args.getInt(REQUESTED_SAMPLE_SIZE);
        requestedOrientation = args.getInt(REQUESTED_ORIENTATION);
        mIndex = args.getInt(LIST_INDEX);
    }
    
    @Override
    public void run() {
        
        if (mListener.get() != null && mListener.get().isRelevant()) {
            BitmapTools bitmapTools = new BitmapTools();
            mBitmap = bitmapTools.LoadPictureFromFile(mFileName, mRequestedWidth, mRequestedHeight, mSampleSize);
            if (mBitmap == null) {
                
                mBitmap = bitmapTools.loadPictureFromAssets(ThisApplication.getMainContext(),
                        NO_PICTURE_FILE_NAME, mRequestedWidth, mRequestedHeight, mSampleSize);
            }
            if (mBitmap != null) {
                if ((requestedOrientation == Configuration.ORIENTATION_PORTRAIT && mBitmap.getHeight() < mBitmap.getWidth()) ||
                        (requestedOrientation == Configuration.ORIENTATION_LANDSCAPE && mBitmap.getHeight() > mBitmap.getWidth())) {
                    mBitmap = bitmapTools.rotate(mBitmap, BITMAP_ROTATE_ANGLE);
                }
                if (mListener.get() != null && mListener.get().isRelevant()) {
                    MainHandler handler = MainHandler.getInstance();
                    
                    //TODO think maybe it's not necessary XZ
                    synchronized (handler) {
                        Message message = MainHandler.getInstance().obtainMessage(MESSAGE_BITMAP_LOAD, this);
                        message.sendToTarget();
                    }
                }
            }
        }
    }
    
    //TODO to understand maybe it's not necessary
    private void clearReference() {
        
        mBitmap = null;
        mFileName = null;
    }
    
    public void onPostBitmapLoad() {
        
        try {
            if (mListener.get() != null && mListener.get().isRelevant()) {
                mListener.get().onBitmapLoadFinished(mIndex, mFileName, mBitmap);
            } else {
                if (mBitmap != null) {
                    mBitmap.recycle();
                }
            }
        } finally {
            clearReference();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        
        //TODO to think it may not need to compare listeners
        return (obj instanceof BitmapInThreadLoader) && ((BitmapInThreadLoader) obj).mFileName.equals(this.mFileName) &&
                (((BitmapInThreadLoader) obj).mListener.get() == this.mListener.get());
    }
    
    public interface BitmapLoaderListener {
        
        void onBitmapLoadFinished(int index, String fileName, Bitmap bitmap);
        
        boolean isRelevant();
    }
}

