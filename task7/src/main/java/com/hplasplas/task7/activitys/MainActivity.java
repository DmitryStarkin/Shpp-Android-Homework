package com.hplasplas.task7.activitys;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.hplasplas.task7.App;
import com.hplasplas.task7.R;
import com.hplasplas.task7.adapters.ForecastAdapter;
import com.hplasplas.task7.interfaces.OpenWeatherMapApi;
import com.hplasplas.task7.managers.MessageManager;
import com.hplasplas.task7.managers.PreferencesManager;
import com.hplasplas.task7.managers.WeatherImageManager;
import com.hplasplas.task7.models.weather.current.CurrentWeather;
import com.hplasplas.task7.models.weather.forecast.FifeDaysForecast;
import com.hplasplas.task7.models.weather.forecast.ThreeHourForecast;
import com.hplasplas.task7.utils.DataTimeUtils;
import com.hplasplas.task7.utils.InternetConnectionChecker;
import com.starsoft.dbtolls.main.DataBaseTolls;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.hplasplas.task7.setting.Constants.*;

public class MainActivity extends AppCompatActivity implements DataBaseTolls.onCursorReadyListener {
    
    private final String TAG = getClass().getSimpleName();
    
    @Inject
    public WeatherImageManager mImageManager;
    @Inject
    public DataTimeUtils mDataTimeUtils;
    @Inject
    public OpenWeatherMapApi mOpenWeatherMapApi;
    @Inject
    public Gson mGson;
    @Inject
    public DataBaseTolls mDataBaseTolls;
    @Inject
    public MessageManager mMessageManager;
    private Toolbar mToolbar;
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mCurrentWeatherIcon;
    private ImageView mBackground;
    private TextView mCityName;
    private TextView mDateTime;
    private TextView mTemperature;
    private TextView mWeatherDescription;
    private TextView mPressure;
    private TextView mHumidity;
    private TextView mWindDescription;
    private TextView mCloudiness;
    private TextView mSunrise;
    private TextView mSunset;
    private ProgressBar mCityFindBar;
    private RecyclerView mRecyclerView;
    private Call<String> mCurrentWeatherCall;
    private Call<String> mForecastWeatherCall;
    private boolean mClearText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        App.getAppComponent().inject(this);
        mDataBaseTolls.setOnCursorReadyListener(this);
        setContentView(R.layout.activity_main);
        findViews();
        adjustViews();
        adjustRecyclerView();
        getAndPrepareLastWeatherData();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setQueryHint(getResources().getString(R.string.search_hint));
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                
                createSuggestionAdapter();
                return true;
            }
            
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                
                if (DEBUG) {
                    Log.d(TAG, "onMenuItemActionCollapse: ");
                }
                mDataBaseTolls.clearAllTasks();
                mSearchView.setQueryHint(getResources().getString(R.string.search_hint));
                closeCursor(mSearchView);
                mCityFindBar.setVisibility(View.INVISIBLE);
                return true;
            }
        });
        
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                
                refreshCityList(CITY_QUERY_FULL_SEARCH_PREFIX + query + CITY_QUERY_FULL_SEARCH_SUFFIX, true);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                
                if (DEBUG) {
                    Log.d(TAG, "onQueryTextChange: ");
                }
                if (newText.length() > 1) {
                    
                    refreshCityList(CITY_QUERY_BEGIN_SEARCH_PREFIX + newText + CITY_QUERY_BEGIN_SEARCH_SUFFIX, false);
                } else {
                    closeCursor(mSearchView);
                }
                return true;
            }
        });
        
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            
            @Override
            public boolean onSuggestionSelect(int position) {
                
                return false;
            }
            
            @Override
            public boolean onSuggestionClick(int position) {
                
                refreshWeatherWithCursor(position);
                mSearchMenuItem.collapseActionView();
                return true;
            }
        });
        return true;
    }
    
    @Override
    protected void onResume() {
        
        super.onResume();
        refreshWeather();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        int id = item.getItemId();
        if (id == R.id.action_about) {
            mMessageManager.makeDialogMessage(this, getString(R.string.about_message), R.drawable.ic_info_outline_white_24dp);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
        
        super.onPause();
        mDataBaseTolls.clearAllTasks();
        cancelCall(mCurrentWeatherCall);
        cancelCall(mForecastWeatherCall);
        closeCursor(mSearchView);
    }
    
    private void closeCursor(SearchView searchView) {
        
        if (searchView != null) {
            CursorAdapter adapter = searchView.getSuggestionsAdapter();
            if (adapter != null) {
                Cursor cursor = adapter.swapCursor(null);
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
    }
    
    private void cancelCall(Call call) {
        
        if (call != null) {
            call.cancel();
        }
    }
    
    private void findViews() {
    
        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        mCityFindBar = (ProgressBar) findViewById(R.id.city_find_bar);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_container);
        mCurrentWeatherIcon = (ImageView) findViewById(R.id.weather_icon);
        mBackground = (ImageView) findViewById(R.id.background_image);
        mCityName = (TextView) findViewById(R.id.city_name);
        mDateTime = (TextView) findViewById(R.id.date_time);
        mTemperature = (TextView) findViewById(R.id.forecast_temperature);
        mPressure = (TextView) findViewById(R.id.pressure);
        mHumidity = (TextView) findViewById(R.id.humidity);
        mWindDescription = (TextView) findViewById(R.id.wind_description);
        mCloudiness = (TextView) findViewById(R.id.cloudiness);
        mSunrise = (TextView) findViewById(R.id.sunrise);
        mSunset = (TextView) findViewById(R.id.sunset);
        mWeatherDescription = (TextView) findViewById(R.id.weather_description);
        mRecyclerView = (RecyclerView) findViewById(R.id.forecast_list);
    }
    
    private void adjustViews() {
        
        setSupportActionBar(mToolbar);
        mCityFindBar.setVisibility(View.INVISIBLE);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshWeatherWitchMessage);
        mSwipeRefreshLayout.setProgressViewOffset(true, REFRESH_INDICATOR_START_OFFSET,
                REFRESH_INDICATOR_END_OFFSET);
    }
    
    private void adjustRecyclerView() {
        
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }
    
    private void createSuggestionAdapter() {
        
        if (mSearchView.getSuggestionsAdapter() == null) {
            int[] to = {R.id.city_item, R.id.country};
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(MainActivity.this, R.layout.city_search_item,
                    null, COLUMNS_CITY_NAME, to, 0);
            mSearchView.setSuggestionsAdapter(adapter);
        }
    }
    
    private void refreshCityList(String query, boolean clearText) {
        
        mClearText = clearText;
        mDataBaseTolls.getDataUsingSQLCommand(SUGGESTION_QUERY_TAG, query);
        mCityFindBar.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onCursorReady(Cursor cursor) {
        
        if (DEBUG) {
            Log.d(TAG, "onCursorReady: ");
        }
        if (mSearchMenuItem == null || !mSearchMenuItem.isActionViewExpanded()) {
            cursor.close();
        } else {
            Cursor oldCursor = mSearchView.getSuggestionsAdapter().swapCursor(cursor);
            if (oldCursor != null && !oldCursor.isClosed()) {
                oldCursor.close();
            }
            if (mClearText && (cursor == null || !cursor.moveToFirst())) {
                mSearchView.setQuery(null, false);
                mSearchView.setQueryHint(getResources().getString(R.string.no_result));
            }
            mCityFindBar.setVisibility(View.INVISIBLE);
        }
        
    }
    
    private void setWeatherValues(CurrentWeather currentWeather) {
    
        mImageManager.setBackground(mBackground, currentWeather.getWeather().get(0).getMain(), currentWeather.getWeather().get(0).getId());
        mImageManager.setWeatherIcon(mCurrentWeatherIcon, currentWeather.getWeather().get(0).getIcon());
        mCityName.setText(currentWeather.getCityName());
        mDateTime.setText(mDataTimeUtils.getTimeString(currentWeather.getCalculationDataTime(), WEATHER_TIME_STAMP_PATTERN, MIL_PER_SEC));
        mSunrise.setText(mDataTimeUtils.getTimeString(currentWeather.getSys().getSunrise(), SUN_TIME_STAMP_PATTERN, MIL_PER_SEC));
        mSunset.setText(mDataTimeUtils.getTimeString(currentWeather.getSys().getSunset(), SUN_TIME_STAMP_PATTERN, MIL_PER_SEC));
        mTemperature.setText(getResources().getString(R.string.temperature, currentWeather.getMain().getTemp()));
        mWeatherDescription.setText(currentWeather.getWeather().get(0).getDescription());
        mPressure.setText(getResources().getString(R.string.pressure, currentWeather.getMain().getPressure()));
        mHumidity.setText(getResources().getString(R.string.humidity, currentWeather.getMain().getHumidity()));
        mCloudiness.setText(getResources().getString(R.string.cloudiness, currentWeather.getClouds().getAll()));
        mWindDescription.setText(getResources().getString(R.string.wind, currentWeather.getWind().getSpeed(),
                determineWindDirection(currentWeather.getWind().getDeg())));
    }
    
    private String determineWindDirection(double deg) {
        
        int direction = getResources().getIdentifier(WIND_DIRECTION_PREFIX + Integer.toString((int) Math.round(deg / WIND_DIRECTION_DIVIDER)),
                "string", getApplicationContext().getPackageName());
        return getResources().getString(direction);
    }
    
    private void refreshWeather() {
        
        showRefreshProgress(mSwipeRefreshLayout);
        if (refreshIntervalIsRight() && isInternetAvailable()) {
            refreshWeatherData();
        } else {
            hideRefreshProgress(mSwipeRefreshLayout);
        }
    }
    
    private void refreshWeatherWithCursor(int CursorPosition) {
        
        Cursor cursor = mSearchView.getSuggestionsAdapter().getCursor();
        if (cursor != null) {
            cursor.moveToPosition(CursorPosition);
            refreshWeather(cursor.getInt(cursor.getColumnIndex(COLUMNS_CITY_ID)));
        }
    }
    
    private void refreshWeather(int cityId) {
        
        showRefreshProgress(mSwipeRefreshLayout);
        if (isInternetAvailable()) {
            refreshWeatherData(cityId);
        } else {
            mMessageManager.makeSnackbarMessage(mToolbar, getResources().getString(R.string.internet_not_available), SNACK_BAR_MESSAGE_DURATION);
            hideRefreshProgress(mSwipeRefreshLayout);
        }
    }
    
    private void refreshWeatherWitchMessage() {
        
        if (!refreshIntervalIsRight()) {
            hideRefreshProgress(mSwipeRefreshLayout);
            String interval = mDataTimeUtils.getTimeString(MIN_REQUEST_INTERVAL - (System.currentTimeMillis() - PreferencesManager.getPreferences(this).getLong(LAST_REQUEST_TIME, 0)),
                    REFRESHING_TIME_STAMP_PATTERN);
            mMessageManager.makeSnackbarMessage(mToolbar, getResources().getString(R.string.weather_refreshed, interval), SNACK_BAR_MESSAGE_DURATION);
        } else if (!isInternetAvailable()) {
            hideRefreshProgress(mSwipeRefreshLayout);
            mMessageManager.makeSnackbarMessage(mToolbar, getResources().getString(R.string.internet_not_available), SNACK_BAR_MESSAGE_DURATION);
        } else {
            refreshWeatherData();
        }
    }
    
    private boolean refreshIntervalIsRight() {
        
        long curTime = System.currentTimeMillis();
        return curTime > PreferencesManager.getPreferences(this).getLong(LAST_REQUEST_TIME, 0) + MIN_REQUEST_INTERVAL;
    }
    
    private boolean isInternetAvailable() {
        
        return InternetConnectionChecker.isInternetAvailable(this);
    }
    
    private void showRefreshProgress(SwipeRefreshLayout swipeRefreshLayout) {
        
        swipeRefreshLayout.setRefreshing(true);
    }
    
    private void hideRefreshProgress(SwipeRefreshLayout swipeRefreshLayout) {
        
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void refreshWeatherData() {
        
        refreshWeatherData(PreferencesManager.getPreferences(this).getInt(PREF_FOR_CURRENT_CITY_ID, DEFAULT_CITY_ID));
    }
    
    private void refreshWeatherData(int cityId) {
        
        mCurrentWeatherCall = mOpenWeatherMapApi.getCurrentWeather(cityId, UNITS_PARAMETER_VALUE, API_KEY);
        mForecastWeatherCall = mOpenWeatherMapApi.getFifeDaysWeather(cityId, UNITS_PARAMETER_VALUE, API_KEY);
        mCurrentWeatherCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                
                if (response.body() != null) {
                    String currentWeatherResponse =  response.body();
                    mForecastWeatherCall.enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.body() != null) {
                                weatherGetSuccessfully();
                               writeAndPrepareWeatherData(currentWeatherResponse, response.body());
                            } else {
                                weatherGetError();
                            }
                        }
    
                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            weatherGetError();
                        }
                    });
                } else {
                    weatherGetError();
                }
            }
            
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                weatherGetError();
            }
        });
        
    }
    
    private void weatherGetError() {
        
        hideRefreshProgress(mSwipeRefreshLayout);
        mMessageManager.makeSnackbarMessage(mToolbar, getResources().getString(R.string.weather_get_error), SNACK_BAR_MESSAGE_DURATION);
    }
    
    private void weatherGetSuccessfully() {
        
        hideRefreshProgress(mSwipeRefreshLayout);
        mMessageManager.makeSnackbarMessage(mToolbar, getResources().getString(R.string.weather_updated), SNACK_BAR_MESSAGE_DURATION);
    }
    
    private void writeAndPrepareWeatherData(String jsonCurrentWeather, String jsonForecast) {
        
        PreferencesManager.getPreferences(this).edit()
                .putString(PREF_FOR_CURRENT_WEATHER_JSON_DATA, jsonCurrentWeather)
                .putString(PREF_FOR_FIFE_DAYS_FORECAST_JSON_DATA, jsonForecast)
                .putLong(LAST_REQUEST_TIME, System.currentTimeMillis())
                .apply();
        prepareCurrentWeatherData(jsonCurrentWeather);
        prepareForecastWeatherData(jsonForecast);
    }
    
    private void getAndPrepareLastWeatherData() {
        
        String jsonCurrentWeather = PreferencesManager.getPreferences(this).getString(PREF_FOR_CURRENT_WEATHER_JSON_DATA, null);
        String jsonForecast = PreferencesManager.getPreferences(this).getString(PREF_FOR_FIFE_DAYS_FORECAST_JSON_DATA, null);
        if (jsonCurrentWeather != null && jsonForecast != null) {
            prepareCurrentWeatherData(jsonCurrentWeather);
            prepareForecastWeatherData(jsonForecast);
        }
    }
    
    private void prepareCurrentWeatherData(String jsonCurrentWeather) {
        
        CurrentWeather currentWeather = mGson.fromJson(jsonCurrentWeather, CurrentWeather.class);
        PreferencesManager.getPreferences(this).edit()
                .putInt(PREF_FOR_CURRENT_CITY_ID, currentWeather.getCityId())
                .apply();
        setWeatherValues(currentWeather);
    }
    
    private void prepareForecastWeatherData(String jsonForecastWeather) {
    
        FifeDaysForecast forecast = mGson.fromJson(jsonForecastWeather, FifeDaysForecast.class);
        setAdapter(mRecyclerView, forecast.getThreeHourForecast());
    }
    
    private ForecastAdapter setAdapter(RecyclerView recyclerView, List<ThreeHourForecast> itemList) {
    
        ForecastAdapter adapter = new ForecastAdapter(itemList);
        if (recyclerView.getAdapter() == null) {
            recyclerView.setAdapter(adapter);
        } else {
            recyclerView.swapAdapter(adapter, true);
        }
        return adapter;
    }
    
}
