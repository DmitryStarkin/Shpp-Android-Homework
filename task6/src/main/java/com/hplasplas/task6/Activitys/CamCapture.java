package com.hplasplas.task6.Activitys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.hplasplas.task6.Adapters.PictureInFolderAdapter;
import com.hplasplas.task6.Dialogs.FileNameInputDialog;
import com.hplasplas.task6.Dialogs.RenameErrorDialog;
import com.hplasplas.task6.Loaders.BitmapLoader;
import com.hplasplas.task6.Models.ListItemModel;
import com.hplasplas.task6.R;
import com.hplasplas.task6.Util.intQueue;
import com.starsoft.recyclerViewItemClickSupport.ItemClickSupport;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.hplasplas.task6.Setting.Constants.DEBUG;
import static com.hplasplas.task6.Setting.Constants.DEFAULT_FILE_NAME_PREFIX;
import static com.hplasplas.task6.Setting.Constants.ERROR_DIALOG_TAG;
import static com.hplasplas.task6.Setting.Constants.FILE_NAME_SUFFIX;
import static com.hplasplas.task6.Setting.Constants.FILE_NAME_TO_LOAD;
import static com.hplasplas.task6.Setting.Constants.FILE_RENAME_DIALOG_TAG;
import static com.hplasplas.task6.Setting.Constants.GET_PICTURE_REQUEST_CODE;
import static com.hplasplas.task6.Setting.Constants.MAIN_PICTURE_LOADER_ID;
import static com.hplasplas.task6.Setting.Constants.NEED_PRIVATE_FOLDER;
import static com.hplasplas.task6.Setting.Constants.NO_EXISTING_FILE_NAME;
import static com.hplasplas.task6.Setting.Constants.PICTURE_FOLDER_NAME;
import static com.hplasplas.task6.Setting.Constants.PREFERENCES_FILE;
import static com.hplasplas.task6.Setting.Constants.PREF_FOR_LAST_FILE_NAME;
import static com.hplasplas.task6.Setting.Constants.PREVIEW_PICTURE_HEIGHT;
import static com.hplasplas.task6.Setting.Constants.PREVIEW_PICTURE_LOADER_START_ID;
import static com.hplasplas.task6.Setting.Constants.PREVIEW_PICTURE_WIDTH;
import static com.hplasplas.task6.Setting.Constants.REQUESTED_PICTURE_HEIGHT;
import static com.hplasplas.task6.Setting.Constants.REQUESTED_PICTURE_WIDTH;
import static com.hplasplas.task6.Setting.Constants.ROWS_IN_TABLE;
import static com.hplasplas.task6.Setting.Constants.TIME_STAMP_PATTERN;

public class CamCapture extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Bitmap>, PopupMenu.OnMenuItemClickListener, FileNameInputDialog.FileNameInputDialogListener {
    
    private final String TAG = getClass().getSimpleName();
    public ArrayList<ListItemModel> filesItemList;
    private boolean mainPictureLoaded;
    private boolean canComeBack;
    private intQueue myPreviewInLoad;
    private int contextMenuPosition = -1;
    private SharedPreferences myPreferences;
    private ImageView myImageView;
    private Bitmap myMainBitmap;
    private Button myButton;
    private ProgressBar mainProgressBar;
    private RecyclerView myRecyclerView;
    private PictureInFolderAdapter myPictureInFolderAdapter;
    private File myCurrentPictureFile;
    private File myPictureDirectory;
    private int myMainImageViewHeight;
    private int myMainImageViewWidth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_capture_activity);
        
        myImageView = (ImageView) findViewById(R.id.foto_frame);
        myMainImageViewHeight = myImageView.getHeight();
        myMainImageViewWidth = myImageView.getWidth();
        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);
        mainProgressBar.setVisibility(View.VISIBLE);
        myButton = (Button) findViewById(R.id.foto_button);
        myButton.setOnClickListener(this);
        
        myPictureDirectory = getDirectory(NEED_PRIVATE_FOLDER);
        initFilesItemList();
        
        myRecyclerView = (RecyclerView) findViewById(R.id.foto_list);
        myRecyclerView.setHasFixedSize(true);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            myRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        } else {
            myRecyclerView.setLayoutManager(new GridLayoutManager(this, ROWS_IN_TABLE, LinearLayoutManager.HORIZONTAL, false));
            myRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        }
        
        myRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        myPictureInFolderAdapter = new PictureInFolderAdapter(filesItemList);
        myRecyclerView.setAdapter(myPictureInFolderAdapter);
        
        ItemClickSupport.addTo(myRecyclerView).setOnItemClickListener((recyclerView, position, v) -> onMyRecyclerViewItemClicked(position, v));
        ItemClickSupport.addTo(myRecyclerView).setOnItemLongClickListener((recyclerView, position, v) -> onMyRecyclerViewItemLongClicked(position, v));
        
        myPreferences = this.getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        Bundle bundle = new Bundle();
        bundle.putString(FILE_NAME_TO_LOAD, myPreferences.getString(PREF_FOR_LAST_FILE_NAME, NO_EXISTING_FILE_NAME));
        mainPictureLoaded = true;
        getSupportLoaderManager().initLoader(MAIN_PICTURE_LOADER_ID, bundle, this);
    }
    
    private void onMyRecyclerViewItemClicked(int position, View v) {
        
        if (!mainPictureLoaded) {
            File clickedFile = filesItemList.get(position).getPictureFile();
            if (myCurrentPictureFile == null || !myCurrentPictureFile.equals(clickedFile)) {
                myCurrentPictureFile = clickedFile;
                loadMainBitmap(myCurrentPictureFile.getPath());
            }
        }
    }
    
    private boolean onMyRecyclerViewItemLongClicked(int position, View v) {
        
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.item_context_menu);
        popup.setOnMenuItemClickListener(this);
        contextMenuPosition = position;
        popup.show();
        
        return false;
    }
    
    private void initFilesItemList() {
        
        filesItemList = new ArrayList<>();
        File[] FilesInPictureFolder = myPictureDirectory.listFiles();
        for (int i = 0; i < FilesInPictureFolder.length; i++) {
            filesItemList.add(new ListItemModel(FilesInPictureFolder[i]));
            loadPreview(i);
        }
    }
    
    private int getFilePositionInList(File fileToSearch, ArrayList<ListItemModel> filesItemList) {
        
        for (int i = 0, y = filesItemList.size(); i < y; i++) {
            if (filesItemList.get(i).getPictureFile().equals(fileToSearch)) {
                return i;
            }
        }
        return -1;
    }
    
    private void recyclePreviewBitmaps(ArrayList<ListItemModel> filesItemList) {
        
        for (int i = 0, y = filesItemList.size(); i < y; i++) {
            Bitmap currentBitmap = filesItemList.get(i).getPicturePreview();
            if (currentBitmap != null) {
                currentBitmap.recycle();
            }
        }
    }
    
    private void loadMainBitmap(String fileName) {
        
        mainPictureLoaded = true;
        mainProgressBar.setVisibility(View.VISIBLE);
        Bundle bundle = new Bundle();
        bundle.putString(FILE_NAME_TO_LOAD, fileName);
        bundle.putInt(REQUESTED_PICTURE_HEIGHT, myMainImageViewHeight);
        bundle.putInt(REQUESTED_PICTURE_WIDTH, myMainImageViewWidth);
        getSupportLoaderManager().restartLoader(MAIN_PICTURE_LOADER_ID, bundle, this);
    }
    
    private void loadPreview(int index) {
        
        if (myPreviewInLoad == null) {
            myPreviewInLoad = new intQueue();
        }
        myPreviewInLoad.add(index);
        if (myPreviewInLoad.size() == 1) {
            loadPreview();
        }
    }
    
    private void loadPreview() {
        
        if (!myPreviewInLoad.isEmpty()) {
            loadPreview(filesItemList.get(myPreviewInLoad.peek()).getPictureFile().getPath());
        }
    }
    
    private void loadPreview(String fileName) {
        
        Bundle bundle = new Bundle();
        bundle.putString(FILE_NAME_TO_LOAD, fileName);
        bundle.putInt(REQUESTED_PICTURE_HEIGHT, PREVIEW_PICTURE_HEIGHT);
        bundle.putInt(REQUESTED_PICTURE_WIDTH, PREVIEW_PICTURE_WIDTH);
        getSupportLoaderManager().restartLoader(PREVIEW_PICTURE_LOADER_START_ID, bundle, this);
    }
    
    @Override
    public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
        
        if (DEBUG) {
            Log.d(TAG, "onCreateLoader: ");
        }
        return new BitmapLoader(this, args);
    }
    
    @Override
    public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
        
        if (loader.getId() == MAIN_PICTURE_LOADER_ID) {
            if (myMainBitmap != null) {
                myMainBitmap.recycle();
            }
            myMainBitmap = data;
            myImageView.setImageBitmap(myMainBitmap);
            mainProgressBar.setVisibility(View.INVISIBLE);
            mainPictureLoaded = false;
        } else {
            setPreview(data, myPreviewInLoad.poll());
            loadPreview();
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Bitmap> loader) {
        
    }
    
    private void setPreview(Bitmap data, int position) {
        
        filesItemList.get(position).setPicturePreview(data);
        myPictureInFolderAdapter.notifyItemChanged(position);
    }
    
    @Override
    public void onClick(View v) {
        
        myButton.setEnabled(false);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        myCurrentPictureFile = generateFileForPicture();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(myCurrentPictureFile));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, GET_PICTURE_REQUEST_CODE);
        }
    }
    
    private File getDirectory(boolean needPrivate) {
        
        if (needPrivate) {
            return getExternalFilesDir(PICTURE_FOLDER_NAME);
        } else {
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PICTURE_FOLDER_NAME);
        }
    }
    
    private File generateFileForPicture() {
        
        String fileName = DEFAULT_FILE_NAME_PREFIX + new SimpleDateFormat(TIME_STAMP_PATTERN, Locale.getDefault()).format(new Date()) + FILE_NAME_SUFFIX;
        return generateFileForPicture(fileName);
    }
    
    private File generateFileForPicture(String fileName) {
        
        return new File(myPictureDirectory.getPath() + "/" + fileName);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        
        if (requestCode == GET_PICTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadMainBitmap(myCurrentPictureFile.getPath());
            filesItemList.add(new ListItemModel(myCurrentPictureFile));
            myPictureInFolderAdapter.notifyItemInserted(filesItemList.size() - 1);
            loadPreview(filesItemList.size() - 1);
            myRecyclerView.scrollToPosition(filesItemList.size() - 1);
        } else {
            if (!filesItemList.isEmpty()) {
                myCurrentPictureFile = filesItemList.get(filesItemList.size() - 1).getPictureFile();
            } else {
                myCurrentPictureFile = null;
            }
        }
        myButton.setEnabled(true);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    
        if (DEBUG) {
            Log.d(TAG, "onSaveInstanceState: ");
        }
        canComeBack = true;
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onPause() {
    
        if (DEBUG) {
            Log.d(TAG, "onPause: ");
        }
        if (myCurrentPictureFile != null) {
            myPreferences.edit()
                    .putString(PREF_FOR_LAST_FILE_NAME, myCurrentPictureFile.getPath())
                    .apply();
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
    
        if (DEBUG) {
            Log.d(TAG, "onDestroy: ");
            if (myMainBitmap != null && !canComeBack) {
                myMainBitmap.recycle();
            }
            recyclePreviewBitmaps(filesItemList);
        }
        super.onDestroy();
    }
    
    @Override
    protected void onStop() {
    
        if (DEBUG) {
            Log.d(TAG, "onStop: ");
        }
        super.onStop();
    }
    
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        
        File clickedItemFile = filesItemList.get(contextMenuPosition).getPictureFile();
        switch (item.getItemId()) {
            case R.id.menu_delete:
                if (clickedItemFile.delete()) {
                    loadPreview(contextMenuPosition);
                }
                if (myCurrentPictureFile == null || !myCurrentPictureFile.exists()) {
                    loadMainBitmap(NO_EXISTING_FILE_NAME);
                }
                break;
            case R.id.menu_rename:
                if (clickedItemFile.exists()) {
                    FileNameInputDialog fileRenameDialog = FileNameInputDialog.newInstance(clickedItemFile);
                    fileRenameDialog.show(getSupportFragmentManager(), FILE_RENAME_DIALOG_TAG);
                }
                break;
        }
        return false;
    }
    
    @Override
    public void onOkButtonClick(AppCompatDialogFragment dialog, String newFileName, File renamedFile, boolean successfully) {
        
        if (successfully) {
            int position = getFilePositionInList(renamedFile, filesItemList);
            File newFile = generateFileForPicture(newFileName);
            if (position < 0 || newFile.exists() || !renamedFile.renameTo(newFile)) {
                RenameErrorDialog.newInstance(getString(R.string.rename_failed)).show(getSupportFragmentManager(), ERROR_DIALOG_TAG);
            } else {
                filesItemList.get(position).setPictureFile(newFile);
                myPictureInFolderAdapter.notifyItemChanged(position);
            }
        } else {
            RenameErrorDialog.newInstance(getString(R.string.invalid_file_name)).show(getSupportFragmentManager(), ERROR_DIALOG_TAG);
        }
    }
    
    @Override
    public void onDialogNegativeClick(AppCompatDialogFragment dialog) {
        
    }
}
