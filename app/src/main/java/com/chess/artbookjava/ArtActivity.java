package com.chess.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.chess.artbookjava.databinding.ActivityArtBinding;
import com.chess.artbookjava.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ArtActivity extends AppCompatActivity {


    // findViewById yerine View Binding kullaniyoruz.
    private ActivityArtBinding binding;

    // NOT: Aktivite sonucu baslatici. Gorsel secilince ne olacagini yazmak, izin isteyip izin istendiginde ne olacagi gibi durumlari bunlarla yazariz.
    ActivityResultLauncher<Intent> activityResultLauncher;// Galeriye gectikten sonra neler olacagini yazmamizi saglayan nesne.
    ActivityResultLauncher<String> permissionLauncher;// Kullanici galerisine ulasilmasina izin verdikten sonra neler olacagini yazmamizi saglayan nesne.
                                                      // NOT: Izin almak icin String ifadelerle ugrasacagiz. Bundan dolayi ici String tipte alindi izin icin.
    // Bitmap degiskeni
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();// onCreate() methodu altinda galeriye ulasma ve ulastiktan sonra hangi islemlerin yapialcagi ile ilgili method cagriliyor.

        // "new" mu yoksa "old" value'suna sahip intent mi yollanmis onu kontrol ederek yeni kayit alacagiz ya da kaydedileni goruntuleyecegiz.
        // NOT: Menu'de new -> yeni kayit, RecyclerView'da old -> varolani goster var.
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equals("new")){// Eger info keywordune sahip ifade (putExtra icerisinde var bu keyword burada kullanip karsilastirmamiz icin menu ve recyclerview'da ayni isimleri verdik.)
                               // new value(deger)'ine sahipse, yeni kayit aliancak

            // Tum veri kisimlarinin icinin bos oldugundan emin olmak icin iclerini bos yapariz.
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.imageView.setImageResource(R.drawable.imgeselect);// Yeni resim secilmesi saglanir.
            // Eger yeni kayit girilecekse save butonu gorunur olsun!
            binding.button.setVisibility(View.VISIBLE);

        }
            else{// old value'suna sahipse, varolan kayit gosterilecek.
                int artId = intent.getIntExtra("artId",0);// RecyclerView adapter'daki id degeri alinip bir degere atansin.
                binding.button.setVisibility(View.INVISIBLE);// Varolan bilgi gosteriliyorsa da kaydetme tusu gorunmez yapilsin!!

                try{

                    // NOT: Cursor ile elemanlari gezecegiz ama hangi id'nin secilecegini bilemeyecegimizden ? koyduk WHERE id = filtrelemesinde.
                    // SQLiteStatement ifadesi rawQuery'de kullanilmadigi icin o gorevi 2. parametresi olan selectionArgs(selectionArguments) yapar.
                    // Ayni statement gibi filtreleyecegimiz deger icin String dizi yaratip eger String degilse de valueof() ile parse islemi uyguluyoruz.
                    // "Tum id degerlerini bir dizide tutup" -> secilenle islem yapmamiza olanak saglar bu islemler

                    database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
                    Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});

                    // Sonrasinda her bir degeri gezmek icin indis degerlerini aliyoruz.
                    int artNameIx = cursor.getColumnIndex("artname");
                    int painterNameIx = cursor.getColumnIndex("paintername");
                    int yearIx = cursor.getColumnIndex("year");
                    int imageIx = cursor.getColumnIndex("image");

                    // Son olarak tabi ki verileri goruntulemek icin geziyoruz.

                    while(cursor.moveToNext()){
                        binding.nameText.setText(cursor.getString(artNameIx));// cursor nesnesi icin parantezin icerisinde yazan indis degerindeki String degerini getir.
                        binding.artistText.setText(cursor.getString(painterNameIx));
                        binding.yearText.setText(cursor.getString(yearIx));


                        // Resmimizi almak.
                        byte[] bytes = cursor.getBlob(imageIx);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        binding.imageView.setImageBitmap(bitmap);
                    }

                    cursor.close();// Son olarak cursor'i kapatiyoruz.

                }catch (Exception e){
                    e.printStackTrace();
                }
            }


    }



    // SQLite islemlerimiz burada yapilacak. Cunku burasi kayit butonunun methodu.
    public void save(View view){// Butonun onclick methodu. Bir gorunum (xml nesnesi) tarafindan cagrildigi icin View classindan parametre alir.
                                // NOT: View classini detaylica arastir!

        // xml nesnelerine girilecek olan verileri binding methodu ile kod haline ceviriyoruz.
        String year = binding.yearText.getText().toString();// Sanatin yapildigi yili xml halinden koddaki String haline cevirdik.
        String artName = binding.nameText.getText().toString();// Sanat ismini aldik .
        String artistName = binding.artistText.getText().toString();

        // Olusturdugumuz method sayesinde kullanicinin aldigi resmi kaydediyoruz.

        Bitmap smallImage = makeSmallerImage(selectedImage,300);// Method icerisine --> (Bitmap degiskenimiz, maximum resim buyuklugu); verilerini gonderdik.

        // Son olarak resim --> "byte tipinde bir dizi" ifade ettiginden dolayi bu tipte veriye ceviriyoruz.

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();// Resim icin gerekli nesneyi olusturduk.
        smallImage.compress(Bitmap.CompressFormat.PNG,50, outputStream);// NOT: compress(Resmin donusturulecegi format, resim kalitesi, ByteArrayOutputStream nesnesi);
        byte[] byteArray = outputStream.toByteArray();// Resmin tipini, kalitesini belirledikten sonra en sonunda byte dizisine ceviriyoruz.

         // SQLite veritabanina kayit islemleri

        try{

            // 1) Veritabani olusturuldu
            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            // 2) Veritabani tablosu olusturuldu
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");// Resim BLOB tiptedir. Binary(0 ve 1) yani.
            // 3) Icerisine girilecek veriler icin String ifade olusturuldu. NOT: Burada dogrudan degeri biz degil de kullanici gireceginden farkli sekilde yapiyoruz.
            String sqlString = "INSERT INTO arts(artname, paintername, year, image) VALUES(?,?,?,?)";// Degerler ayni scanf kullanirmis gibi kullanici tarafindan girileceginden deger kisimlari basta "?" seklinde alinir.
            // 3.1) Veri girilmesini saglayan String ifadeyi yazdiktan sonra, SQLiteStatement sınıfı ile (SQLite beyanı).
            // database.compileStatement(sqlString) ifadesi de veritabani icerisine girilecek verilerin yapisini al ve database isimli veritabanimizda calistir. Bu calismayi da sqliteStatement degiskenine ata.
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            // NOT: Sonrasinda verileri baglama islemi yapiyoruz. Burada indisler 0'dan degil, 1'den baslar.
            sqLiteStatement.bindString(1,artName);// bindVeriTip(binding edilen degisken); methodu ile baglariz ve icerisinde de "xml ifadesini bagladigimiz degiskeni" yazariz.
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();// Son olarak da bu ifadeleri execute() methodu ile "çalıştır" diyoruz.

        }catch(Exception e){
           e.printStackTrace();
        }

        // Kayıt islemleri bittikten sonra tekrar anasayfaya donecegimiz icin bir intent yaziyoruz.
        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        // Veri alma islemini surduren ArtActivity calismasini biz ekran degistirdikten sonra bitirmesi icin asgidaki satiri yaziyoruz.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// Icinde bulundugumuz ArtActivity dahil tum aktivitleri kapat ve sadece gidecegimiz MainActivity'yi calistir diyoruz.
        startActivity(intent);



    }

    // Sqlite'a resim kaydetmeden once boyutunu kucultecegimiz method.
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){// Bitmap deger donduren bir method. Bu methoda parametre olarak gonderilen resim, image nesnesi uzerinden bruada isleme tabii tutulacak.

        int width = image.getWidth();// Gonderilen resmin genisligini aldik.
        int height = image.getHeight();// Gonderilen resmin yuksekligini aldik.
        float bitmapRatio = (float)width / (float) height;// Resmin dikey ve yatay boyutta nasil bir kucultmeye ugrayacagina karar vermek icin bu orani aldik. ratio = oran.
                                           // Not: Oranlar 1/3 gibi kesirli cikabileceginden float bir degiskende bu degeri sakliyoruz.

        if(bitmapRatio > 1){// NOT: Eger resmin en/boy degeri 1 den buyukse resim yataydir(landscape).
            width = maximumSize;// Genislik yokseklikten burada daha buyuk oldugu icin basta girecegimiz maximum deger onun olsun.
            height = (int) (width / bitmapRatio);// Sonrasinda uzunluk degerini de yuksekligin aldigi deger / oran degeri yaparak ayni oranda ikisini kucultmus oluruz.
                                                 // int'e cast ettik cunku height degeri int ve bitmapRatio float oldugundan dolayi esitledigimiz veri tipinde olmali sonuc deger.
        }

        else{// NOT: Eger resmin en/boy degeri 1 den kucukse resim dikeydir(portrait).
            // Yukaridakinin tam tersi durumda da mantiken tam tersi islemleri yapiyoruz. height icin maximum deger alinir bu sefer de.
            height = maximumSize;
           width = (int) (height * bitmapRatio);// Tersini yapiyoruz oranlama olmasi icin. Zaten bolersek daha buyuk deger cikardi.
        }

        // createScaledBitmap(boyutu ayarlanacak nesne, eni, boyu, filtre kullanilsin mi kullanilmasin mi) --> Daha buyuk veya daha kucuk boyutta sekilde ayarlanmis bir Bitmap olustur.
        return image.createScaledBitmap(image,width,height,true);

    }

    public void selectImage(View view){// ImageView nesnemiz icin onclick methodu.

        // Eger sart kabul edilmediyse izin isteyecegiz. (Ilk basta kabul edilmemis sayildigi icin izin isteyecegiz).
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
           // NOT: Burada kullaniciya mesaj gosterip gostermemeyi android karar verir. Biz bu karara karsi hem bunu hem de asagidaki dogrudan onay verip vermemeyi soran ifadeyi yazdik.
           // NOT: Kullanicinin ilk reddetmesinden sonra android asagidaki kullaniciya durumu aciklama blogunu calistirir.
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){// Kullanicinin neden izin vermesi gerektigi ile ilgili bir uyari methodu olusturuldu.
               Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {// setAction ile olusturdugumuz butona tiklandiginda olacaklari ayarlamak icin bir Listener yarattik.
                      permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                   }
               }).show();// Ayni Toast mesajda oldugu gibi burada da gosterilmesi icin show() methodu sona eklenir.
           }

           else{// Eger uyari mesajindan sonra kabul etmiyorsa son bir kez burada rica edecegiz.
               permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
           }
        }

        else{// Kabul edildiyse de kullanicinin galerisine ulasacagiz.

            // Intent ile galeriye gecis yapacagiz. Intent.ACTION_PICK = Intent sinifi methodlarindan "git şunu getir" ile galeriden gorsel getirecegiz.
            // 2. kisma yazdigimiz --> MediaStore.Images.Media.EXTERNAL_CONTENT_URI ifadesi de galeriye gidilmesini saglar. (Ezberlenmeli! ama yine de arastir.)
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);// Galeriye gectikten sonra neler olmasi gerektigi ile ilgili islemleri yazdigimiz nesneyi baslatiyoruz (launch).
                                                           // Cunku kullanici burada galerisine ulasilmasina izin verdi. Bundan dolayi baslattik.

        }
    }

    // NOT: ActivityResultLauncher<> ifadelerimizin ne yapacagini ayri bir kendi olusturdugumuz method icerisinde yazmamiz gerekir. Sonrasinda da onCreate() icerisinde bu ifadeleri kullanacagiz.

    private void registerLauncher(){

        // new ActivityResultContracts.StartActivityForResult() --> Gorselin nerede kayitli oldugunun bilgisi icin ( Gorselin URI bilgisi ) bir aktivite olusturduk.

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override

            // NOT: Kullanici resim sectiginde veya secmediginde asagidaki sartlara gore burasi calismaya baslaycak.
            public void onActivityResult(ActivityResult result) {// Kullanici galerisine ulastiktan sonra resim secmeyebilir, sayfayi kapatabilir... Bunun gibi durumlarda neler yapilacagini yazacagiz.

                // Burada boolean deger degil Aktivitesel islemler kontrol edecegiz.
                if(result.getResultCode() == RESULT_OK){// Eger kullanici resim "secerse" --> RESULT_OK
                   Intent intentFromResult = result.getData();// Secilen nesne verisini intent ile aliyoruz. (getIntent() yaparmis gibi intentFromResult nesnesinde veriyi depoladik.)
                   if(intentFromResult != null){// Eger sectigimiz nesnenin verisi bos bir nesne degil de resim gibi istedigimiz boyutu olan bir nesneyse...
                     Uri imageData =  intentFromResult.getData();// Burada kullandigimiz getData() ise URI bilgisini yani resmin (nesnenin) URL' de oldugu gibi baglanti adresini bize veriyor.
                                                                 // Sonunda sectigimiz nesnenin URI (kaynagina) eristik ve Uri classindan olusturugumuz imageData nesnesinde bu URI'yi depoladik.
                     //binding.imageView.setImageURI(imageData);// Son olarak da imageView'umuz uzerinde gosterilmesini sagladik setImageURI(URI kaynagi) methodunu kullanarak.

                       // SQLite icerisinde resmimizi depolayabilmek icin URI'yi resim bilgisine(Bitmap) cevirmeliyiz.
                       try{

                           if(Build.VERSION.SDK_INT >= 28){// Asagidaki donussturme islemi yalnizca Android API 28 ve uzerinde calistigi icin boyle bir kontrol ifadesi icerisinde yazdik.
                               ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(),imageData);
                               selectedImage = ImageDecoder.decodeBitmap(source);// Yukarida URI kodunu cozduk ve burada da cozulen kodu BitMap'e cevirdik.
                               binding.imageView.setImageBitmap(selectedImage);
                           }

                           else{// Eger Androi API 28 altindaki versiyona sahip bir telefonda bu islem yapiliyorsa...


                               selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);// imageData URI'sinin Bitmap'i cevrilmeye gerek kalmadan MediaStore'dan dogrudan alinsin.
                               binding.imageView.setImageBitmap(selectedImage);// Bitmap nesnesi imageView icin degistirilsin ve bu sayede secilen resim goruntulenebilsin.
                           }



                     }catch(Exception e){
                         e.printStackTrace();// Olusan hatalar olursa logcat uzerinden bu kod sayesinde hatalari gorbilecegiz.
                     }
                   }
                }
            }
        });

        // new ActivityResultContracts.RequestPermission() --> İzin isteme methodu. new ActivityResultCallback<Boolean>() --> callback(geri cagirma) Kullanicinin onay verip vermedigi ile ilgili boolean deger tutan bir geri donus ifadesi acilir asagida.
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
               if(result == true){// Eger kullanici erisime izin verdiyse.
                   Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);// Izin verildiyse galeriye ulasilsin.
                   activityResultLauncher.launch(intentToGallery);// Galeriye gectikten sonra neler olmasi gerektigi ile ilgili islemleri yazdigimiz nesneyi baslatiyoruz (launch).
                                                                  // Cunku kullanici burada galerisine ulasilmasina izin verdi. Bundan dolayi baslattik.
               }

               else{// Eger izin vermediyse.
                   Toast.makeText(ArtActivity.this,"Permission needed!",Toast.LENGTH_LONG).show();// Izin gerekli diye tekrar mesaj gosterilsin.
               }
            }
        });
    }

}