package com.lam.progetto_lam.Servizi;

import android.Manifest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.lam.progetto_lam.DataBaseR.Entità.MisuraEntita;
import com.lam.progetto_lam.DataBaseR.MioDatabase;
import com.lam.progetto_lam.R;
import com.lam.progetto_lam.Main.MainActivity;
import com.lam.progetto_lam.Utility.AreaCelle;
import com.lam.progetto_lam.Utility.CelleColoreUtility;

import java.text.ParseException;
import java.util.List;

import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.tile.MGRSTileProvider;

/**
 *
 * Servizio che avvisa se nella cella corrente della griglia non sono presenti misure registrate da ieri.
 * Si avvia quando l'attività principale chiama onStop e si interrompe quando mainAcitivty riprende,
 * in questo modo viene eseguito in background quando l'utente non vede la mappa.
 *
 */
public class ServizioBackground extends Service {
    private static final String TAG = "ServizioBackground";

    // DataBaseR
    private MioDatabase db = MioDatabase.getInstance(this);
    private List<MisuraEntita> allMisuraEntita;
    private static final String CHANNEL_ID = "NotificationServiceChannel";
    private static final int ID_NOTIFICA = 1;
    private static final long INTERVALLO_NOTIFICA = 5000; // 5 seconds

    private static GridType sceltaGridUtente;

    // per thread
    private Handler handler;
    private Runnable notificaRunnable;

    // per location
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location posizioneUtente;
    private String areaUtente;
    private CelleColoreUtility celleColoreUtility;
    private AreaCelle cellaAttualeUtente;
    private AreaCelle cellaPrecedenteUtente;
    private MGRSTileProvider tileProvider;

    private boolean isStarted=false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        tileProvider = new MGRSTileProvider(this, GridType.KILOMETER);
        celleColoreUtility = new CelleColoreUtility();
        sceltaGridUtente = GridType.KILOMETER;


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        generaNotificaChannel();
        String granularity = intent.getStringExtra("granularity");
        if(granularity.equals("KILOMETER")){
            sceltaGridUtente = GridType.KILOMETER;
        }else{
            sceltaGridUtente = GridType.HUNDRED_METER;
        }

        generaRichistaPosizione();

        // Schedule periodic notifications
        notificaRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: metodo run ok");
                if(allMisuraEntita !=null && !cellaAttualeUtente.equals(cellaPrecedenteUtente)){
                    Log.d(TAG, "run: controllo griglia");
                    cellaPrecedenteUtente = cellaAttualeUtente;
                    try {
                        if(!checkCelleUtenteInDB(sceltaGridUtente)){
                            inviaNotifica();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                handler.postDelayed(this, INTERVALLO_NOTIFICA);
            }
        };
        handler.post(notificaRunnable);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(notificaRunnable);
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "onDestroy: metodo onDestroy ");
        super.onDestroy();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void generaNotificaChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notification Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager mg = getSystemService(NotificationManager.class);
            if (mg != null) {
                mg.createNotificationChannel(channel);
            }
        }
    }

    private void inviaNotifica() {
        Intent mainInt = new Intent(this, MainActivity.class);
        mainInt.setAction(Intent.ACTION_MAIN);
        mainInt.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingInt = PendingIntent.getActivity(this, 0, mainInt, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder build = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Aggiorna la tua mappa!")
                .setContentText("Nessun valore recente nella tua zona...")
                .setContentIntent(pendingInt)
                .setAutoCancel(true);

        NotificationManager mg = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (mg != null) {
            mg.notify(ID_NOTIFICA, build.build());
        }
    }

    private void generaRichistaPosizione() {
        // 1. creo la request
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(20000)
                .build();

        // 2. setto la callback:
        setPosizioneCallback();

        // 3. se tutto va bene, partono updates
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task location = mFusedLocationProviderClient.getLastLocation();
        location.addOnCompleteListener(task -> {
            Log.d(TAG, "createLocationRequest: build richeista posizione");
            Location startingLocation = (Location) task.getResult();
            avvioAggiornamentoPosizione();
        });

        location.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog
                Log.e(TAG, "createLocationRequest: errore nella configurazione della posizione:", e);
            }
        });
    }

    private void setPosizioneCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                   posizioneUtente = location;
                    try {
                        calcolaAreaAttuale();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "onLocationRisulta: " + location.getLatitude() + " " + location.getLongitude());
                }
            }
        };
    }

    private void avvioAggiornamentoPosizione() {

        Log.d(TAG, "startLocationUpdates: inizio aggiornamento posizione.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public void calcolaAreaAttuale() throws ParseException {

        if(tileProvider!=null && posizioneUtente !=null){
            LatLng userLatLng= new LatLng(posizioneUtente.getLatitude(), posizioneUtente.getLongitude());
            String mgrs = tileProvider.getCoordinate(userLatLng, GridType.METER);
            areaUtente = mgrs.substring(0,5);
            cellaAttualeUtente = buildCelleArea(mgrs, sceltaGridUtente);
            if(!isStarted){
                new Thread(() -> {
                    allMisuraEntita = db.getAllMeasurements(areaUtente);
                }).start();

                isStarted=true;
            }
        }
    }

    public AreaCelle buildCelleArea(String mgrsCoord, GridType gridType) throws ParseException {
        return celleColoreUtility.getCelleArea(mgrsCoord, gridType);
    }
    private boolean checkCelleUtenteInDB(GridType gridType) throws ParseException {
        // popolo la tabella in base alla poszione
        Log.d(TAG, "CellaUtente: " + cellaAttualeUtente.getNordEst());
        for(MisuraEntita misuraEntita : allMisuraEntita){
            AreaCelle temp = buildCelleArea(misuraEntita.getMgrsCoordinate(), gridType);

            Log.d(TAG, "tempCell: " + temp.getNordEst());
            if(temp.equals(cellaAttualeUtente)){
                Log.d(TAG, "trovato!");
                return true;
            }
        }
        return false;
    }
}
