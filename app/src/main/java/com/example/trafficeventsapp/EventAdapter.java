package com.example.trafficeventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private List<EventModel> events_list;
    private OnItemClickListener listener;
    public EventAdapter(List<EventModel> events, OnItemClickListener listener) {
        this.events_list = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull EventAdapter.ViewHolder holder, int position) {
        int resource = events_list.get(position).getImageView();
        String event_type = events_list.get(position).getEventType();
        String date = events_list.get(position).getEventDate();
        String confir_count = events_list.get(position).getEventConfirmationCount();


        holder.setData(resource, event_type, date, confir_count);


    }

    @Override
    public int getItemCount() {
        return events_list.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        ImageView im_delete;
        private TextView tv_ev_name;
        private TextView tv_ev_date;
        private TextView tv_ev_conf_count;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.im_view_hist);
            tv_ev_name = itemView.findViewById(R.id.rv_tv_event_name);
            tv_ev_date = itemView.findViewById(R.id.rv_tv_date);
            tv_ev_conf_count = itemView.findViewById(R.id.rv_tv_count_conf);
            im_delete = itemView.findViewById(R.id.delete_item);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });

            im_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDeleteClick(position);
                        }
                    }
                }
            });

        }
        public void setData(int resource, String ev_name, String ev_date, String conf_count) {
            imageView.setImageResource(resource);
            tv_ev_name.setText(ev_name);
            tv_ev_date.setText(ev_date);
            tv_ev_conf_count.setText(String.valueOf(conf_count));
        }
    }
}