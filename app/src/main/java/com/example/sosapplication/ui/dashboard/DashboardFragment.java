package com.example.sosapplication.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

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
    private GeoPoint selectedAedPoint;
    private Polyline currentRoute;

    private final List<Marker> aedMarkers = new ArrayList<>();

    private static final int LOCATION_REQUEST = 101;
    private static final int FAB_MARGIN_NORMAL = 180;
    private static final int FAB_MARGIN_WITH_PANEL = 320;

    private final double north = 49.6138;
    private final double south = 47.7312;
    private final double east = 22.5657;
    private final double west = 16.8332;
    
    private float initialY;
    private boolean isPanelVisible = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);

        BoundingBox slovakiaBox = new BoundingBox(north, east, south, west);
        mapView.setScrollableAreaLimitDouble(slovakiaBox);
        mapView.setMinZoomLevel(7.0);
        mapView.setMaxZoomLevel(20.0);
        mapView.zoomToBoundingBox(slovakiaBox, true);

        mapView.addMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) { return true; }
            @Override public boolean onZoom(ZoomEvent event) {
                updateAedByZoom();
                return true;
            }
        });

        binding.btnMyLocation.bringToFront();
        binding.btnMyLocation.setZ(100f);

        initMyLocation();
        loadAedMarkers();
        highlightSlovakia();

        // FAB animation
        binding.btnMyLocation.setScaleX(0f);
        binding.btnMyLocation.setScaleY(0f);
        binding.btnMyLocation.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        binding.btnMyLocation.setOnClickListener(v -> {
            animateButtonPress(v);
            if (userLocationPoint != null) {
                mapView.getController().animateTo(userLocationPoint);
                mapView.getController().setZoom(17.0);
            }
        });

        // Navigate button - build route only when clicked
        binding.btnNavigate.setOnClickListener(v -> {
            if (userLocationPoint != null && selectedAedPoint != null) {
                animateButtonPress(v);
                buildRoute(userLocationPoint, selectedAedPoint);
                zoomToUserAndAed(userLocationPoint, selectedAedPoint);
            }
        });

        binding.aedInfoPanel.setVisibility(View.GONE);
        
        // Setup swipe to dismiss on panel
        setupPanelSwipeToDismiss();

        return root;
    }
    
    private void setupPanelSwipeToDismiss() {
        binding.aedInfoPanel.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private float startTranslationY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        startTranslationY = v.getTranslationY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startY;
                        if (deltaY > 0) { // Only allow swiping down
                            v.setTranslationY(startTranslationY + deltaY);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float finalDeltaY = event.getRawY() - startY;
                        if (finalDeltaY > 100) { // Threshold to dismiss
                            hideAedPanel();
                        } else {
                            // Snap back
                            v.animate()
                                    .translationY(0f)
                                    .setDuration(200)
                                    .start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    // ---------------- LOCATION ----------------

    private void initMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST
            );
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
                userLocationPoint = new GeoPoint(
                        loc.getLatitude(),
                        loc.getLongitude()
                );
            }
        });

        mapView.getOverlays().add(myLocationOverlay);
    }

    // ---------------- AED ----------------

    private void loadAedMarkers() {
        try {
            String lang = getCurrentLang();
            if (!lang.equals("en") && !lang.equals("sk") && !lang.equals("uk")) {
                lang = "en";
            }

            String fileName = "SK_" + lang + ".geojson";

            InputStream is = requireContext().getAssets().open(fileName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            JSONObject json = new JSONObject(
                    new String(buffer, StandardCharsets.UTF_8)
            );

            JSONArray features = json.getJSONArray("features");
            aedMarkers.clear();

            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);
                JSONArray coords = feature
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                double lon = coords.getDouble(0);
                double lat = coords.getDouble(1);

                JSONObject props = feature.getJSONObject("properties");

                String location =
                        props.optString("defibrillator:location", "AED");
                String access =
                        props.optString("access", "-");
                String hours =
                        props.optString("opening_hours", "-");

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lon));
                marker.setIcon(requireContext().getDrawable(R.drawable.ic_aed));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle(location);

                marker.setOnMarkerClickListener((m, map) -> {
                    // Store selected AED position (don't build route yet)
                    selectedAedPoint = m.getPosition();
                    
                    // Show panel with animation
                    showAedPanel(location, access, hours);
                    
                    // Center on AED
                    mapView.getController().animateTo(selectedAedPoint);
                    
                    return true;
                });

                mapView.getOverlays().add(marker);
                aedMarkers.add(marker);
            }

            updateAedByZoom();
            mapView.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAedPanel(String location, String access, String hours) {
        isPanelVisible = true;
        binding.aedInfoPanel.setVisibility(View.VISIBLE);
        binding.txtAedTitle.setText(location);
        binding.txtAedInfo.setText(
                getString(R.string.aed_access) + ": " + access + "\n" +
                        getString(R.string.aed_hours) + ": " + hours
        );

        // Slide up animation
        binding.aedInfoPanel.setTranslationY(300f);
        binding.aedInfoPanel.setAlpha(0f);
        binding.aedInfoPanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        // Move FAB up when panel is shown
        moveFabUp();
    }
    
    private void hideAedPanel() {
        isPanelVisible = false;
        
        // Slide down animation
        binding.aedInfoPanel.animate()
                .translationY(300f)
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    binding.aedInfoPanel.setVisibility(View.GONE);
                    binding.aedInfoPanel.setTranslationY(0f);
                })
                .start();
        
        // Move FAB back down
        moveFabDown();
        
        // Clear selected AED
        selectedAedPoint = null;
        
        // Remove route if exists
        if (currentRoute != null) {
            mapView.getOverlays().remove(currentRoute);
            currentRoute = null;
            mapView.invalidate();
        }
    }
    
    private void moveFabUp() {
        // Animate FAB to move up
        binding.btnMyLocation.animate()
                .translationY(-140f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
    
    private void moveFabDown() {
        // Animate FAB back to original position
        binding.btnMyLocation.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
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

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(url).openConnection();

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
                    currentRoute.setColor(0xFF007AFF);  // Blue accent color
                    currentRoute.setWidth(12f);

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

        BoundingBox box = new BoundingBox(n, e, s, w);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 300));
    }

    // ---------------- MASK ----------------

    private void highlightSlovakia() {
        Polygon mask = new Polygon();
        mask.setFillColor(0x66000000);  // Slightly lighter overlay
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

        mask.setHoles(List.of(hole));
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

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
