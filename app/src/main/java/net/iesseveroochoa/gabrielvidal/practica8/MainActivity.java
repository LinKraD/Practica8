package net.iesseveroochoa.gabrielvidal.practica8;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_READ_LOCATION = 1;
    private static final int REQUEST_CONFIG_UBICACION = 201;
    private static final String LOGTAG = "";

    private FusedLocationProviderClient mFusedLocationClient;
    LocationRequest mLocationRequest;

    private LocationCallback mLocationCallback;

    private int veces=0;

    private PolylineOptions lineas = new PolylineOptions();

    LatLng latlong;

    private TextView tvLatitud;
    private TextView tvLongitud;

    private GoogleMap mapa;


    private TextView tvVerLatitud;
    private TextView tvVerLongitud;

    private ToggleButton tbActualizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLatitud=findViewById(R.id.tv_Latitud);
        tvLongitud=findViewById(R.id.tv_Longitud);
        tvVerLatitud=findViewById(R.id.tv_VerLatitud);
        tvVerLongitud=findViewById(R.id.tv_VerLongitud);
        tbActualizar=findViewById(R.id.tbt_Actualizar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync( this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_LOCATION);
            }
        } else {
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        solicitarUltimaLocalizacion();

        tbActualizar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                if (enable) {
                    activaLocationUpdates();
                } else {
                    desactivaLocationUpdates();
                }
            }
        });
    }

    private void actualizaLocalizacionUI(Location loc) {
        if (loc != null) {
            tvVerLatitud.setText("Latitud: " + String.valueOf(loc.getLatitude()));
            tvVerLongitud.setText("Longitud: " + String.valueOf(loc.getLongitude()));
        } else {
            tvVerLatitud.setText("Latitud: (desconocida)");
            tvVerLongitud.setText("Longitud: (desconocida)");
        }
    }

    private void solicitarUltimaLocalizacion (){
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                actualizaLocalizacionUI(location);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }
        }
    }

    private void crearEventoRecepcionLocalizaciones(){
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    actualizaLocalizacionUI(location);
                    if (latlong!=null){
                        lineas.add(latlong);
                    }
                    latlong=new LatLng(location.getLatitude(),location.getLongitude());
                    lineas.add(latlong);
                    if (veces!=1){
                        ponMarcador("Inicio",latlong);
                        veces++;
                    }
                    if(lineas.getPoints().size()>=2){
                        lineas.width(8);
                        lineas.color(Color.GREEN);
                        mapa.addPolyline(lineas);
                        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong,20));
                    }
                }
            };
        };
    }

    private void activaLocationUpdates() {
        if(mLocationCallback==null){
            crearEventoRecepcionLocalizaciones();
        }
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,null /* Looper */);
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,REQUEST_CONFIG_UBICACION);
                    } catch (IntentSender.SendIntentException sendEx) {

                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONFIG_UBICACION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        activaLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOGTAG, "El usuario no ha realizado los cambios de configuración necesarios");
                        tbActualizar.setChecked(false);
                        break;
                }
                break;
        }
    }

    private void desactivaLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        ponMarcador("Final",latlong);
    }

    private void ponMarcador(final String titulo, final LatLng latlong){

        mapa.addMarker(new MarkerOptions()
                .position(new LatLng(latlong.latitude,latlong.longitude))
                .title(titulo));

        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latlong,20));
    }

    @Override
    public void onMapReady(GoogleMap mMap) {
        mapa = mMap;

        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(
                        MainActivity.this,
                        "Marcador pulsado:\n" +
                                marker.getTitle(),
                        Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }
}
