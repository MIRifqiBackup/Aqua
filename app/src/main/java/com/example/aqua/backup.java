//package com.example.aqua;
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.content.ContentValues;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.res.AssetManager;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.os.Environment;
//import android.text.InputType;
//import android.util.Log;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfRect;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.objdetect.CascadeClassifier;
//
////Simpan foto
//import org.opencv.android.Utils;
//import org.opencv.core.Mat;
//import org.opencv.core.CvType;
//import org.opencv.objdetect.Objdetect;
//
//import android.content.Context;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.Toast;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class RekamDataWajah extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
//    private static final String TAG = "RekamDataWajah";
//    private int mCurrentCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
//    private boolean mIsSwitchingCamera = false;
//    private Mat mRgba;
//
//    private boolean mIsFrontCamera = true;
//
//    private boolean isButtonPressed = false;
//
//    private static final String DATABASE_NAME = "dbPresensi";
//    private static final int DATABASE_VERSION = 1;
//    private static final String TABLE_NAME = "siswa";
//    private static final String COLUMN_ID = "id_siswa";
//    private static final String COLUMN_NAMA = "nama_siswa";
//    private static final String COLUMN_NIS = "nis_siswa";
//    private static final String COLUMN_KELAS = "kelas_siswa";
//
//    private SQLiteDatabase database;
//
//    private long lastDetectionTime = 0;
//
//
//    // Deklarasikan array untuk menyimpan foto wajah sementara
//    private Mat[] temporaryFaceImages = new Mat[20];
//
//    private int counter = 0; // Variabel untuk menghitung jumlah gambar yang sudah diambil
//
//    private boolean isCameraStopped = false;
//
//    // Deklarasikan variabel global
//    private Button mulaiButton;
//    private EditText inputNama;
//    private EditText inputNIS;
//    private EditText inputKelas;
//
//    private Mat croppedFace;
//
//    private CascadeClassifier mFaceCascade;
//
//    private boolean isFaceDetected = false; // Variabel untuk menandai apakah wajah terdeteksi atau tidak
//
//
//    private CameraBridgeViewBase mOpenCVCamera;
//    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS: {
//                    Log.i(TAG, "OpenCV is loaded");
//                    mOpenCVCamera.enableView();
//                    break;
//                }
//                default: {
//                    super.onManagerConnected(status);
//                    break;
//                }
//            }
//        }
//    };
//
//    public RekamDataWajah() {
//    }
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        ActivityCompat.requestPermissions(RekamDataWajah.this, new String[]{Manifest.permission.CAMERA}, 1);
//        setContentView(R.layout.activity_rekam_data_wajah);
//
//        mOpenCVCamera = findViewById(R.id.frame_surface);
//        mOpenCVCamera.setVisibility(SurfaceView.VISIBLE);
//        mOpenCVCamera.setCvCameraViewListener(this);
//
//        // Use the front-facing camera
//        mOpenCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
//
//
//
//        Button mulaiButton = findViewById(R.id.button2);
//        EditText inputNama = findViewById(R.id.inputNama);
//        EditText inputNIS = findViewById(R.id.inputNIS);
//        EditText inputKelas = findViewById(R.id.inputKelas);
//
//        mulaiButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isButtonPressed = true;
//
//                // Membuat direktori folder MyPresensi di penyimpanan eksternal jika belum ada
//                File folder = new File(getExternalFilesDir(null), "MyPresensi");
//                if (!folder.exists()) {
//                    if (folder.mkdirs()) {
//                        Log.d(TAG, "Folder MyPresensi berhasil dibuat");
//                    } else {
//                        Log.e(TAG, "Gagal membuat folder MyPresensi");
//                        return;
//                    }
//                }
//
//                // Mendapatkan path lengkap dari folder MyPresensi
//                String folderPath = folder.getAbsolutePath();
//                Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);
//
//                // Membuka atau membuat database SQLite
//                String databasePath = folderPath + File.separator + DATABASE_NAME;
//                database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);
//
//
//
//                // Dapatkan nilai dari inputNama, inputNIS, dan inputKelas
//                String nama = inputNama.getText().toString();
//                int nis = Integer.parseInt(inputNIS.getText().toString());
//                String kelas = inputKelas.getText().toString();
//
//                // Menyimpan data ke dalam tabel
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(COLUMN_NAMA, nama);
//                contentValues.put(COLUMN_NIS, nis);
//                contentValues.put(COLUMN_KELAS, kelas);
//                database.insert(TABLE_NAME, null, contentValues);
//                // Menutup database setelah selesai
//                database.close();
//                // Menampilkan pesan ke logcat bahwa database berhasil dibuat
//                Log.d(TAG, "input siswa berhasil disimpan");
//
//            }
//        });
//
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case 1: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    mOpenCVCamera.setCameraPermissionGranted();
//                }
//                break;
//            }
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (OpenCVLoader.initDebug()) {
//            Log.i(TAG, "onResume");
//            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        } else {
//            Log.i(TAG, "onResume Failed");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
//        }
//        // Reset the switching camera flag
//        mIsSwitchingCamera = false;
//
//        // Periksa izin akses WRITE_EXTERNAL_STORAGE
//        if (!isExternalStorageWritable()) {
//            Log.e(TAG, "Tidak dapat mengakses penyimpanan eksternal");
//            return;
//        }
//
//        // Membuat direktori folder MyPresensi di penyimpanan eksternal jika belum ada
//        File folder = new File(getExternalFilesDir(null), "MyPresensi");
//        if (!folder.exists()) {
//            if (folder.mkdirs()) {
//                Log.d(TAG, "Folder MyPresensi berhasil dibuat");
//            } else {
//                Log.e(TAG, "Gagal membuat folder MyPresensi");
//                return;
//            }
//        }
//
//// Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
//        File fotoWajahGrayFolder = new File(folder, "fotowajahgray");
//        if (!fotoWajahGrayFolder.exists()) {
//            if (fotoWajahGrayFolder.mkdirs()) {
//                Log.d(TAG, "Subfolder fotowajahgray berhasil dibuat");
//            } else {
//                Log.e(TAG, "Gagal membuat subfolder fotowajahgray");
//                return;
//            }
//        }
//
////// Menambahkan file .nomedia ke folder fotowajahgray
////        File nomediaFileGray = new File(fotoWajahGrayFolder, ".nomedia");
////        if (!nomediaFileGray.exists()) {
////            try {
////                if (nomediaFileGray.createNewFile()) {
////                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder fotowajahgray");
////                } else {
////                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder fotowajahgray");
////                }
////            } catch (IOException e) {
////                e.printStackTrace();
////                return;
////            }
////        }
//
//// Membuat subfolder fotowajahwarna di dalam folder MyPresensi jika belum ada
//        File fotoWajahWarnaFolder = new File(folder, "fotowajahwarna");
//        if (!fotoWajahWarnaFolder.exists()) {
//            if (fotoWajahWarnaFolder.mkdirs()) {
//                Log.d(TAG, "Subfolder fotowajahwarna berhasil dibuat");
//            } else {
//                Log.e(TAG, "Gagal membuat subfolder fotowajahwarna");
//                return;
//            }
//        }
//
////// Menambahkan file .nomedia ke folder fotowajahwarna
////        File nomediaFileWarna = new File(fotoWajahWarnaFolder, ".nomedia");
////        if (!nomediaFileWarna.exists()) {
////            try {
////                if (nomediaFileWarna.createNewFile()) {
////                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder fotowajahwarna");
////                } else {
////                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder fotowajahwarna");
////                }
////            } catch (IOException e) {
////                e.printStackTrace();
////                return;
////            }
////        }
//
//
//        // Mendapatkan path lengkap dari folder MyPresensi
//        String folderPath = folder.getAbsolutePath();
//        Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);
//
//        // Membuka atau membuat database SQLite
//        String databasePath = folderPath + File.separator + DATABASE_NAME;
//        database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);
//
//        // Mengeksekusi perintah SQL untuk membuat tabel siswa jika belum ada
//        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
//                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                COLUMN_NAMA + " TEXT, " +
//                COLUMN_NIS + " INTEGER, " +
//                COLUMN_KELAS + " TEXT)";
//        database.execSQL(createTableQuery);
//
//        // Menutup database setelah selesai
//        database.close();
//
//
//        // Menampilkan pesan ke logcat bahwa database berhasil dibuat
//        Log.d(TAG, "Database berhasil dibuat");
//    }
//
//    // Fungsi untuk memeriksa izin akses WRITE_EXTERNAL_STORAGE
//    private boolean isExternalStorageWritable() {
//        String state = Environment.getExternalStorageState();
//        return Environment.MEDIA_MOUNTED.equals(state);
//    }
//
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (mOpenCVCamera != null) {
//            Log.i(TAG, "onPause");
//            mOpenCVCamera.disableView();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (mOpenCVCamera != null) {
//            Log.i(TAG, "onDestroy");
//            mOpenCVCamera.disableView();
//        }
//    }
//
//    private String getCascadeFile() {
//        try {
//            // Nama file XML deteksi wajah Haar Cascade
//            String cascadeFileName = "haarcascade_frontalface_default.xml";
//
//            // Mendapatkan AssetManager dari konteks
//            AssetManager assetManager = getApplicationContext().getAssets();
//
//            // Membuat path absolut ke file XML dengan AssetManager
//            String cascadeFilePath = getFilesDir().getPath() + File.separator + cascadeFileName;
//
//            // Memeriksa apakah file XML sudah ada, jika belum, salin dari assets ke internal storage
//            if (!new File(cascadeFilePath).exists()) {
//                InputStream inputStream = assetManager.open(cascadeFileName);
//                OutputStream outputStream = new FileOutputStream(cascadeFilePath);
//
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//
//                inputStream.close();
//                outputStream.close();
//            }
//
//            return cascadeFilePath;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return "";
//    }
//
//    @Override
//    public void onCameraViewStarted(int width, int height) {
//        Log.i(TAG, "onCameraViewStarted lebar = " + width + " tinggi = " + height);
//        mRgba = new Mat(height, width, CvType.CV_8UC4);
//
//        mFaceCascade = new CascadeClassifier();
//        mFaceCascade.load(getCascadeFile());
//
//    }
//
//    @Override
//    public void onCameraViewStopped() {
//
//        mRgba.release();
//        Log.i(TAG, "onCameraViewStopped");
//
//    }
//
//
//
//
//    @Override
//    public Mat onCameraFrame(@NonNull CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Log.i(TAG, "onCameraFrame ");
//
//        if (!isButtonPressed) {
//            return inputFrame.rgba();
//        }
//        Mat mRgba = inputFrame.rgba();
//
//        float zoomFactor = 1.0f;
//        // Mengatur faktor zoom
//        float scaleWidth = (float) inputFrame.rgba().cols() / mOpenCVCamera.getWidth();
//        float scaleHeight = (float) inputFrame.rgba().rows() / mOpenCVCamera.getHeight();
//        float scaleFactor = zoomFactor * Math.max(scaleWidth, scaleHeight);
//
//        // Mengubah ukuran citra dengan faktor zoom
//        Mat zoomedFrame = new Mat();
//        Imgproc.resize(inputFrame.rgba(), zoomedFrame, new Size(), scaleFactor, scaleFactor);
//
//        // Deteksi wajah pada citra yang telah di-zoom
//        Mat gray = new Mat();
//        Imgproc.cvtColor(zoomedFrame, gray, Imgproc.COLOR_RGBA2GRAY);
//        Imgproc.equalizeHist(gray, gray);
//
//        String cascadeFilePath = getCascadeFile();
//        CascadeClassifier face_cascade = new CascadeClassifier(cascadeFilePath);
//
//        // Deteksi wajah menggunakan Cascade Classifier
//        MatOfRect faces = new MatOfRect();
//        face_cascade.detectMultiScale(gray, faces, 1.08, 6, Objdetect.CASCADE_SCALE_IMAGE, new Size(200, 200));
//
//        Rect[] facesArray = faces.toArray();
//        if (facesArray.length > 0) {
//            // Set isFaceDetected menjadi true jika terdeteksi setidaknya satu wajah
//            isFaceDetected = true;
//            lastDetectionTime = System.currentTimeMillis(); // Update waktu deteksi terakhir
//            int minFaceSize = 200; // Ukuran minimum persegi wajah yang diizinkan
//            int largeFaceSize = 300; // Ukuran persegi wajah yang dianggap besar
//
//            for (Rect face : facesArray) {
//                // Menghitung koordinat persegi pada citra asli sebelum di-zoom
//                int x = (int) (face.x / scaleFactor);
//                int y = (int) (face.y / scaleFactor);
//                int width = (int) (face.width / scaleFactor);
//                int height = (int) (face.height / scaleFactor);
//                Rect originalFaceRect = new Rect(x, y, width, height);
//
//                // Memeriksa ukuran persegi wajah
//                if (width >= minFaceSize && height >= minFaceSize) {
//                    // Jika ukuran persegi wajah besar, ambil gambar
//                    if (width >= largeFaceSize && height >= largeFaceSize) {
//                        // Ambil gambar wajah
//                        // ...
//
//                        // Menggambar persegi pada citra asli
//                        Imgproc.rectangle(mRgba, originalFaceRect.tl(), originalFaceRect.br(), new Scalar(255, 255, 255), 5);
//
//                        // Ambil gambar hanya pada bagian wajah
//                        Mat croppedFace = new Mat(mRgba, originalFaceRect);
//
//                        // Tunda penangkapan gambar selama 0,32 detik
//                        try {
//                            Thread.sleep(120); // Tunda selama 0,32 detik (320 milidetik)
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//
//                        // Simpan data gambar wajah sementara (menggunakan array sebanyak 20 foto)
//                        if (isButtonPressed && counter < 20) {
//                            Mat croppedFaceCopy = new Mat();
//                            croppedFace.copyTo(croppedFaceCopy);
//                            temporaryFaceImages[counter] = croppedFaceCopy;
//
//                            // saveCroppedFaceImageTofotoWajahGray(croppedFace, inputNama.getText().toString());
//                            saveCroppedFaceImageTofotoWajahGray(croppedFace);
//
//                            counter++;
//
//                            if (counter == 20) {
//                                // Hentikan kamera setelah 20 gambar wajah terdeteksi
//                                // mOpenCVCamera.disableView();
//                                Intent intent = new Intent(RekamDataWajah.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
//                            }
//
//                        }
//                    }
//                }
//            }
//        } else {
//            // Set isFaceDetected menjadi false jika tidak ada wajah terdeteksi
//            isFaceDetected = false;
//
//            // Periksa waktu deteksi terakhir dan hentikan kamera jika wajah tidak terdeteksi selama 5 detik
//            long currentTime = System.currentTimeMillis();
//            long elapsedTime = currentTime - lastDetectionTime;
//            if (elapsedTime > 5000) { // 5000 milidetik = 5 detik
//                // Hentikan kamera
//                // mOpenCVCamera.disableView();
//                Intent intent = new Intent(RekamDataWajah.this, MainActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        }
//
//        return mRgba;
//    }
//
//
//
//
//
//
//
//    // Fungsi untuk menyimpan gambar wajah ke folder DCIM
//    private void saveCroppedFaceImageTofotoWajahGray(Mat croppedFace) {
////        private void saveCroppedFaceImageTofotoWajahGray(Mat croppedFace, String nama, String nis, String kelas) {
//        // Membuat direktori folder MyPresensi jika belum ada
//        File folder = new File(getExternalFilesDir(null), "MyPresensi");
//        if (!folder.exists()) {
//            folder.mkdirs();
//        }
//
//        // Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
//        File fotoWajahGrayFolder = new File(folder, "fotowajahgray");
//        if (!fotoWajahGrayFolder.exists()) {
//            fotoWajahGrayFolder.mkdirs();
//        }
//
//        // Mendapatkan path lengkap dari folder MyPresensi
//        String folderPath = folder.getAbsolutePath();
//        Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);
//
//// Membuka atau membuat database SQLite
//        String databasePath = folderPath + File.separator + DATABASE_NAME;
//        database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);
//
//// Mengeksekusi query untuk mendapatkan nama dari database berdasarkan id terakhir
//        String query = "SELECT nama_siswa FROM siswa ORDER BY id_siswa DESC LIMIT 1";
//        Cursor cursor = database.rawQuery(query, null);
//        String nameFromDatabase = "";
//        if (cursor.moveToFirst()) {
//            nameFromDatabase = cursor.getString(0);
//        }
//        cursor.close();
//        // Menutup database setelah selesai
//        database.close();
//
//// Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
//        File labels = new File(fotoWajahGrayFolder, nameFromDatabase);
//        if (!labels.exists()) {
//            labels.mkdirs();
//        }
//
//
//        // Menambahkan file .nomedia ke folder fotowajahgray
//        File nomediaFileLabel = new File(labels, ".nomedia");
//        if (!nomediaFileLabel.exists()) {
//            try {
//                if (nomediaFileLabel.createNewFile()) {
//                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder nomediaFileLabel");
//                } else {
//                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder nomediaFileLabel");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//        }
//
//// Menyimpan gambar wajah dengan nama sesuai inputan nama_siswa_count.jpg
//        int count = labels.list().length;
//        // Menyimpan gambar wajah dengan nama unik
//        String fileName = nameFromDatabase + "_" + count + ".jpg";
//        File file = new File(labels, fileName);
//
//        try {
//            // Konversi gambar wajah ke bitmap
//            Mat rgbImage = new Mat();
//            Imgproc.cvtColor(croppedFace, rgbImage, Imgproc.COLOR_RGBA2BGR);
//            Bitmap bitmap = Bitmap.createBitmap(rgbImage.cols(), rgbImage.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(rgbImage, bitmap);
//
//            // Menyimpan bitmap ke file JPEG
//            FileOutputStream outputStream = new FileOutputStream(file);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//            outputStream.flush();
//            outputStream.close();
//
//            // Tampilkan pesan sukses
//            Log.d("SaveImage", "Gambar wajah berhasil disimpan di: " + file.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Tampilkan pesan error jika terjadi masalah saat menyimpan gambar
//            Log.e("SaveImage", "Gagal menyimpan gambar wajah: " + e.getMessage());
//        }
//    }
//
//
//
//}