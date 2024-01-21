package com.lam.progetto_lam.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.lam.progetto_lam.R;
import com.lam.progetto_lam.Main.MainViewModel;

/**
 * Gestisce le scelte dell'utente per X, N, modalità e valori di notifica.
 */
public class OpzioniDialogFragment extends DialogFragment {
    public static final String TAG = "OpzioniDialogFragment";


    private Button btnApplica;
    private Button btnChiudi;
    private boolean attivo;
    private MainViewModel mainViewModel;
    private SharedPreferences preferenze;
    private int x;
    private int n;
    private Spinner xSpin;
    private Spinner nSpin;
    private Spinner granSpin;
    private Switch switchMode;
    private String gran;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_options_dialog, container, false);
        switchMode = view.findViewById(R.id.switchMode);
        btnChiudi = view.findViewById(R.id.btnChiudi);
        btnApplica = view.findViewById(R.id.btnApplica);
        xSpin = view.findViewById(R.id.spnX);
        nSpin = view.findViewById(R.id.spnN);
        granSpin = view.findViewById(R.id.spinGran);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        preferenze = requireActivity().getPreferences(Context.MODE_PRIVATE);

        x = preferenze.getInt(getString(R.string.xVal), 5);
        n = preferenze.getInt(getString(R.string.nVal), 1);
        attivo = preferenze.getBoolean(getString(R.string.activeMode), true);
        gran = preferenze.getString(getString(R.string.notificationMode), "KILOMETER");

        ArrayAdapter<CharSequence> xAdapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.x_values, android.R.layout.simple_spinner_dropdown_item);
        xSpin.setAdapter(xAdapter);

        ArrayAdapter<CharSequence> nAdapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.n_values, android.R.layout.simple_spinner_dropdown_item);
        nSpin.setAdapter(nAdapter);

        ArrayAdapter<CharSequence> granAdapter = ArrayAdapter.createFromResource(view.getContext(),
                R.array.gran_values, android.R.layout.simple_spinner_dropdown_item);
        granSpin.setAdapter(granAdapter);


        xSpin.setSelection(xAdapter.getPosition(x+""));
        xSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                String selected = (String) adapterView.getItemAtPosition(pos);
                x = Integer.parseInt(selected);
                Log.d(TAG, "X_onSelect: " + selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                String selected = (String) adapterView.getItemAtPosition(0);
                x = Integer.parseInt(selected);
                Log.d(TAG, "X_onSelect: " + selected);
            }
        });

        nSpin.setSelection(nAdapter.getPosition(n+""));
        nSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                String selected = (String)  adapterView.getItemAtPosition(0);
                n = Integer.parseInt(selected);
                Log.d(TAG, "N_onSelect: " + selected);
            }
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                String selected = (String) adapterView.getItemAtPosition(pos);
                n = Integer.parseInt(selected);
                Log.d(TAG, "N_onSelect: " + selected);
            }


        });

        granSpin.setSelection(xAdapter.getPosition(gran+""));
        granSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                String selected = (String) adapterView.getItemAtPosition(0);
                gran = selected;
                Log.d(TAG, "X_onSelect: " + selected);
            }
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                String selected = (String) adapterView.getItemAtPosition(pos);
                gran = selected;
                Toast.makeText(getContext(), "Selected: " + gran, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Gran_onSelect: " + selected);
            }


        });
        if(attivo){
            switchMode.setText("Attiva");
            switchMode.setChecked(true);
        }else{
            switchMode.setText("Passiva");
            switchMode.setChecked(false);
        }
        switchMode.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                Log.d(TAG, "modalitàPassiva: Active");
                attivo = true;
                switchMode.setText("Attiva");
            }else{
                Log.d(TAG, "ModalitàPassiva: Passive");
                attivo = false;
                switchMode.setText("Passiva");
            }
        });


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnApplica.setOnClickListener(view1 -> {
            // set nel view model
            String params = x + "-" + n + "-" + attivo + "-" + gran;
            mainViewModel.setParamUtente(params);
            Toast.makeText(getContext(), "Paramettri Cambiati", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        btnChiudi.setOnClickListener(view1 -> {
            // set nel view model
            dismiss();
        });

    }
}