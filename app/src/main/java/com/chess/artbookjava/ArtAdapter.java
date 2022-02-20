package com.chess.artbookjava;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chess.artbookjava.databinding.ReyclerRowBinding;

import java.util.ArrayList;

// Adapter yazdigimiz icin RecyclerView sinifindan kalitim aliyoruz.
// RecyclerView.Adapter<ViewHolder> --> ViewHolder gorevinde bir sinif yazacagiz. Bu sinifin icerisinde "gorunumlerimiz tutulacak".

public class ArtAdapter extends RecyclerView.Adapter<ArtAdapter.ArtHolder> {

    ArrayList<Art> artArrayList;

    // Adapter'umuz icin constructor yarattik.
    public ArtAdapter(ArrayList<Art> artArrayList){
        this.artArrayList = artArrayList;
    }

    @NonNull
    @Override
    public ArtHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ReyclerRowBinding reyclerRowBinding = ReyclerRowBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new ArtHolder(reyclerRowBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtHolder holder, @SuppressLint("RecyclerView") int position) {
      holder.binding.recyclerViewTextView.setText(artArrayList.get(position).name);
      holder.itemView.setOnClickListener(new View.OnClickListener() {// Herhangi bir RecyclerView "liste elemanina tiklandiginda" olacaklar -> itemView.setOnClickListener

          @Override
          public void onClick(View v) {
              Intent intent = new Intent(holder.itemView.getContext(),ArtActivity.class);
              intent.putExtra("info","old");
              intent.putExtra("artId",artArrayList.get(position).id);// artArrayList'den hangi konumdaysak onu al ve onun id'sinin degerini gonder.
              holder.itemView.getContext().startActivity(intent);
          }
      });
    }

    @Override
    // Kac tane RecyclerView liste elemani olusturulacagi burada belirtilir.
    public int getItemCount() {
        return artArrayList.size();// ArrayList icerisinde kac tane eleman varsa o kadari kadar RecyclerView elemani eklenecek.
    }
    public class ArtHolder extends RecyclerView.ViewHolder{
        private ReyclerRowBinding binding;

        // RecyclerView xml'i buraya baglanacak. ArtHolder Constructor'i !
        public ArtHolder(ReyclerRowBinding binding) {// Class -> RecyclerViwer'umuzun bulundugu xml'in ismi(ReyclerRow) + Binding, Nesne -> binding
            super(binding.getRoot());// getRoot() methodu ile gorunumu aldik. binding ile de oncesinde gorunumu almak icin baglamistik.
            this.binding = binding;// Gorunumun bulundugu xml'den binding adinda bir nesne urettik ve constructor parametresi ile bagladik.
        }
    }
}
