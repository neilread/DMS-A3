package android.dms.aut.ac.nz.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Arrays;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fused = LocationServices.getFusedLocationProviderClient(this);

        Button button = (Button) findViewById(R.id.location_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fused.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                    if (location != null) {
                        TextView textView = (TextView) findViewById(R.id.textView);
                        textView.setText(getString(R.string.location_details, location.getLatitude(), location.getLongitude(), location.getAltitude()));
                    }
                    }


                });
            }
        });
    }

    public void showLocation(View view) {
        startActivity(new Intent(getApplicationContext(),MapsActivity.class));
    }
    public void sendMessage(View view) {
        startActivity(new Intent(getApplicationContext(),SendSmsActivity.class));
    }
    private FusedLocationProviderClient fused;

}
