package com.example.aqua;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonPresensi = findViewById(R.id.buttonPresensi);
        Button buttonDaftarHadir = findViewById(R.id.buttonDaftarHadir);
        Button buttonDaftarMurid = findViewById(R.id.buttonDaftarMurid);
        Button buttonDataMurid = findViewById(R.id.buttonDataMurid);

        // Tambahkan logika untuk tombol di sini, seperti mengatur onClickListener
        // Misalnya:
        buttonPresensi.setOnClickListener(v -> {
            // Aksi yang ingin dijalankan ketika tombol "Presensi" diklik
            showPresensiDialog();
        });

        // Tambahkan logika untuk tombol lainnya sesuai kebutuhan Anda
    }
    private void showPresensiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Presensi");
        builder.setMessage("Pilih aksi:");
        builder.setPositiveButton("Cek Kehadiran", (dialog, which) -> {
            // Aksi yang ingin dijalankan ketika tombol "Cek Kehadiran" diklik
        });
        builder.setNegativeButton("Rekam Data Wajah", (dialog, which) -> {
            // Aksi yang ingin dijalankan ketika tombol "Rekam Data Wajah" diklik
            Intent intent = new Intent(MainActivity.this, RekamDataWajah.class);
            startActivity(intent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}