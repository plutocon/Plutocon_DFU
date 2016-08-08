package com.kongtech.plutocon.template.dfu;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kongtech.plutocon.sdk.Plutocon;
import com.kongtech.plutocon.template.PlutoconListActivity;
import com.kongtech.plutocon.template.view.AttrItemView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class TemplateFragment extends Fragment implements View.OnClickListener {

    private AttrItemView aivTargetName;
    private AttrItemView aivTargetAddress;

    private Plutocon targetPlutocon;

    private ProgressDialog progressDialog;
    private String dialogMessage;
    private int dialogProgress;

    private final DfuProgressListener dfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            changeDialogMessage("기기 연결 중 입니다.", -1);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {

        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            changeDialogMessage("DFU를 시작합니다.", -1);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {

        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            changeDialogMessage("부트로더 부팅 중 입니다.", -1);
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            changeDialogMessage("업로드 중 입니다.", percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            changeDialogMessage("검증 중 입니다.", -1);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            changeDialogMessage("종료 중 입니다.", -1);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {

        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            DfuEnded(true, null);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            DfuEnded(true, null);
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            DfuEnded(false, message);
        }
    };

    private void changeDialogMessage(final String msg, final int progress){
        this.dialogMessage = msg;
        this.dialogProgress = progress;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(dialogMessage + (progress < 0 ? "" :  "(" + dialogProgress + "%)"));
            }
        });
    }

    private void startDFU() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("준비 중 입니다.");
        progressDialog.show();

        String filePath = getFirmwarePath();
        final DfuServiceInitiator starter = new DfuServiceInitiator(aivTargetAddress.getValue())
                .setDeviceName(aivTargetName.getValue())
                .setKeepBond(false).setDisableNotification(true);
        starter.setZip(null, filePath);
        starter.start(getContext(), DfuService.class);

    }

    private void DfuEnded(final boolean isSuccess, final String msg){
        progressDialog.dismiss();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isSuccess) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage(msg != null ? "실패하였습니다. 다시 한번 시도해주세요." : msg);
                    builder.setPositiveButton("확인", null).show();
                }
            }
        });
    }

    public static Fragment newInstance(Context context) {
        TemplateFragment f = new TemplateFragment();
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(getContext(), dfuProgressListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(getContext(), dfuProgressListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_template, null);

        aivTargetName = (AttrItemView)view.findViewById(R.id.aivTargetName);
        aivTargetAddress = (AttrItemView)view.findViewById(R.id.aivTargetAddress);

        aivTargetName.setOnClickListener(this);
        view.findViewById(R.id.btnDFU).setOnClickListener(this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.aivTargetName :
                if(checkPermission())
                    startActivityForResult(new Intent(getActivity(), PlutoconListActivity.class), 0);
                break;
            case R.id.btnDFU:
                startDFU();
                break;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 0:
                if(resultCode == 1) {
                    targetPlutocon = (Plutocon)data.getParcelableExtra("PLUTOCON");
                    aivTargetName.setValue(targetPlutocon.getName());
                    aivTargetAddress.setValue(targetPlutocon.getMacAddress());

                }
            break;
        }
    }

    private boolean checkPermission(){
        BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if((mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())){
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }

            LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ex) {}

            if(!gps_enabled){
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return false;
            }
        }
        return true;
    }

    public String getFirmwarePath() {
        try {
            InputStream inputStream = getContext().getAssets().open("plutocon1.1.zip");
            byte[] buffer = new byte[2048];
            int readSize;
            File outfile = new File(getContext().getFilesDir() + "/plutocon1.1.zip");
            FileOutputStream fos = new FileOutputStream(outfile);
            while((readSize = inputStream.read(buffer)) != -1){
                fos.write(buffer,0,readSize);
            }
            inputStream.close();
            fos.close();

            return outfile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
