package com.example.myapk;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    Intent intent;
    private Context context;
    private ArrayList<String> slideList;
    private ListView audioView;

    public RecyclerViewAdapter(Context context,ArrayList<String> slideList, ListView audioView){
        this.context = context;
        this.slideList = slideList;
        this.audioView = audioView;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.lower_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
               String listItem = slideList.get(position);
               holder.lowerText.setText(listItem);
    }

    @Override
    public int getItemCount() {
        return slideList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public TextView lowerText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            lowerText = itemView.findViewById(R.id.lowertext);
        }
        @Override
        public void onClick(View v) {
            int position = this.getAdapterPosition();


        }
    }



}
