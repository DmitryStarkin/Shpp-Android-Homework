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
package com.hplasplas.cam_capture.managers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.hplasplas.cam_capture.ThisApplication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.hplasplas.cam_capture.setting.Constants.DEFAULT_FILE_NAME_PREFIX;
import static com.hplasplas.cam_capture.setting.Constants.FILE_NAME_SUFFIX;
import static com.hplasplas.cam_capture.setting.Constants.NEED_PRIVATE_FOLDER;
import static com.hplasplas.cam_capture.setting.Constants.PICTURE_FOLDER_NAME;
import static com.hplasplas.cam_capture.setting.Constants.TIME_STAMP_PATTERN;

/**
 * Created by StarkinDG on 26.03.2017.
 */

public class FileSystemManager {
    
    public static File generateFileForPicture() {
        
        String fileName = DEFAULT_FILE_NAME_PREFIX + new SimpleDateFormat(TIME_STAMP_PATTERN,
                Locale.getDefault()).format(new Date()) + FILE_NAME_SUFFIX;
        return generateFileForPicture(fileName);
    }
    
    public static File generateFileForPicture(String fileName) {
        
        return new File(getDirectory().getPath() + "/" + fileName);
    }
    
    private static File getDirectory(boolean needPrivate) {
        
        File dir;
        if (needPrivate) {
            dir = ThisApplication.getInstance().getExternalFilesDir(PICTURE_FOLDER_NAME);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    ContextCompat.checkSelfPermission(ThisApplication.getMainContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                dir = ThisApplication.getInstance().getExternalFilesDir(PICTURE_FOLDER_NAME);
            } else {
                dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PICTURE_FOLDER_NAME);
            }
        }
        if (dir != null && !dir.exists() && !dir.mkdir()) {
            throw new IllegalStateException("Dir create error");
        }
        return dir;
    }
    
    public static File getDirectory() {
        
        return getDirectory(NEED_PRIVATE_FOLDER);
    }
    
    private static int getFilesCount(File dir) {
        
        if (dir == null || dir.listFiles() == null) {
            return 0;
        } else {
            return dir.listFiles().length;
        }
    }
    
    public static int getFilesCount() {
        
        return getFilesCount(getDirectory());
    }
}
