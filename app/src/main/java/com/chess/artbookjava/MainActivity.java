package com.chess.artbookjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.chess.artbookjava.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // findViewById yerine View Binding kullaniyoruz.

    private ActivityMainBinding binding;
    // RecyclerView'da verilerimi goruntuleyebilmek icin, Art adli bir class yaratip ondan aldigimiz verileri de bir ArrayList'e kaydedecegiz.
    ArrayList<Art> artArrayList;
    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Initialize (tanimlama) islemleri onCreate() altinda yapilir.
        artArrayList = new ArrayList<>();// NOT: Ilk basta bos olarak tanimlanip, sonrasinda add methodu ile icerisi doldurulur.

        //RecyclerView'umuzun goruntusunu(layout) ayarliyoruz.
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));// Alt alta listelenir recyclerview elemanlari.
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        getData();

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        };
    }

    // Kullanicinin girdigi verileri kendisine gostermeye yarayan method. Anasayfada gosterecegimiz icin MainActivit'de yapiyoruz bu islemi.
    public void getData(){

        // Sadece girilen sanat eserinin ismi ve id'sini cekecegiz. id'sini de cekmemizin sebebi, ayni isiml sanat eseri kaydedebilir farkli resimlerle kullanici.
        // Ayni sanat eserini kaydetse de id'si farkli oldugu gozuksun ki birbirinden ayrildigini gorsun.

        try{

            // Burada da Arts adli database oncesinde yaratildigi icin open ozelligi ile database'e ulasilacak.
            SQLiteDatabase sqLiteDatabase = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM arts", null);
            int nameIndex = cursor.getColumnIndex("artname");// Database'e kaydedilen sutunun ismi.
            int idIndex = cursor.getColumnIndex("id");

            while(cursor.moveToNext()){
                String name = cursor.getString(nameIndex);
                int id = cursor.getInt(idIndex);

                //RecyclerView'da goruntuleyebilmek icin bir class yarattik ki ArrayList icerisine onu kullancagimizi yazalim ve ArrayList ile almaliyiz ki goruntuleyebilelim.
                Art art = new Art(name,id);
                artArrayList.add(art);// Art classindan olusturulan art nesnesini ekle dedik. Bu sayede icerisindeki name ve id degerleri ArrayList icerisine eklenmis olur.
            }

            // Veri girdikce recyclerview'un kendi gorunumunu guncellemesini saglar notifyDataChanged() -> Veri seti degisti, veriler artti.
            artAdapter.notifyDataSetChanged();

            // cursor son olarak kapatilir.
            cursor.close();



        }catch(Exception e){
            e.printStackTrace();
        }

    }




    // MENU YAPIMI
    // Menu ile aktiviteyi baglamak icin asagidaki 2 tane methodu override ederek ozelliklerini kendimize gore degistirmeliyiz (1- Baglama islemi, 2-Tiklandiginda neler olacak).

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {// Java kodlarimiz ile menu nesnesinin baglanmasi.

        MenuInflater menuInflater = getMenuInflater();// Menu'nun kendisine ozel inflater'i vardir --> xml icerigini java tabanli View nesnesine donusturen bir sinif.
        menuInflater.inflate(R.menu.art_menu,menu);// menuInflater.inflate(hangi menu kaynagini baglamak istiyorsun, hangi aktivitedeki nesne ile menu nesnesi baglanacak);
                                                   // inflate'in 2. kisminda onCreateOptionsMenu(Menu menu) icindeki menuInflater nesnesi ile baglanmistir. Cunku bu aktivite icerisinde kullanip islemler yaptiracagiz.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {// Menu icerigine (sutunlarina) tiklaninca hangi islemlerin gerceklesecegini yazdigimiz method.
                                                                  // onOptionsItemSelected --> "Menu seceneklerinden biri secilirse" anlamina gelir.
        if(item.getItemId() == R.id.add_art){// Method iceriginde MenuItem classindan olusturulan item nesnesi, "id'sini tanimladigimiz menu sutunumuz" ile iliskilendirilerek islemlere baslaniyor.
                                             // getItemId() methodu bizim ilk sutunumuzun id'si ile eslestiginde olusacak durumlari yazacagiz.
                                             // NOT: Birden fazla item(sutun,icerik) olsaydi else if diyerek onlar icin de icerikler tanimlardik.

            // Menu sutununa tiklaninca sanatsal iceriklerin gozukcegi sayfaya yonlendirilmeliyiz. Asagida da onu yapiyoruz.
            Intent intent = new Intent(this,ArtActivity.class);// Intent(MainActivity icerisindeyiz --> icinde oldugumuz yer , secildiginde gidecegimiz yer)
            intent.putExtra("info","new");// Anahtar kelimesi info secilen menuye tiklandiginda degeri new olan bir intent. Kontrol amacli bu degeri koyduk.
                                                      // NOT: ArtActivity icerisine de value = "old" olani koyup "hangisine basildiginda" -> yeni kayit, "hangisine basildiginda" -> sadece verileri goster islemini yapacagiz!!
            startActivity(intent);// Aktivite gecis islemimizi baslatan method.
        }


        return super.onOptionsItemSelected(item);
    }
}