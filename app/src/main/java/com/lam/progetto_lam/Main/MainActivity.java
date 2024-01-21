package com.lam.progetto_lam.Main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.Task;
import com.lam.progetto_lam.DataBaseR.Entità.MisuraEntita;
import com.lam.progetto_lam.Enums.Modalita;
import com.lam.progetto_lam.R;
import com.lam.progetto_lam.Fragments.OpzioniDialogFragment;
import com.lam.progetto_lam.Utility.AreaCelle;
import com.lam.progetto_lam.Utility.CelleColoreUtility;
import com.lam.progetto_lam.Utility.Misurazioni.AudioRecorder;
import com.lam.progetto_lam.Utility.Misurazioni.LTESignal;
import com.lam.progetto_lam.Utility.Misurazioni.Misurazione;
import com.lam.progetto_lam.Utility.Misurazioni.LivelloRumore;
import com.lam.progetto_lam.Utility.Misurazioni.WiFiSignal;
import com.lam.progetto_lam.Enums.OpzioniZoom;

import com.lam.progetto_lam.Servizi.ServizioBackground;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.grid.style.Grids;
import mil.nga.mgrs.tile.MGRSTileProvider;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivityLog";

    // Permessi:
    private static final int RECORDER_PERMISSION_REQUEST_CODE = 200;
    private static final int FORGROUND_LOCATION_REQUEST_CODE = 400;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 500;
    private static final int FILE_MEDIA_PERMISSION_REQUEST_CODE = 600;

    // valori
    private static final int WIFI_CODE = 0;
    private static final int CELL_CODE = 1;

    // per misurazioni
    private Button btnDecibel;
    private Button btnWiFi;
    private Button btnCellular;
    private Button btnRegister;
    private TextView txtTitle;

    private MainViewModel mainViewModel;
    private List<MisuraEntita> misuraEntitaList;
    private Map<AreaCelle, List<MisuraEntita>> areaMap;
    Observer<List<MisuraEntita>> measureObserver;
    private Timer timer;
    private CelleColoreUtility cellUtil;
    private Handler noiseHandler;
    private AudioRecorder audioRecorder;
    private String fileAudioPath;

    // per map:
    private SupportMapFragment supportMapFragment;
    private GoogleMap nMap;
    private HashMap<OpzioniZoom, Integer> zoomLevels;
    private MGRSTileProvider mgrsTileProvider;
    private ImageButton btnZoomIn;
    private ImageButton btnZoomOut;

    // Impostazioni
    private ImageButton btnSettings;
    private int x=5;
    private int n=1;
    private boolean activeMode=true;
    private String notificationMode="KILOMETER";
    private SharedPreferences preferences;

    // Localizzazione:
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static boolean requestingLocationUpdates=false;
    private LocationCallback locationCallback;
    private Location userLocation;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDecibel = findViewById(R.id.btnDecibel);
        btnCellular = findViewById(R.id.btnCellular);
        btnWiFi = findViewById(R.id.btbWifi);
        btnRegister = findViewById(R.id.btnChangeMap);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        txtTitle = findViewById(R.id.txtHello);
        btnSettings = findViewById(R.id.btnSettings);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        zoomLevels = new HashMap<>();
        zoomLevels.put(OpzioniZoom.BASSO, 12);
        zoomLevels.put(OpzioniZoom.MEDIO, 15);
        zoomLevels.put(OpzioniZoom.ALTO, 18);
        cellUtil = new CelleColoreUtility();

        noiseHandler = new Handler();
        retrivePreferences();

        setupListeners();

        // per map:
        if (checkLocationPermission()) {
            initMap();
        }

        // connesione al DB:
        mainViewModel.conessioneDb(this);


        // observables:
         measureObserver = new Observer<List<MisuraEntita>>() {
            @Override
            public void onChanged(List<MisuraEntita> measureEntities) {
                misuraEntitaList = measureEntities;
                if(nMap!=null){
                    try {
                        paintGridCellFromMeasurements(nMap.getCameraPosition().zoom);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mainViewModel.getMisurazioni().observe(this, measureObserver);

        areaMap = new HashMap<>();

        Observer<String> paramObserver = new Observer<String>() {
            @Override
            public void onChanged(String stringLiveData) {
                String[] splitted = stringLiveData.split("-");
                x = Integer.parseInt(splitted[0]);
                n = Integer.parseInt(splitted[1]);
                activeMode = Boolean.parseBoolean(splitted[2]);
                notificationMode = splitted[3];

                Log.d(TAG, "onParamsChanged: " +x + " " + n+" " + activeMode);

                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(getString(R.string.xVal), x);
                editor.putInt(getString(R.string.nVal), n);
                editor.putBoolean(getString(R.string.activeMode), activeMode);
                editor.putString(getString(R.string.notificationMode), notificationMode);
                editor.apply();

                if(!activeMode){ // build worker
                    startPassiveMeasurement();
                    Log.d(TAG, "onParamsChanged: PASSIVO");
                }else{
                    stopPassiveMeasurement();
                    Log.d(TAG, "onParamsChanged: ATTIVO");
                }
                if(misuraEntitaList != null){
                    try {
                        paintGridCellFromMeasurements(nMap.getCameraPosition().zoom);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mainViewModel.getParamUtente().observe(this, paramObserver);


    }


    /**
     * Imposta i listerner dei click su tutti i pulsanti
     */
    private void setupListeners() {
        btnDecibel.setOnClickListener(view -> {
            if(checkRecorderPermission()) {
                try {
                    initRecorder();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!mainViewModel.getModeAttuale().equals(Modalita.DECIBEL)) {
                    mainViewModel.setModeAttuale(Modalita.DECIBEL);
                    txtTitle.setText("Modalità Suono: ");

                    resetObserver();
                } else {
                    Toast.makeText(this, "Sei in modalità Suono ", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "Permessi Mancanti modalità Suono ", Toast.LENGTH_SHORT).show();

            }
        });

        btnWiFi.setOnClickListener(v->{
            if(!mainViewModel.getModeAttuale().equals(Modalita.WIFI)){
                mainViewModel.setModeAttuale(Modalita.WIFI);
                txtTitle.setText("Modalità Wifi: ");

                resetObserver();
            }else{
                Toast.makeText(this, "Sei in modalità Wifi. " , Toast.LENGTH_SHORT).show();
            }
        });

        btnCellular.setOnClickListener(v->{
            if(!mainViewModel.getModeAttuale().equals(Modalita.CELLULARE)){
                mainViewModel.setModeAttuale(Modalita.CELLULARE);
                txtTitle.setText("Modalità Rete: ");

                resetObserver();
            }else{
                Toast.makeText(this, "Sei in modalità rete. " , Toast.LENGTH_SHORT).show();
            }
        });


        btnRegister.setOnClickListener(v->{
            if(activeMode){
                salvaMisure();
            }else{
                Toast.makeText(this, "Sei in modalità PASSIVA, cambia in Impostazioni", Toast.LENGTH_SHORT).show();
            }

        });

        btnZoomIn.setOnClickListener(view -> {
            int currentZoomOrdinal = mainViewModel.getZoomUtente().ordinal();
            if(currentZoomOrdinal==2){ // se zoom al massimo
                return;
            }
            int next = currentZoomOrdinal+ 1;
            mainViewModel.setZoomUtente(OpzioniZoom.values()[next]);
            moveCamera(mainViewModel.getPosizioneUtente(), zoomLevels.get(mainViewModel.getZoomUtente()));
            if(nMap!=null){
                try {
                    paintGridCellFromMeasurements(zoomLevels.get(mainViewModel.getZoomUtente()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

        });

        btnZoomOut.setOnClickListener(view -> {
            int currentZoomOrdinal = mainViewModel.getZoomUtente().ordinal();
            if(currentZoomOrdinal==0){ // se è minimo
                return;
            }
            int prev = currentZoomOrdinal - 1;
            mainViewModel.setZoomUtente(OpzioniZoom.values()[prev]);
            moveCamera(mainViewModel.getPosizioneUtente(),zoomLevels.get(mainViewModel.getZoomUtente()));
            if(nMap!=null){
                try {
                    paintGridCellFromMeasurements(zoomLevels.get(mainViewModel.getZoomUtente()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        btnSettings.setOnClickListener(view -> {
            new OpzioniDialogFragment().show(getSupportFragmentManager(), OpzioniDialogFragment.TAG);
        });
    }

    /**
     * Ottieni i valori preferences e imposta la modalità di eseczione e misurazione corretta
     */
    private void retrivePreferences(){
        preferences = this.getPreferences(Context.MODE_PRIVATE);
        int ordinal = preferences.getInt(getString(R.string.lastMeasureMode),0);
        activeMode = preferences.getBoolean(getString(R.string.activeMode), true);

        if(!activeMode){
            startPassiveMeasurement();
        }

        mainViewModel.setModeAttuale(Modalita.values()[ordinal]);
        switch (mainViewModel.getModeAttuale()){
            case WIFI:
                txtTitle.setText("Modalità Wifi: ");
                break;
            case CELLULARE:
                txtTitle.setText("Modalità Rete: ");
                break;
            case DECIBEL:
                txtTitle.setText("Modalità Suono: ");
                if(checkRecorderPermission() && checkFilesMediaPermission()){
                    try {
                        initRecorder();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }


    private void saveOnPreferences(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.lastMeasureMode), mainViewModel.getModeAttuale().ordinal());
        editor.apply();
    }


    /**
     * Salva la misurazione della modalità attuale sul DB.
     * a) crea tipo di misurazione appropriato.
     * b) se decibal, passa l'esecuzione a createNoiseMeasurement()
     * c) se no, controlla se la AreaCelle della posizione corrente è già in areaMap, e controlla il tempo (timer)
     * dell'ultima misurazione.
     * d) la misurazione viene salvata sul DB tramite mainViewModel.salvaMisurazioniAttuali(...)
     *
     */
    private void salvaMisure(){
        Misurazione m=null;
        switch (mainViewModel.getModeAttuale()){
            case WIFI:
                m = getConnectionStrength(WIFI_CODE);
                if(m==null)
                    return;
                break;
            case CELLULARE:
                m = getConnectionStrength(CELL_CODE);
                if(m==null)
                    return;
                break;
            case DECIBEL:
                createNoiseMeasurement();
                break;

        }

        if(mainViewModel.getModeAttuale().equals(Modalita.DECIBEL)){
            return;
        }

        userLocation=mainViewModel.getPosizioneUtente();
        if(mgrsTileProvider!=null && userLocation!=null){
            LatLng userLatLng= new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            String mgrs = mgrsTileProvider.getCoordinate(userLatLng, GridType.METER);
            try {
                AreaCelle userCell = buildCellArea(mgrs, zoomLevels.get(mainViewModel.getZoomUtente()));
                if(areaMap.containsKey(userCell)){
                    if(checkTimeout(n, areaMap.get(userCell))){
                        mainViewModel.salvaMisurazioniAttuali(m,mgrs);
                        Toast.makeText(this, "Misurazione salvata!", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(this, "aspetta " + n + " minuti per la prossima misurazione in quest'area", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    mainViewModel.salvaMisurazioniAttuali(m,mgrs);
                    Toast.makeText(this, "Misurazione salvata!", Toast.LENGTH_SHORT).show();
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * chiama i valori più recenti per ottenere quello più recente e calcola quanti minuti sono trascorsi dalla sua creazione
     * @param n N è scelto dall'utente
     * @param misuraEntitaList lista measureEntity
     * @return true if elapsed('tempo trascorso') >= n, false if not
     */
    private boolean checkTimeout(int n, List<MisuraEntita> misuraEntitaList) {
        List<MisuraEntita> ordered = getRecentValues(1, 0, misuraEntitaList);
        Timestamp lastTimestamp = ordered.get(0).getDataCreazione();
        Log.d(TAG, "ControlloTimeout: " + lastTimestamp);
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        // convert to instant to have a duration
        Duration elapsed = Duration.between(lastTimestamp.toInstant(), currentTime.toInstant() );
        Log.d(TAG, "ControlloTimeout: " + elapsed.toMinutes());
        if(elapsed.toMinutes() >= n){
            return true;
        }
        return false;
    }

    /**
     * rimozione observer, resettiamo LiveData, e ricollegamento di observer per recuperare
     * il nuovo tipo di dati
     */
    private void resetObserver() {
        mainViewModel.getMisurazioni().removeObservers(this);
        mainViewModel.resetCache();
        mainViewModel.getMisurazioni().observe(this, measureObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(nMap!=null){
            stopService(new Intent(this, ServizioBackground.class));
        }
    }

    // lifecycle:
    @Override
    public void onResume() {
        super.onResume();
        if(nMap!=null){
            Log.d(TAG, "onResume: on resume");
            if (requestingLocationUpdates && userLocation!=null) {
                startLocationUpdates();
            }
            retrivePreferences();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if(nMap != null){
            saveOnPreferences();
            stopPassiveMeasurement();
            Log.d(TAG, "onPause: on pause");
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates=true;
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        // create channel
        if(nMap != null){
            Intent serviceIntent = new Intent(this, ServizioBackground.class);
            serviceIntent.putExtra("granularity",notificationMode);
            startService(serviceIntent);
            Log.d(TAG, "onStop: on stop");
            stopPassiveMeasurement();
            saveOnPreferences();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: on destroy");
        stopPassiveMeasurement();
        saveOnPreferences();
        stopService(new Intent(this, ServizioBackground.class));


    }

   // metodi per map:
    private void initMap() {
        FragmentManager fm = getSupportFragmentManager();/// getChildFragmentManager();
        supportMapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map_container);
        Log.d(TAG, "initMap: avvio inizializzazione");
        if (supportMapFragment == null) {
            supportMapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction().replace(R.id.map_container, supportMapFragment).commit();
        }
        supportMapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Toast.makeText(this, "Caricamento mappa", Toast.LENGTH_SHORT).show();
        nMap = googleMap;

        // check for permission
        if(!checkLocationPermission()){
            return;
        }
        // set listeners and map properties
        nMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        nMap.setMyLocationEnabled(true);
        generaRichistaPosizione();

        updateMapLayer();
        if(misuraEntitaList != null){
            try {
                paintGridCellFromMeasurements(zoomLevels.get(mainViewModel.getZoomUtente()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ri-disegna le celle sulla griglia, al livello di zoom specificato
     * @param zoom livello di zoom per GridType
     * @throws ParseException se buildCellArea lo lancia
     */
    private void paintGridCellFromMeasurements(float zoom) throws ParseException {
        nMap.clear();
        updateMapLayer();
        areaMap.clear();

        for(MisuraEntita misuraEntita : misuraEntitaList){
            AreaCelle temp = buildCellArea(misuraEntita.getMgrsCoordinate(), zoom);
            if(areaMap.containsKey(temp)){
                areaMap.get(temp).add(misuraEntita);
            }else{
                List<MisuraEntita> created = new ArrayList<>();
                created.add(misuraEntita);
                areaMap.put(temp, created);
            }
        }

        for(AreaCelle p : areaMap.keySet()){
                nMap.addPolygon(calcolaMediaMisurazioni(p, areaMap.get(p), x));
        }
    }

    public PolygonOptions calcolaMediaMisurazioni(AreaCelle area, List<MisuraEntita> misuraEntitaList, int x){
        List<MisuraEntita> recentMeasureEntities = getRecentValues(x, n, misuraEntitaList);

        int number = recentMeasureEntities.size();
        double sum = 0;
        for(MisuraEntita m : recentMeasureEntities){
            sum = sum + m.getValore();
        }
        double avg = sum/number;

        switch (mainViewModel.getModeAttuale()){
            case WIFI:
                return cellUtil.coloreWifi(avg, area);
            case CELLULARE:
                return cellUtil.coloreLTE(avg, area);
            case DECIBEL:
                return cellUtil.coloreDecibel(avg, area);
            default:
                return null;
        }
    }


    /**
     * @param x seleziona il primo elemento X ordinato nel tempo
     * @param n  la differenza tra l'elemento precedente dovrebbe essere > di n. Se 0, ritorna la lista ordinata.
     * @param misuraEntitaList lista da modificare
     * @return lista dei primi X elementi con differenza oraria in N
     */
    private List<MisuraEntita> getRecentValues(int x, int n, List<MisuraEntita> misuraEntitaList) {
        misuraEntitaList.sort((m1, m2) -> m2.compareTo(m1));

        if(x < misuraEntitaList.size()){
            misuraEntitaList =  misuraEntitaList.subList(0, x);
        }

        if(n!=0){
            List<MisuraEntita> recentValues = new ArrayList<>();
            for(int i = 0; i< misuraEntitaList.size()-1; i++){
                MisuraEntita curr = misuraEntitaList.get(i);
                MisuraEntita prev = misuraEntitaList.get(i+1);

                Duration duration = Duration.between(prev.getDataCreazione().toInstant(), curr.getDataCreazione().toInstant());
                if(duration.toMinutes() >= n){
                    recentValues.add(curr);
                }

                if(i == misuraEntitaList.size()-2){ // ultimo elemento si mette sempre
                    recentValues.add(prev);
                }

            }
            return recentValues;
        }else{
            return misuraEntitaList;
        }
    }

    /**
     *
     * Calcola la macroarea, i primi 5 valori delle coordinate MGRS, della posizione dell'utente
     */
    public void calculateCurrentArea(){
        userLocation=mainViewModel.getPosizioneUtente();
        if(mgrsTileProvider!=null && userLocation!=null){
            LatLng userLatLng= new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            String mgrs = mgrsTileProvider.getCoordinate(userLatLng, GridType.KILOMETER);
            String area = mgrs.substring(0,5);
            mainViewModel.setArea(area);
        }
    }

    /**
     * Creazione mapOverlay.
     */
    public void updateMapLayer() {
        // create provider:
        nMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        Grids grids = Grids.create();
        grids.setLabelMinZoom(GridType.GZD, 5);
        grids.setColor(GridType.GZD, mil.nga.color.Color.gray());
        grids.setWidth(GridType.GZD, 0.3);
        grids.disable(GridType.METER);
        grids.disable(GridType.TEN_KILOMETER);
        grids.disable(GridType.HUNDRED_KILOMETER);

        grids.setMinZoom(GridType.TEN_METER, 17);
        grids.setMaxZoom(GridType.TEN_METER, 20);
        grids.setWidth(GridType.TEN_METER, 0.3);
        grids.setColor(GridType.TEN_METER, mil.nga.color.Color.color(Color.CYAN));

        grids.setMinZoom(GridType.HUNDRED_METER, 14);
        grids.setMaxZoom(GridType.HUNDRED_METER, 16);
        grids.setWidth(GridType.HUNDRED_METER, 0.3);
        grids.setColor(GridType.HUNDRED_METER, mil.nga.color.Color.color(Color.GREEN));

        grids.setMinZoom(GridType.KILOMETER, 9);
        grids.setMaxZoom(GridType.KILOMETER, 13);
        grids.setWidth(GridType.KILOMETER, 0.3);
        grids.enableLabeler(GridType.KILOMETER);
        grids.setColor(GridType.KILOMETER, mil.nga.color.Color.color(Color.MAGENTA));

        mgrsTileProvider = MGRSTileProvider.create(this, grids);
        Log.d(TAG, "createMGRSGrid: griglia creata");

        nMap.addTileOverlay(new TileOverlayOptions().tileProvider(mgrsTileProvider));


        Log.d(TAG, "onMapReady: creazione mappa terminata");

    }

    /**
     * Crea un AreaCelle di GridType da zoom, che contiene mgrsCoordd
     * @param mgrsCoord MGRS posizione
     * @param zoom livello di zoom scelto
     * @return una AreaCelle che contiene mgrsCoord
     * @throws ParseException se mgrsCoord è formattato in modo errato
     *
     */
    public AreaCelle buildCellArea(String mgrsCoord, float zoom) throws ParseException {

        if(zoom<=13){
            GridType gridType = GridType.KILOMETER;
            return cellUtil.getCelleArea(mgrsCoord, gridType);
        }else if(zoom >= 14 && zoom <= 16){
            GridType gridType = GridType.HUNDRED_METER;
            return cellUtil.getCelleArea(mgrsCoord, gridType);
        }else if(zoom > 16){
            GridType gridType = GridType.TEN_METER;
            return cellUtil.getCelleArea(mgrsCoord, gridType);
        }
        return cellUtil.getCelleArea(mgrsCoord, GridType.HUNDRED_METER);
    }

    private void setLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    mainViewModel.setPosizioneUtente(location);
                    calculateCurrentArea();

                }
            }
        };
    }


    private boolean checkLocationPermission(){
          Log.d(TAG, "getLocationPermission: controllo Permessi");
          String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, FORGROUND_LOCATION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkBackgroundLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    private boolean checkRecorderPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORDER_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkFilesMediaPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, FILE_MEDIA_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case FORGROUND_LOCATION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i =0; i<grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "checkPermission: fallito");
                            Toast.makeText(this, "Abilita permesso di geolocalizzazione del dispositivo!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Log.d(TAG, "checkPermission: concesso");
                    checkBackgroundLocationPermission();
                    //initializzo la mappa
                    initMap();
                }
            }
            break;
            case RECORDER_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i =0; i<grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "checkRecordPermission: fallito");

                        }
                    }
                    Log.d(TAG, "checkRecordPermission: concesso");

                    try {
                        initRecorder();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    checkFilesMediaPermission();
                }
            }
            break;
            case BACKGROUND_LOCATION_REQUEST_CODE:{
                Log.d(TAG, "onRequestPermissionsResult: posizione in background abilitata");
            }
            break;
            case FILE_MEDIA_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i =0; i<grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "checkRecordPermission: fallito");
                        }
                    }
                    Log.d(TAG, "checkRecordPermission: concesso");

                    //inizializzo recorder
                    try {
                        initRecorder();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // location:
    private void generaRichistaPosizione() {
        if(!checkLocationPermission()){
            return;
        }
        // 1. creo la request
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(20000)
                .build();

        // 2. setto la callback:
        setLocationCallback();

        // 3. faccio partire updates
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Task location = mFusedLocationProviderClient.getLastLocation();
        location.addOnCompleteListener(task -> {
            Log.d(TAG, "generaRichiestaPosizione: build richiesta di posizione");
            Location startingLocation = (Location) task.getResult();
            mainViewModel.setPosizioneUtente(startingLocation);
            moveCamera(startingLocation, zoomLevels.get(OpzioniZoom.BASSO));
            mainViewModel.setZoomUtente(OpzioniZoom.BASSO);
            startLocationUpdates();

        });

        location.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                Log.e(TAG, "generaRichiestaPosizione: errore nella configurazione della posizione:", e);
            }
        });

    }

    private void startLocationUpdates() {
        if (!checkLocationPermission()) {
            return;
        }
        Log.d(TAG, "startLocationUpdates: inizio updates.");
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    private void moveCamera(Location location, float zoom){
        if(location!=null){
            nMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()),
                    zoom));
        }
    }


    /**
     * Controlla se la modalità specificata ha l'accesso e crea la misura
     * @param type 0 se proviene da wifi, 1 se viene da cellulare
     * @return la misurazione wifi o cellulare
     */
    // Metodi per le misurazioni:
    private Misurazione getConnectionStrength(int type) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        if(activeNetwork==null || !activeNetwork.isConnected()){
            Toast.makeText(this, "Apertura conessione Internet!", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if(type==WIFI_CODE){
                // crei una misurazione di tipo Wifi
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d(TAG, "getMeasurement: versione interna.");
                    WiFiSignal level = new WiFiSignal(caps.getSignalStrength());
                    return level;
                }
            }else{
                Toast.makeText(this, "Disattiva il WIFI e attiva la Connesione Dati!", Toast.LENGTH_SHORT).show();
            }

        }else if(caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
            if(type==CELL_CODE){
                // misure LTE
                return getCellularStrength();
            }else{
                Toast.makeText(this, "Attiva il WIFI!", Toast.LENGTH_SHORT).show();

            }

        }else{
            return null;
        }
        return null;
    }

    /**
     * calcola la potenza della connesione LTE
     * @return LTESignal
     */
    public Misurazione getCellularStrength() {

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (!checkLocationPermission()) {
            return null;
        }
        List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
        if(cellInfos != null){
            for(CellInfo info : cellInfos){
                if (info instanceof CellInfoLte) {
                    CellInfoLte cellInfoLte = (CellInfoLte) info;
                    CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                    return new LTESignal(cellSignalStrengthLte.getDbm());
                }
            }
        }

        return null;

    }

    public void initRecorder() throws IOException {
        if(audioRecorder == null){
            fileAudioPath = getExternalCacheDir().getAbsolutePath() + "/audio.3gp";
            audioRecorder = new AudioRecorder(fileAudioPath);
        }

    }

    /**
     * salvo le misurazioni in modalità passiva
     */
    public void startPassiveMeasurement() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    Log.d(TAG, "run: salvataggio misurazioni in modalità passiva" );
                    salvaMisure();
                });

            }
        }, 0, (n * 60 * 1000)+5); // esegui ogni n minuti
    }

    public void stopPassiveMeasurement() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * crea un LivelloRumore, quindi attendere 2 secondi dopo l'avvio del registratore
     * per ottenere il valore di getAmplitude.
     * succesivamente è uguale a salvaMisure().
     */
    public void createNoiseMeasurement(){

        try {
            if (audioRecorder != null) {
                audioRecorder.start();
                noiseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        double amp = audioRecorder.getAmplitude();
                        Log.d(TAG, "run: amp " + amp);
                        double db = 20 * Math.log10(amp / 32767.0); // più è vicino allo zero, più è rumoroso

                        audioRecorder.stop();
                        Misurazione soundM = new LivelloRumore((int) db);

                        Log.d(TAG, "createNoiseMeasurement: " + soundM.getValore());
                        userLocation = mainViewModel.getPosizioneUtente();
                        if (mgrsTileProvider != null && userLocation != null) {
                            LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                            String mgrs = mgrsTileProvider.getCoordinate(userLatLng, GridType.METER);
                            try {
                                OpzioniZoom userZoom = mainViewModel.getZoomUtente();
                                AreaCelle userCell = buildCellArea(mgrs, zoomLevels.get(userZoom));
                                if (areaMap.containsKey(userCell)) {
                                    if (checkTimeout(n, areaMap.get(userCell))) {
                                        mainViewModel.salvaMisurazioniAttuali(soundM, mgrs);
                                        Toast.makeText(getApplicationContext(), "Misurazione salvata!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Aspetta " + n + " minuti per la prossima misurazione in quest'area", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    mainViewModel.salvaMisurazioniAttuali(soundM, mgrs);
                                    Toast.makeText(getApplicationContext(), "Misurazione salvata!", Toast.LENGTH_SHORT).show();

                                }

                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 2000);
            }else{
                Toast.makeText(this, "Registratore non pronto", Toast.LENGTH_SHORT).show();
            }
            } catch(IOException e){
                e.printStackTrace();
            }



    }
}
