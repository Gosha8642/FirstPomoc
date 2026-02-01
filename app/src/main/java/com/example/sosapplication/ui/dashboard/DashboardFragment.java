package com.example.sosapplication.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sosapplication.R;
import com.example.sosapplication.databinding.FragmentDashboardBinding;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private MapView mapView;

    private MyLocationNewOverlay myLocationOverlay;
    private GeoPoint userLocationPoint;
    private Polyline currentRoute;

    private final List<Marker> aedMarkers = new ArrayList<>();

    private static final int LOCATION_REQUEST = 101;

    private final double north = 49.6138;
    private final double south = 47.7312;
    private final double east = 22.5657;
    private final double west = 16.8332;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnMyLocation.bringToFront();
        binding.btnMyLocation.setZ(100f);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        BoundingBox slovakiaBox = new BoundingBox(north, east, south, west);
        mapView.setScrollableAreaLimitDouble(slovakiaBox);
        mapView.setMinZoomLevel(7.0);
        mapView.setMaxZoomLevel(20.0);
        mapView.zoomToBoundingBox(slovakiaBox, true);

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateAedByZoom();
                return true;
            }
        });

        initMyLocation();
        loadAedMarkers();
        highlightSlovakia();

        binding.btnMyLocation.setOnClickListener(v -> {
            if (userLocationPoint != null) {
                mapView.getController().animateTo(userLocationPoint);
                mapView.getController().setZoom(17.0);
            }
        });

        binding.aedInfoPanel.setVisibility(View.GONE);

        return root;
    }

    // ---------------- LOCATION ----------------

    private void initMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }

        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()),
                mapView
        );

        myLocationOverlay.enableMyLocation();

        myLocationOverlay.runOnFirstFix(() -> {
            Location loc = myLocationOverlay.getLastFix();
            if (loc != null) {
                userLocationPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
            }
        });

        mapView.getOverlays().add(myLocationOverlay);
    }

    // ---------------- AED ----------------

    private void loadAedMarkers() {
        try {

            String lang = getCurrentLang();
            String fileName = "SK_" + lang + ".geojson";

            InputStream is = requireContext().getAssets().open(fileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            JSONObject jsonObject = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            JSONArray features = jsonObject.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);
                JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");

                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);

                JSONObject props = feature.getJSONObject("properties");

                String location = props.optString("defibrillator:location", "AED");
                String access = props.optString("access", "-");
                String hours = props.optString("opening_hours", "-");

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lon));
                marker.setIcon(requireContext().getDrawable(R.drawable.ic_aed));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                marker.setTitle(location);
                marker.setSnippet("Access: " + access + "\nHours: " + hours);

                marker.setOnMarkerClickListener((m, map) -> {
                    if (userLocationPoint != null) {

                        GeoPoint aed = m.getPosition();
                        buildRoute(userLocationPoint, aed);
                        zoomToUserAndAed(userLocationPoint, aed);

                        binding.aedInfoPanel.setVisibility(View.VISIBLE);
                        binding.txtAedTitle.setText(m.getTitle());
                        binding.txtAedInfo.setText(m.getSnippet());
                    }
                    return true;
                });

                mapView.getOverlays().add(marker);
                aedMarkers.add(marker);
            }

            updateAedByZoom();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAedByZoom() {
        double zoom = mapView.getZoomLevelDouble();
        for (Marker m : aedMarkers) {
            m.setEnabled(zoom >= 12);
        }
    }

    // ---------------- ROUTE ----------------

    private void buildRoute(GeoPoint start, GeoPoint end) {

        if (currentRoute != null) {
            mapView.getOverlays().remove(currentRoute);
        }

        new Thread(() -> {
            try {
                String url =
                        "https://router.project-osrm.org/route/v1/foot/"
                                + start.getLongitude() + "," + start.getLatitude()
                                + ";" + end.getLongitude() + "," + end.getLatitude()
                                + "?overview=full&geometries=geojson";

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

                Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String json = sc.hasNext() ? sc.next() : "";

                JSONArray coords = new JSONObject(json)
                        .getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> points = new ArrayList<>();

                for (int i = 0; i < coords.length(); i++) {
                    JSONArray c = coords.getJSONArray(i);
                    points.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                }

                requireActivity().runOnUiThread(() -> {
                    currentRoute = new Polyline();
                    currentRoute.setPoints(points);
                    currentRoute.setColor(0xFF0066FF);
                    currentRoute.setWidth(9f);

                    mapView.getOverlays().add(currentRoute);
                    mapView.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ---------------- CAMERA ----------------

    private void zoomToUserAndAed(GeoPoint user, GeoPoint aed) {

        double n = Math.max(user.getLatitude(), aed.getLatitude());
        double s = Math.min(user.getLatitude(), aed.getLatitude());
        double e = Math.max(user.getLongitude(), aed.getLongitude());
        double w = Math.min(user.getLongitude(), aed.getLongitude());

        double latPadding = (n - s) * 0.4;
        double lonPadding = (e - w) * 0.4;

        BoundingBox box = new BoundingBox(
                n + latPadding,
                e + lonPadding,
                s - latPadding,
                w - lonPadding
        );

        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 300));
    }

    // ---------------- MASK ----------------

    private void highlightSlovakia() {
        Polygon mask = new Polygon();
        mask.setFillColor(0x99000000);
        mask.setStrokeWidth(0f);

        List<GeoPoint> outer = new ArrayList<>();
        outer.add(new GeoPoint(90, -180));
        outer.add(new GeoPoint(90, 180));
        outer.add(new GeoPoint(-90, 180));
        outer.add(new GeoPoint(-90, -180));
        outer.add(new GeoPoint(90, -180));
        mask.setPoints(outer);

        List<GeoPoint> hole = new ArrayList<>();
        hole.add(new GeoPoint(north, west));
        hole.add(new GeoPoint(north, east));
        hole.add(new GeoPoint(south, east));
        hole.add(new GeoPoint(south, west));
        hole.add(new GeoPoint(north, west));

        List<List<GeoPoint>> holes = new ArrayList<>();
        holes.add(hole);
        mask.setHoles(holes);

        mapView.getOverlays().add(mask);
    }

    private String getCurrentLang() {
        return requireContext()
                .getResources()
                .getConfiguration()
                .getLocales()
                .get(0)
                .getLanguage();
    }

    // ---------------- LIFECYCLE ----------------

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
