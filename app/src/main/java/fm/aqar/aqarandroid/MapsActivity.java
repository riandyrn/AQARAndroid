package fm.aqar.aqarandroid;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private RequestQueue queue;
    private ArrayList<AdMarker> adMarkers;

    private ArrayList<LatLng> currentSavedBigBoundaries;
    private final String URL_ADS_API = "https://api-dev-sa.aqar.fm/v2/query/ads";

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //setup map type
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(mMap.MAP_TYPE_HYBRID);

        //instantiate request queue
        queue = Volley.newRequestQueue(this);

        //show progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Fetching initial data...");
        progressDialog.show();

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                // riyadh coordinate
                double mainLat = 24.7107;
                double mainLng = 46.7222;
                float zoom = 15;

                // setup camera
                setUpCamera(mainLat, mainLng, zoom);

                // setup initial markers
                adMarkers = new ArrayList<>();
                displayAdMarkersOnMap();
                /*fillAreaBoundaries(currentSavedBigBoundaries, Color.BLUE);

                ArrayList<LatLng> boundaries = getViewBoundaries(getCurrentScreenBounds());
                fillAreaBoundaries(boundaries, Color.YELLOW);

                //Log.d("RAYPOLYGONRESULT", String.valueOf(RayCastingPolygon.isPointInPolygon(boundaries.get(0), currentSavedBigBoundaries)));
                Log.d("POLYGONRESULT", String.valueOf(isAreaInsideBoundaries(boundaries, currentSavedBigBoundaries)));*/
            }
        });
    }

    private boolean isAreaInsideBoundaries(ArrayList<LatLng> area, ArrayList<LatLng> boundaries)
    {

        for(LatLng latlng: area)
        {
            if(!isPointInsideArea(latlng, boundaries))
                return false;
        }

        return true;
    }

    private boolean isPointInsideArea(LatLng point, ArrayList<LatLng> area)
    {
        LatLng northEastBoundaries = area.get(0);
        LatLng southWestBoundaries = area.get(2);

        return (northEastBoundaries.longitude > point.longitude) && (southWestBoundaries.longitude < point.longitude)
                && (northEastBoundaries.latitude > point.latitude) && (southWestBoundaries.latitude < point.latitude);
    }

    private void fillAreaBoundaries(ArrayList<LatLng> boundaries, int color)
    {
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.addAll(boundaries);
        polygonOptions.fillColor(color);
        mMap.addPolygon(polygonOptions);
    }

    private void setUpCamera(double mainLat, double mainLng, float zoom)
    {
        // move camera to (mainLat, mainLng) with zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mainLat, mainLng), zoom));

        // set onChangeCamera listener
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                loadMoreAdMarkers();
            }
        });

    }

    private void displayAdMarkersOnMap()
    {
        ArrayList<LatLng> boundaries = getBigBoundaries(getCurrentScreenBounds());
        putAdMarkersOnMap(boundaries);
        moveBigBoundaries(boundaries);
    }

    private LatLngBounds getCurrentScreenBounds()
    {
        return mMap.getProjection().getVisibleRegion().latLngBounds;
    }

    private ArrayList<LatLng> getBigBoundaries(LatLngBounds curScreen)
    {
        /*
         *  calculate big boundaries based
         *  on curScreen
         */

        LatLng northEast = curScreen.northeast;
        LatLng southWest = curScreen.southwest;

        double deltaLat = Math.abs(northEast.latitude - southWest.latitude);
        double deltaLng = Math.abs(northEast.longitude - southWest.longitude);

        LatLng northEastBigBoundary = new LatLng(northEast.latitude + deltaLat, northEast.longitude + deltaLng);
        LatLng southWestBigBoundary = new LatLng(southWest.latitude - deltaLat, southWest.longitude - deltaLng);
        LatLng northWestBigBoundary = new LatLng(northEastBigBoundary.latitude, southWestBigBoundary.longitude);
        LatLng southEastBigBoundary = new LatLng(southWestBigBoundary.latitude, northEastBigBoundary.longitude);

        return new ArrayList<>(Arrays.asList
        (
            northEastBigBoundary,
            northWestBigBoundary,
            southWestBigBoundary,
            southEastBigBoundary
        ));
    }

    private void loadMoreAdMarkers()
    {
        /*
         *  load ad markers based on curScreen
         *  and last big boundary
         */

        boolean needToLoadMoreMarkers = isNeedToLoadMoreAdMarkers();
        //Log.d("YUHU", "need load more marker? " + String.valueOf(isNeedToLoadMoreAdMarkers()));

        if(needToLoadMoreMarkers)
        {
            displayAdMarkersOnMap();
            cleanUpMarkersNotInBigBoundaries();
        }
    }

    private void cleanUpMarkersNotInBigBoundaries()
    {
        /*
         *  cleanup unused marker so it won't
         *  waste the phone memory
         */

        ArrayList<AdMarker> adMarkersNotInBoundary = new ArrayList<>();

        for(int i = 0; i < adMarkers.size(); i++)
        {
            AdMarker adMarker = adMarkers.get(i);
            if(!isPointInsideArea(adMarker.getLatlng(), currentSavedBigBoundaries))
            {
                adMarker.getMarker().remove();
                adMarkersNotInBoundary.add(adMarker);
            }
        }

        adMarkers.removeAll(adMarkersNotInBoundary);
    }

    private ArrayList<LatLng> getViewBoundaries(LatLngBounds curScreen)
    {
        /*
         *  calculate view boundaries based
         *  on curScreen
         */

        LatLng northEast = curScreen.northeast;
        LatLng southWest = curScreen.southwest;
        LatLng northWest = new LatLng(northEast.latitude, southWest.longitude);
        LatLng southEast = new LatLng(southWest.latitude, northEast.longitude);

        return new ArrayList<>(Arrays.asList
        (
            northEast,
            northWest,
            southWest,
            southEast
        ));
    }

    private boolean isNeedToLoadMoreAdMarkers()
    {
        /*
         *  need to load more ad markers if current view
         *  is already outside of currentSavedPreviousBigBoundaries
         */

        ArrayList<LatLng> currentViewBoundaries =
                getViewBoundaries(getCurrentScreenBounds());

        return !isAreaInsideBoundaries(currentViewBoundaries, currentSavedBigBoundaries);
    }

    private void moveBigBoundaries(ArrayList<LatLng> boundaries)
    {
        this.currentSavedBigBoundaries = boundaries;
    }

    private void putAdMarkersOnMap(ArrayList<LatLng> boundaries)
    {
        /*
         *  make request to AQAR API to get available ads
         *  in boundaries and put them to the map
         */

        ArrayList<String> selectAttributes = new ArrayList<>(Arrays.asList("id", "lat", "lng", "url"));
        JSONObject query = getJSONPolygonQuery(boundaries, selectAttributes);
        fillAdMarkersAndPutOnMap(query);
    }

    private void putMarkerOnMap()
    {
        // put marker to map based on contents
        // of adMarkers

        int adMarkersSize = adMarkers.size();
        for(int i = 0; i < adMarkersSize; i++)
        {
            // get parameters
            LatLng latlng = adMarkers.get(i).getLatlng();
            String url = adMarkers.get(i).getUrl();

            // set marker and put it to map
            MarkerOptions markerOptions = new MarkerOptions().position(latlng).title("\u200e" + url);
            Marker marker = mMap.addMarker(markerOptions);

            // add marker to object
            adMarkers.get(i).setMarker(marker);
        }
    }

    private void dismissProgressDialog()
    {
        if(progressDialog != null)
        {
            progressDialog.dismiss();
            progressDialog = null;
            Toast.makeText(this, "Load data done", Toast.LENGTH_SHORT).show();
        }
    }

    private void fillAdMarkersAndPutOnMap(JSONObject query)
    {
        /*
         *  Send request based on query then
         *  fill the adMarkers
         */
        Log.d("JSONDebug", "query: " + query.toString());
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, URL_ADS_API, query, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d("JSONDebug", "response: " + response.toString());
                            String status = response.getString("status");
                            if(status.equals("success"))
                            {
                                JSONArray results = response.getJSONObject("data").getJSONArray("results");
                                for(int i = 0; i < results.length(); i++)
                                {
                                    JSONObject objAdMarker = results.getJSONObject(i);

                                    double lat = objAdMarker.getDouble("lat");
                                    double lng = objAdMarker.getDouble("lng");
                                    int id = objAdMarker.getInt("id");
                                    String url = objAdMarker.getString("url");

                                    AdMarker adMarker = new AdMarker(lat, lng, id, url);
                                    if(!adMarkers.contains(adMarker)) adMarkers.add(adMarker);
                                }

                                putMarkerOnMap();

                                // dismiss progress dialog after markers
                                // put on the map
                                dismissProgressDialog();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("JSONDebug", error.toString());
                        dismissProgressDialog();
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                    }
                });

        // add request to queue
        queue.add(jsObjRequest);
    }

    private JSONObject getJSONPolygonQuery(ArrayList<LatLng> boundaries, ArrayList<String> selectAttributes)
    {
        // construct polygon array
        JSONArray polygon = new JSONArray();
        for(LatLng latlng: boundaries)
        {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", latlng.latitude);
                point.put("lng", latlng.longitude);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            polygon.put(point);
        }

        // construct geo object
        JSONObject geo = new JSONObject();
        try {
            geo.put("type", "polygon");
            geo.put("polygon", polygon);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // construct selection array
        JSONArray select = new JSONArray();
        for(String attribute: selectAttributes)
            select.put(attribute);

        // construct content query
        JSONObject contentQuery = new JSONObject();
        try {
            contentQuery.put("geo", geo);
            contentQuery.put("select", select);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //construct main query
        JSONObject query = new JSONObject();
        try {
            query.put("query", contentQuery);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return query;
    }
}
