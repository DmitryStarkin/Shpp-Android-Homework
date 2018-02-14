/**
 * Copyright © 2017 Dmitry Starkin Contacts: t0506803080@gmail.com. All rights reserved
 *
 */
package com.hplasplas.cam_capture.models;

import android.graphics.Bitmap;

import java.io.File;

/**
 * Created by StarkinDG on 07.03.2017.
 */

public class ListItemModel {
    
    private File mPictureFile;
    private Bitmap mPicturePreview;
    
    public ListItemModel(File pictureFile) {
        
        this.mPictureFile = pictureFile;
    }
    
    public File getPictureFile() {
        
        return mPictureFile;
    }
    
    public void setPictureFile(File pictureFile) {
        
        this.mPictureFile = pictureFile;
    }
    
    public Bitmap getPicturePreview() {
        
        return mPicturePreview;
    }
    
    public void setPicturePreview(Bitmap picturePreview) {
        
        this.mPicturePreview = picturePreview;
    }
    
    public void clearPreview() {
        
            setPicturePreview(null);
    }
}
