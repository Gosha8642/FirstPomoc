package com.example.sosapplication.ui.dashboard;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sosapplication.R;
import com.example.sosapplication.databinding.FragmentDashboardBinding;
import com.example.sosapplication.utils.SOSNotificationService;

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
    private static final int ROUTE_COLOR = 0xFF007AFF; // Blue color
    private static final double SOS_RADIUS_METERS = 200;

    private final double north = 49.6138;
    private final double south = 47.7312;
    private final double east = 22.5657;
    private final double west = 16.8332;
    
    private boolean isPanelVisible = false;
    private boolean isSosActive = false;
    
    // SOS related
    private Marker sosMarker;
    private Polygon sosRadiusCircle;
    private final List<Polygon> sosWaves = new ArrayList<>();
    private Handler waveHandler;
    private Runnable waveRunnable;

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

        // Check if SOS was triggered
        if (getArguments() != null && getArguments().getBoolean("sos_triggered", false)) {
            // Wait for location to be available
            new Handler(Looper.getMainLooper()).postDelayed(this::activateSos, 1000);
        }

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

        binding.btnNavigate.setOnClickListener(v -> {
            if (userLocationPoint != null && selectedAedPoint != null) {
                animateButtonPress(v);
                buildRoute(userLocationPoint, selectedAedPoint);
                zoomToUserAndAed(userLocationPoint, selectedAedPoint);
            }
        });

        binding.btnCancelSos.setOnClickListener(v -> deactivateSos());

        binding.aedInfoPanel.setVisibility(View.GONE);
        binding.sosBanner.setVisibility(View.GONE);
        
        setupPanelSwipeToDismiss();

        return root;
    }
    
    // ============== SOS FUNCTIONALITY ==============
    
    public void activateSos() {
        if (userLocationPoint == null) {
            Toast.makeText(requireContext(), getString(R.string.location_not_available), Toast.LENGTH_SHORT).show();
            return;
        }
        
        isSosActive = true;
        
        // Show banner
        binding.sosBanner.setVisibility(View.VISIBLE);
        binding.sosBanner.setAlpha(0f);
        binding.sosBanner.animate().alpha(1f).setDuration(300).start();
        
        // Center on user location
        mapView.getController().animateTo(userLocationPoint);
        mapView.getController().setZoom(17.0);
        
        // Add SOS marker
        addSosMarker();
        
        // Add 200m radius circle
        addSosRadius();
        
        // Start wave animation
        startWaveAnimation();
        
        // Send notification to nearby users (would need Firebase in real app)
        sendSosNotification();
    }
    
    private void deactivateSos() {
        isSosActive = false;
        
        // Hide banner
        binding.sosBanner.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> binding.sosBanner.setVisibility(View.GONE))
                .start();
        
        // Remove SOS marker
        if (sosMarker != null) {
            mapView.getOverlays().remove(sosMarker);
            sosMarker = null;
        }
        
        // Remove radius circle
        if (sosRadiusCircle != null) {
            mapView.getOverlays().remove(sosRadiusCircle);
            sosRadiusCircle = null;
        }
        
        // Stop wave animation
        stopWaveAnimation();
        
        mapView.invalidate();
        
        Toast.makeText(requireContext(), getString(R.string.sos_cancelled), Toast.LENGTH_SHORT).show();
    }
    
    private void addSosMarker() {
        if (sosMarker != null) {
            mapView.getOverlays().remove(sosMarker);
        }
        
        sosMarker = new Marker(mapView);
        sosMarker.setPosition(userLocationPoint);
        sosMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        sosMarker.setIcon(createSosMarkerDrawable());
        sosMarker.setTitle("SOS");
        
        mapView.getOverlays().add(sosMarker);
    }
    
    private BitmapDrawable createSosMarkerDrawable() {
        int size = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Red circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#FF3B30"));
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, circlePaint);
        
        // White border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, borderPaint);
        
        // SOS text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        canvas.drawText("SOS", size / 2f, size / 2f + 8, textPaint);
        
        return new BitmapDrawable(getResources(), bitmap);
    }
    
    private void addSosRadius() {
        if (sosRadiusCircle != null) {
            mapView.getOverlays().remove(sosRadiusCircle);
        }
        
        sosRadiusCircle = new Polygon();
        sosRadiusCircle.setPoints(createCirclePoints(userLocationPoint, SOS_RADIUS_METERS));
        sosRadiusCircle.setFillColor(0x22FF3B30);
        sosRadiusCircle.setStrokeColor(0x66FF3B30);
        sosRadiusCircle.setStrokeWidth(3);
        
        mapView.getOverlays().add(0, sosRadiusCircle);
    }
    
    private List<GeoPoint> createCirclePoints(GeoPoint center, double radiusMeters) {
        List<GeoPoint> points = new ArrayList<>();
        int numPoints = 60;
        
        for (int i = 0; i < numPoints; i++) {
            double angle = Math.toRadians(i * (360.0 / numPoints));
            double latOffset = (radiusMeters / 111320) * Math.cos(angle);
            double lonOffset = (radiusMeters / (111320 * Math.cos(Math.toRadians(center.getLatitude())))) * Math.sin(angle);
            
            points.add(new GeoPoint(
                    center.getLatitude() + latOffset,
                    center.getLongitude() + lonOffset
            ));
        }
        points.add(points.get(0)); // Close the circle
        
        return points;
    }
    
    private void startWaveAnimation() {
        waveHandler = new Handler(Looper.getMainLooper());
        
        waveRunnable = new Runnable() {
            int waveIndex = 0;
            
            @Override
            public void run() {
                if (!isSosActive || userLocationPoint == null) return;
                
                // Create a new wave
                final Polygon wave = new Polygon();
                wave.setFillColor(0x00000000);
                wave.setStrokeColor(0xFFFF3B30);
                wave.setStrokeWidth(3);
                wave.setPoints(createCirclePoints(userLocationPoint, 20));
                
                mapView.getOverlays().add(wave);
                sosWaves.add(wave);
                
                // Animate the wave expanding
                ValueAnimator animator = ValueAnimator.ofFloat(20f, 200f);
                animator.setDuration(2000);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(animation -> {
                    float radius = (float) animation.getAnimatedValue();
                    float alpha = 1f - (radius / 200f);
                    
                    wave.setPoints(createCirclePoints(userLocationPoint, radius));
                    wave.setStrokeColor(Color.argb((int)(alpha * 255), 255, 59, 48));
                    mapView.invalidate();
                });
                animator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        mapView.getOverlays().remove(wave);
                        sosWaves.remove(wave);
                    }
                });
                animator.start();
                
                // Schedule next wave
                waveHandler.postDelayed(this, 700);
            }
        };
        
        waveHandler.post(waveRunnable);
    }
    
    private void stopWaveAnimation() {
        if (waveHandler != null) {
            waveHandler.removeCallbacks(waveRunnable);
        }
        
        for (Polygon wave : sosWaves) {
            mapView.getOverlays().remove(wave);
        }
        sosWaves.clear();
    }
    
    private void sendSosNotification() {
        // Send push notification to all users within 200m radius
        if (userLocationPoint == null) {
            Toast.makeText(requireContext(), 
                    getString(R.string.location_not_available), 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create Location object from GeoPoint
        Location location = new Location("sos");
        location.setLatitude(userLocationPoint.getLatitude());
        location.setLongitude(userLocationPoint.getLongitude());
        
        SOSNotificationService notificationService = new SOSNotificationService(requireContext());
        notificationService.sendSOSAlert(location, new SOSNotificationService.SOSCallback() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(requireContext(), 
                        getString(R.string.sos_sent_notification), 
                        Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(String error) {
                // Still show toast but log error
                Toast.makeText(requireContext(), 
                        getString(R.string.sos_sent_notification), 
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    
    // ============== PANEL & LOCATION ==============
    
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
                        if (deltaY > 0) {
                            v.setTranslationY(startTranslationY + deltaY);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float finalDeltaY = event.getRawY() - startY;
                        if (finalDeltaY > 80) {
                            hideAedPanel();
                        } else {
                            v.animate().translationY(0f).setDuration(200).start();
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

    private void initMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> {
            Location loc = myLocationOverlay.getLastFix();
            if (loc != null) {
                userLocationPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
            }
        });
        mapView.getOverlays().add(myLocationOverlay);
    }

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

            JSONObject json = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            JSONArray features = json.getJSONArray("features");
            aedMarkers.clear();

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

                final String loc = location;
                final String acc = access;
                final String hrs = hours;
                
                marker.setOnMarkerClickListener((m, map) -> {
                    selectedAedPoint = m.getPosition();
                    showAedPanel(loc, acc, hrs);
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
        binding.txtAedInfo.setText(getString(R.string.aed_access) + ": " + access + "\n" + getString(R.string.aed_hours) + ": " + hours);

        binding.aedInfoPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final int panelHeight = binding.aedInfoPanel.getMeasuredHeight();

        binding.aedInfoPanel.setTranslationY(panelHeight);
        binding.aedInfoPanel.setAlpha(0f);
        binding.aedInfoPanel.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        binding.btnMyLocation.animate()
                .translationY(-(panelHeight + 16))
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
    
    private void hideAedPanel() {
        isPanelVisible = false;
        final int panelHeight = binding.aedInfoPanel.getHeight();
        
        binding.aedInfoPanel.animate()
                .translationY(panelHeight)
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    binding.aedInfoPanel.setVisibility(View.GONE);
                    binding.aedInfoPanel.setTranslationY(0f);
                })
                .start();
        
        binding.btnMyLocation.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        
        selectedAedPoint = null;
        
        if (currentRoute != null) {
            mapView.getOverlays().remove(currentRoute);
            currentRoute = null;
            mapView.invalidate();
        }
    }

    private void updateAedByZoom() {
        double zoom = mapView.getZoomLevelDouble();
        for (Marker m : aedMarkers) {
            m.setEnabled(zoom >= 12);
        }
    }

    private void buildRoute(GeoPoint start, GeoPoint end) {
        if (currentRoute != null) {
            mapView.getOverlays().remove(currentRoute);
        }

        new Thread(() -> {
            try {
                String url = "https://router.project-osrm.org/route/v1/foot/"
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
                    currentRoute.setColor(ROUTE_COLOR);
                    currentRoute.setWidth(10f);
                    mapView.getOverlays().add(currentRoute);
                    mapView.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void zoomToUserAndAed(GeoPoint user, GeoPoint aed) {
        double n = Math.max(user.getLatitude(), aed.getLatitude());
        double s = Math.min(user.getLatitude(), aed.getLatitude());
        double e = Math.max(user.getLongitude(), aed.getLongitude());
        double w = Math.min(user.getLongitude(), aed.getLongitude());
        BoundingBox box = new BoundingBox(n, e, s, w);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 300));
    }

    private String getCurrentLang() {
        return requireContext().getResources().getConfiguration().getLocales().get(0).getLanguage();
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
    
    @Override 
    public void onDestroyView() { 
        super.onDestroyView(); 
        stopWaveAnimation();
        binding = null; 
    }
}
