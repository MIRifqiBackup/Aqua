package com.example.aqua;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RekamDataWajah extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "RekamDataWajah";
    private int mCurrentCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
    private boolean mIsSwitchingCamera = false;
    private Mat mRgba;

    private boolean mIsFrontCamera = true;

    private boolean isButtonPressed = false;

    private static final String DATABASE_NAME = "dbPresensi";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "siswa";
    private static final String COLUMN_ID = "id_siswa";
    private static final String COLUMN_NAMA = "nama_siswa";
    private static final String COLUMN_NIS = "nis_siswa";
    private static final String COLUMN_KELAS = "kelas_siswa";

    private SQLiteDatabase database;

    private long lastDetectionTime = 0;


    // Deklarasikan array untuk menyimpan foto wajah sementara
    private Mat[] temporaryFaceImages = new Mat[20];

    private int counter = 0; // Variabel untuk menghitung jumlah gambar yang sudah diambil

    private boolean isCameraStopped = false;

    // Deklarasikan variabel global
    private Button mulaiButton;
    private EditText inputNama;
    private EditText inputNIS;
    private EditText inputKelas;

    private Mat croppedFace;

    private CascadeClassifier mFaceCascade;

    private boolean isFaceDetected = false; // Variabel untuk menandai apakah wajah terdeteksi atau tidak

    private static final String DATASET_PATH = "fotowajahgray";
    private static final String MODEL_PATH = "MyPresensi/model.yml";
    private static final String LABEL_MAP_PATH = "MyPresensi/label_map.txt";

    private CameraBridgeViewBase mOpenCVCamera;
    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV is loaded");
                    mOpenCVCamera.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActivityCompat.requestPermissions(RekamDataWajah.this, new String[]{Manifest.permission.CAMERA}, 1);
        setContentView(R.layout.activity_rekam_data_wajah);

        mOpenCVCamera = findViewById(R.id.frame_surface);
        mOpenCVCamera.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCamera.setCvCameraViewListener(this);

        // Use the front-facing camera
        mOpenCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);



        Button mulaiButton = findViewById(R.id.button2);
        EditText inputNama = findViewById(R.id.inputNama);
        EditText inputNIS = findViewById(R.id.inputNIS);
        EditText inputKelas = findViewById(R.id.inputKelas);

        mulaiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isButtonPressed = true;

                // Membuat direktori folder MyPresensi di penyimpanan eksternal jika belum ada
                File folder = new File(getExternalFilesDir(null), "MyPresensi");
                if (!folder.exists()) {
                    if (folder.mkdirs()) {
                        Log.d(TAG, "Folder MyPresensi berhasil dibuat");
                    } else {
                        Log.e(TAG, "Gagal membuat folder MyPresensi");
                        return;
                    }
                }

                // Mendapatkan path lengkap dari folder MyPresensi
                String folderPath = folder.getAbsolutePath();
                Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);

                // Membuka atau membuat database SQLite
                String databasePath = folderPath + File.separator + DATABASE_NAME;
                database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);



                // Dapatkan nilai dari inputNama, inputNIS, dan inputKelas
                String nama = inputNama.getText().toString();
                int nis = Integer.parseInt(inputNIS.getText().toString());
                String kelas = inputKelas.getText().toString();

                // Menyimpan data ke dalam tabel
                ContentValues contentValues = new ContentValues();
                contentValues.put(COLUMN_NAMA, nama);
                contentValues.put(COLUMN_NIS, nis);
                contentValues.put(COLUMN_KELAS, kelas);
                database.insert(TABLE_NAME, null, contentValues);
                // Menutup database setelah selesai
                database.close();
                // Menampilkan pesan ke logcat bahwa database berhasil dibuat
                Log.d(TAG, "input siswa berhasil disimpan");

            }
        });

//        // Inisialisasi model pengenalan wajah menggunakan metode LBPH
//        recognizer = LBPHFaceRecognizer.create();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCVCamera.setCameraPermissionGranted();
                }
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "onResume");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "onResume Failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallBack);
        }
        // Reset the switching camera flag
        mIsSwitchingCamera = false;

        // Periksa izin akses WRITE_EXTERNAL_STORAGE
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "Tidak dapat mengakses penyimpanan eksternal");
            return;
        }

        // Membuat direktori folder MyPresensi di penyimpanan eksternal jika belum ada
        File folder = new File(getExternalFilesDir(null), "MyPresensi");
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.d(TAG, "Folder MyPresensi berhasil dibuat");
            } else {
                Log.e(TAG, "Gagal membuat folder MyPresensi");
                return;
            }
        }

// Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
        File fotoWajahGrayFolder = new File(folder, "fotowajahgray");
        if (!fotoWajahGrayFolder.exists()) {
            if (fotoWajahGrayFolder.mkdirs()) {
                Log.d(TAG, "Subfolder fotowajahgray berhasil dibuat");
            } else {
                Log.e(TAG, "Gagal membuat subfolder fotowajahgray");
                return;
            }
        }

//// Menambahkan file .nomedia ke folder fotowajahgray
//        File nomediaFileGray = new File(fotoWajahGrayFolder, ".nomedia");
//        if (!nomediaFileGray.exists()) {
//            try {
//                if (nomediaFileGray.createNewFile()) {
//                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder fotowajahgray");
//                } else {
//                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder fotowajahgray");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//        }

// Membuat subfolder fotowajahwarna di dalam folder MyPresensi jika belum ada
        File fotoWajahWarnaFolder = new File(folder, "fotowajahwarna");
        if (!fotoWajahWarnaFolder.exists()) {
            if (fotoWajahWarnaFolder.mkdirs()) {
                Log.d(TAG, "Subfolder fotowajahwarna berhasil dibuat");
            } else {
                Log.e(TAG, "Gagal membuat subfolder fotowajahwarna");
                return;
            }
        }

//// Menambahkan file .nomedia ke folder fotowajahwarna
//        File nomediaFileWarna = new File(fotoWajahWarnaFolder, ".nomedia");
//        if (!nomediaFileWarna.exists()) {
//            try {
//                if (nomediaFileWarna.createNewFile()) {
//                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder fotowajahwarna");
//                } else {
//                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder fotowajahwarna");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//        }


        // Mendapatkan path lengkap dari folder MyPresensi
        String folderPath = folder.getAbsolutePath();
        Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);

        // Membuka atau membuat database SQLite
        String databasePath = folderPath + File.separator + DATABASE_NAME;
        database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);

        // Mengeksekusi perintah SQL untuk membuat tabel siswa jika belum ada
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAMA + " TEXT, " +
                COLUMN_NIS + " INTEGER, " +
                COLUMN_KELAS + " TEXT)";
        database.execSQL(createTableQuery);

        // Menutup database setelah selesai
        database.close();


        // Menampilkan pesan ke logcat bahwa database berhasil dibuat
        Log.d(TAG, "Database berhasil dibuat");
    }

    // Fungsi untuk memeriksa izin akses WRITE_EXTERNAL_STORAGE
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCVCamera != null) {
            Log.i(TAG, "onPause");
            mOpenCVCamera.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCVCamera != null) {
            Log.i(TAG, "onDestroy");
            mOpenCVCamera.disableView();
        }
    }

    private String getCascadeFile() {
        try {
            // Nama file XML deteksi wajah Haar Cascade
            String cascadeFileName = "haarcascade_frontalface_default.xml";

            // Mendapatkan AssetManager dari konteks
            AssetManager assetManager = getApplicationContext().getAssets();

            // Membuat path absolut ke file XML dengan AssetManager
            String cascadeFilePath = getFilesDir().getPath() + File.separator + cascadeFileName;

            // Memeriksa apakah file XML sudah ada, jika belum, salin dari assets ke internal storage
            if (!new File(cascadeFilePath).exists()) {
                InputStream inputStream = assetManager.open(cascadeFileName);
                OutputStream outputStream = new FileOutputStream(cascadeFilePath);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();
            }

            return cascadeFilePath;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted lebar = " + width + " tinggi = " + height);
        mRgba = new Mat(height, width, CvType.CV_8UC4);

        mFaceCascade = new CascadeClassifier();
        mFaceCascade.load(getCascadeFile());

    }

    @Override
    public void onCameraViewStopped() {

        mRgba.release();
        Log.i(TAG, "onCameraViewStopped");

    }




    @Override
    public Mat onCameraFrame(@NonNull CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG, "onCameraFrame ");

        if (!isButtonPressed) {
            return inputFrame.rgba();
        }
        Mat mRgba = inputFrame.rgba();

        float zoomFactor = 1.0f;
        // Mengatur faktor zoom
        float scaleWidth = (float) inputFrame.rgba().cols() / mOpenCVCamera.getWidth();
        float scaleHeight = (float) inputFrame.rgba().rows() / mOpenCVCamera.getHeight();
        float scaleFactor = zoomFactor * Math.max(scaleWidth, scaleHeight);

        // Mengubah ukuran citra dengan faktor zoom
        Mat zoomedFrame = new Mat();
//        Imgproc.resize(inputFrame.rgba(), zoomedFrame, new Size(), scaleFactor, scaleFactor);
        Imgproc.resize(inputFrame.rgba(), zoomedFrame, new Size(), scaleFactor, scaleFactor, Imgproc.INTER_LINEAR);

        // Deteksi wajah pada citra yang telah di-zoom
        Mat gray = new Mat();
        Imgproc.cvtColor(zoomedFrame, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.equalizeHist(gray, gray);

        String cascadeFilePath = getCascadeFile();
        CascadeClassifier face_cascade = new CascadeClassifier(cascadeFilePath);

        // Deteksi wajah menggunakan Cascade Classifier
        MatOfRect faces = new MatOfRect();
        face_cascade.detectMultiScale(gray, faces, 1.08, 6, Objdetect.CASCADE_SCALE_IMAGE, new Size(200, 200));
//        face_cascade.detectMultiScale(gray, faces, 1.08, 6, Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size(35, 35));

        Rect[] facesArray = faces.toArray();
        if (facesArray.length > 0) {
            // Set isFaceDetected menjadi true jika terdeteksi setidaknya satu wajah
            isFaceDetected = true;
            lastDetectionTime = System.currentTimeMillis(); // Update waktu deteksi terakhir
            int minFaceSize = 200; // Ukuran minimum persegi wajah yang diizinkan
            int largeFaceSize = 300; // Ukuran persegi wajah yang dianggap besar

            for (Rect face : facesArray) {
                // Menghitung koordinat persegi pada citra asli sebelum di-zoom
                int x = (int) (face.x / scaleFactor);
                int y = (int) (face.y / scaleFactor);
                int width = (int) (face.width / scaleFactor);
                int height = (int) (face.height / scaleFactor);
                Rect originalFaceRect = new Rect(x, y, width, height);

                // Memeriksa ukuran persegi wajah
                if (width >= minFaceSize && height >= minFaceSize) {
                    // Jika ukuran persegi wajah besar, ambil gambar
                    if (width >= largeFaceSize && height >= largeFaceSize) {
                        // Ambil gambar wajah
                        // ...

                        // Menggambar persegi pada citra asli
                        Imgproc.rectangle(mRgba, originalFaceRect.tl(), originalFaceRect.br(), new Scalar(255, 255, 255), 5);

                        // Ambil gambar hanya pada bagian wajah
                        Mat croppedFace = new Mat(mRgba, originalFaceRect);

                        // Tunda penangkapan gambar selama 0,32 detik
                        try {
                            Thread.sleep(120); // Tunda selama 0,32 detik (320 milidetik)
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Simpan data gambar wajah sementara (menggunakan array sebanyak 20 foto)
                        if (isButtonPressed && counter < 20) {
                            Mat croppedFaceCopy = new Mat();
                            croppedFace.copyTo(croppedFaceCopy);
                            temporaryFaceImages[counter] = croppedFaceCopy;

                            // saveCroppedFaceImageTofotoWajahGray(croppedFace, inputNama.getText().toString());
                            saveCroppedFaceImageTofotoWajahGray(croppedFace);

                            counter++;

                            if (counter == 20) {
                                // Hentikan kamera setelah 20 gambar wajah terdeteksi
                                // mOpenCVCamera.disableView();
                                Intent intent = new Intent(RekamDataWajah.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }

                        }
                    }
                }
            }
        } else {
            // Set isFaceDetected menjadi false jika tidak ada wajah terdeteksi
            isFaceDetected = false;

            // Periksa waktu deteksi terakhir dan hentikan kamera jika wajah tidak terdeteksi selama 5 detik
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastDetectionTime;
            if (elapsedTime > 5000) { // 5000 milidetik = 5 detik
                // Hentikan kamera
                // mOpenCVCamera.disableView();
                Intent intent = new Intent(RekamDataWajah.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        return mRgba;
    }

    // Fungsi untuk menyimpan gambar wajah ke folder DCIM
    private void saveCroppedFaceImageTofotoWajahGray(Mat croppedFace) {
//        private void saveCroppedFaceImageTofotoWajahGray(Mat croppedFace, String nama, String nis, String kelas) {
        // Membuat direktori folder MyPresensi jika belum ada
        File folder = new File(getExternalFilesDir(null), "MyPresensi");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
        File fotoWajahGrayFolder = new File(folder, "fotowajahgray");
        if (!fotoWajahGrayFolder.exists()) {
            fotoWajahGrayFolder.mkdirs();
        }

        // Mendapatkan path lengkap dari folder MyPresensi
        String folderPath = folder.getAbsolutePath();
        Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);

// Membuka atau membuat database SQLite
        String databasePath = folderPath + File.separator + DATABASE_NAME;
        database = openOrCreateDatabase(databasePath, Context.MODE_PRIVATE, null);

// Mengeksekusi query untuk mendapatkan nama dari database berdasarkan id terakhir
        String query = "SELECT nama_siswa FROM siswa ORDER BY id_siswa DESC LIMIT 1";
        Cursor cursor = database.rawQuery(query, null);
        String nameFromDatabase = "";
        if (cursor.moveToFirst()) {
            nameFromDatabase = cursor.getString(0);
        }
        cursor.close();
        // Menutup database setelah selesai
        database.close();

// Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
        File labels = new File(fotoWajahGrayFolder, nameFromDatabase);
        if (!labels.exists()) {
            labels.mkdirs();
        }

        // Menambahkan file .nomedia ke folder fotowajahgray
        File nomediaFileLabel = new File(labels, ".nomedia");
        if (!nomediaFileLabel.exists()) {
            try {
                if (nomediaFileLabel.createNewFile()) {
                    Log.d(TAG, "File .nomedia berhasil ditambahkan ke folder nomediaFileLabel");
                } else {
                    Log.e(TAG, "Gagal menambahkan file .nomedia ke folder nomediaFileLabel");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

// Menyimpan gambar wajah dengan nama sesuai inputan nama_siswa_count.jpg
        int count = labels.list().length;
        // Menyimpan gambar wajah dengan nama unik
        String fileName = nameFromDatabase + "_" + count + ".jpg";
        File file = new File(labels, fileName);

        try {
            // Konversi gambar wajah ke grayscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(croppedFace, grayImage, Imgproc.COLOR_RGBA2GRAY);

// Buat bitmap grayscale
            Bitmap bitmap = Bitmap.createBitmap(grayImage.cols(), grayImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(grayImage, bitmap);

// Menyimpan bitmap ke file JPEG
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            outputStream.flush();
            outputStream.close();

// Tampilkan pesan sukses
            Log.d("SaveImage", "Gambar wajah berhasil disimpan di: " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            // Tampilkan pesan error jika terjadi masalah saat menyimpan gambar
            Log.e("SaveImage", "Gagal menyimpan gambar wajah: " + e.getMessage());
        }

    }
}
//    private void trainData() {
//        // Mendapatkan path lengkap dari folder MyPresensi
//        File folder = new File(getExternalFilesDir(null), "MyPresensi");
//        String folderPath = folder.getAbsolutePath();
//        Log.d(TAG, "Lokasi folder MyPresensi: " + folderPath);
//
//        // Membuat subfolder fotowajahgray di dalam folder MyPresensi jika belum ada
//        File fotoWajahGrayFolder = new File(folder, "fotowajahgray");
//        if (!fotoWajahGrayFolder.exists()) {
//            fotoWajahGrayFolder.mkdirs();
//        }
//
//        // Inisialisasi recognizer LBPH
//        FaceRecognizer recognizer = LBPHFaceRecognizer.create();
//
//        // List untuk menyimpan data wajah dan label
//        List<Mat> faceImages = new ArrayList<>();
//        List<Integer> labels = new ArrayList<>();
//
//        // Mendapatkan daftar subfolder di dalam fotowajahgray
//        File[] subfolders = fotoWajahGrayFolder.listFiles();
//        if (subfolders != null) {
//            for (File subfolder : subfolders) {
//                if (subfolder.isDirectory()) {
//                    String label = subfolder.getName();
//
//                    // Mendapatkan daftar file gambar dalam subfolder
//                    File[] imageFiles = subfolder.listFiles();
//                    if (imageFiles != null) {
//                        for (File imageFile : imageFiles) {
//                            if (imageFile.isFile()) {
//                                // Mengubah gambar menjadi grayscale
//                                Mat grayImage = Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
//                                if (grayImage.empty()) {
//                                    Log.e(TAG, "Gagal membaca gambar: " + imageFile.getAbsolutePath());
//                                    continue;
//                                }
//
//                                // Menambahkan gambar dan label ke list
//                                faceImages.add(grayImage);
//                                labels.add(Integer.parseInt(label));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Melatih recognizer dengan data wajah dan label
//        Mat labelsMat = new Mat(labels.size(), 1, CvType.CV_32SC1);
//        for (int i = 0; i < labels.size(); i++) {
//            labelsMat.put(i, 0, labels.get(i));
//        }
//        recognizer.train(faceImages, labelsMat);
//
//        // Simpan recognizer ke file
//        String recognizerPath = folderPath + File.separator + "lbph_recognizer.yml";
//        recognizer.save(recognizerPath);
//        Log.d(TAG, "Recognizer berhasil disimpan di: " + recognizerPath);

//        // Simpan label map sebagai file .yml
//        String labelMapPath = folderPath + File.separator + "label_map.yml";
//        Map<Integer, String> labelMap = new HashMap<>();
//        for (int i = 0; i < labels.size(); i++) {
//            labelMap.put(labels.get(i), subfolders[labels.get(i)].getName());
//        }
//        try {
//            // Save the label map as a .yml file
//            DumperOptions options = new DumperOptions();
//            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//
//            Yaml yaml = new Yaml(options);
//            FileOutputStream outputStream = new FileOutputStream(labelMapPath);
//            yaml.dump(labelMap, new OutputStreamWriter(outputStream));
//            outputStream.flush();
//            outputStream.close();
//            Log.d(TAG, "Label map berhasil disimpan di: " + labelMapPath);
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "Gagal menyimpan label map: " + e.getMessage());
//        }
//    }

//    private void trainData() {
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
//        // Path untuk file label map
//        String labelMapPath = folderPath + File.separator + "label_map.yml";
//
//        // Menyimpan label map sebagai file .yml
//        saveLabelMap(labelMap, labelMapPath);
//
//        // Path untuk file YAML
//        String yamlFilePath = folderPath + File.separator + "label_map.yaml";
//
//        // Menyimpan label map sebagai file YAML
//        writeLabelMapToYaml(labelMap, yamlFilePath);
//
//        // Melakukan proses training data LBPH
//        LBPHFaceRecognizer lbphFaceRecognizer = LBPHFaceRecognizer.create();
//        List<Mat> images = new ArrayList<>();
//        List<Integer> labels = new ArrayList<>();
//
//        // Mengambil data wajah dari subfolder fotowajahgray
//        File[] subfolders = fotoWajahGrayFolder.listFiles();
//        if (subfolders != null) {
//            for (File subfolder : subfolders) {
//                if (subfolder.isDirectory()) {
//                    String name = subfolder.getName();
//                    int label = Integer.parseInt(name); // Contoh: Nama subfolder berisi angka sebagai label
//                    File[] files = subfolder.listFiles();
//                    if (files != null) {
//                        for (File file : files) {
//                            // Mengubah file menjadi gambar grayscale dan menyimpannya ke daftar images
//                            Mat grayImage = loadImageAsGrayscale(file);
//                            images.add(grayImage);
//                            labels.add(label);
//                        }
//                    }
//                }
//            }
//        }
//
//        // Melakukan training menggunakan data wajah
//        MatVector imagesVector = Converters.vector_Mat_to_MatVector(images);
//        Mat labelsMat = Converters.vector_int_to_Mat(labels);
//        lbphFaceRecognizer.train(imagesVector, labelsMat);
//
//        // Menyimpan model LBPH
//        String lbphModelPath = folderPath + File.separator + "lbph_model.yml";
//        lbphFaceRecognizer.save(lbphModelPath);
//
//        // Tampilkan pesan sukses
//        Log.d(TAG, "Proses training data selesai");
//    }


//    private void trainData() {
//        // Menginisialisasi list untuk menyimpan data wajah dan label
//        List<Mat> faceImages = new ArrayList<>();
//        MatOfInt faceLabels = new MatOfInt();
//
//        // Mendapatkan path ke folder fotowajahgray
//        File folder = new File(getExternalFilesDir(null), "MyPresensi/fotowajahgray");
//
//        // Mendapatkan daftar subfolder (nama pemilik foto)
//        File[] ownerFolders = folder.listFiles();
//
//        // Menyimpan data wajah dan label dari setiap subfolder
//        List<String> labelMap = new ArrayList<>();
//        int labelIndex = 0;
//
//        for (File ownerFolder : ownerFolders) {
//            if (ownerFolder.isDirectory()) {
//                // Mendapatkan nama pemilik foto dari nama subfolder
//                String ownerName = ownerFolder.getName();
//
//                // Mendapatkan daftar file gambar wajah dalam subfolder
//                File[] faceFiles = ownerFolder.listFiles();
//
//                for (File faceFile : faceFiles) {
//                    if (faceFile.isFile()) {
//                        // Membaca gambar wajah dari file
//                        Mat faceImage = Imgcodecs.imread(faceFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
//
//                        // Menambahkan data wajah dan label ke list
//                        faceImages.add(faceImage);
//                        faceLabels.push_back(new MatOfInt(labelIndex));
//                    }
//                }
//
//                // Menambahkan pemilik foto ke label map
//                labelMap.add(ownerName);
//                labelIndex++;
//            }
//        }
//
//        // Membuat model pengenalan wajah LBPH
//        FaceRecognizer recognizer = LBPHFaceRecognizer.create();
//        recognizer.train(faceImages, faceLabels);
//
//        // Menyimpan model pengenalan wajah
//        String modelPath = getExternalFilesDir(null) + "/MyPresensi/lbph_model.xml";
//        recognizer.save(modelPath);
//        Log.d("SaveModel", "Model LBPH berhasil disimpan di: " + modelPath);
//
//        // Menyimpan label map sebagai file .yml
//        String labelMapPath = getExternalFilesDir(null) + "/MyPresensi/label_map.yml";
//        saveLabelMap(labelMap, labelMapPath);
//    }

//    private void trainData() {
//        // Mendapatkan path lengkap dari folder fotowajahgray
//        String folderPath = fotoWajahGrayFolder.getAbsolutePath();
//
//        // Mendapatkan daftar subfolder (nama pemilik foto)
//        File[] subFolders = fotoWajahGrayFolder.listFiles();
//
//        // Membuat label map untuk menyimpan pemetaan label ke ID
//        List<String> labelMap = new ArrayList<>();
//
//        // Membuat list untuk menyimpan data training
//        List<Mat> trainingData = new ArrayList<>();
//        List<Integer> labels = new ArrayList<>();
//
//        int label = 0;
//
//        // Loop melalui setiap subfolder (nama pemilik foto)
//        for (File subFolder : subFolders) {
//            if (subFolder.isDirectory()) {
//                // Mendapatkan nama pemilik foto dari nama subfolder
//                String ownerName = subFolder.getName();
//
//                // Menambahkan pemilik foto ke label map
//                labelMap.add(ownerName);
//
//                // Mendapatkan daftar file gambar wajah dalam subfolder
//                File[] imageFiles = subFolder.listFiles();
//
//                // Loop melalui setiap file gambar wajah
//                for (File imageFile : imageFiles) {
//                    if (imageFile.isFile()) {
//                        // Membaca gambar wajah ke dalam matriks
//                        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
//
//                        // Menambahkan gambar wajah dan label ke data training
//                        trainingData.add(image);
//                        labels.add(label);
//                    }
//                }
//
//                // Meningkatkan label untuk pemilik foto berikutnya
//                label++;
//            }
//        }
//
//        // Mengecek apakah ada data training yang tersedia
//        if (trainingData.isEmpty()) {
//            Log.e("TrainData", "Tidak ada data training yang tersedia.");
//            return;
//        }
//
//        // Membuat model LBPH recognizer
//        FaceRecognizer recognizer = LBPHFaceRecognizer.create();
//
//        // Melakukan pelatihan model dengan data training
//        MatVector trainingImages = Converters.vector_Mat_to_MatVector(trainingData);
//        Mat labelsMat = Converters.vector_int_to_Mat(labels);
//        recognizer.train(trainingImages, labelsMat);
//
//        // Simpan model ke file classifier.yml
//        String modelPath = folderPath + File.separator + "classifier.yml";
//        recognizer.save(modelPath);
//        Log.d("TrainData", "Model berhasil disimpan di: " + modelPath);
//
//        // Simpan label map sebagai file .yml
//        String labelMapPath = folderPath + File.separator + "label_map.yml";
//        saveLabelMap(labelMap, labelMapPath);
//
//        // Tulis label map ke file YAML
//        String labelMapYamlPath = folderPath + File.separator + "label_map";
//        writeLabelMapToYaml(labelMap, labelMapYamlPath);
//    }


//    private Mat convertLabelMapToMat(List<String> labelMap) {
//        Mat mat = new Mat(labelMap.size(), 1, CvType.CV_32S);
//        int[] data = new int[labelMap.size()];
//
//        for (int i = 0; i < labelMap.size(); i++) {
//            data[i] = i;
//        }
//
//        mat.put(0, 0, data);
//        return mat;
//    }
//
//    private void saveLabelMap(List<String> labelMap, String filePath) {
//        Mat labelMat = convertLabelMapToMat(labelMap);
//
//        // Simpan label map sebagai file .yml
//        FaceRecognizer.saveModel(filePath, labelMat);
//
//        // Tulis label map ke file YAML
//        DumperOptions options = new DumperOptions();
//        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//        Yaml yaml = new Yaml(options);
//
//        try {
//            FileWriter writer = new FileWriter(filePath + ".yml");
//            yaml.dump(labelMap, writer);
//            writer.close();
//            Log.d("SaveLabelMap", "Label map berhasil disimpan di: " + filePath + ".yml");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e("SaveLabelMap", "Gagal menyimpan label map: " + e.getMessage());
//        }
//    }

