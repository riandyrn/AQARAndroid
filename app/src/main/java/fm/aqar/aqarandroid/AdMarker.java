package fm.aqar.aqarandroid;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by RiandyRN on 7/25/2015.
 */
public class AdMarker {

    private LatLng latlng;
    private int id;
    private String url;
    private Marker marker; //add marker here so we have full control of marker

    public AdMarker(double lat, double lng, int id, String url)
    {
        latlng = new LatLng(lat, lng);
        this.id = id;
        this.url = url;
    }

    public LatLng getLatlng() {
        return latlng;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
