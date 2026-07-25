package com.neoplay.tv.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.neoplay.tv.R;
import com.neoplay.tv.models.Channel;
import com.neoplay.tv.utils.FavoriteManager;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_GRID = 1;

    private List<Channel> channels;
    private OnChannelClickListener listener;
    private FavoriteManager favoriteManager;
    private int currentViewType = VIEW_TYPE_LIST;
    private int selectedPosition = -1;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
        void onChannelFocus(Channel channel);
        void onChannelLongClick(Channel channel);
    }

    public ChannelAdapter(List<Channel> channels, OnChannelClickListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    public void setViewType(int viewType) {
        this.currentViewType = viewType;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        this.selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    @Override
    public int getItemViewType(int position) {
        return currentViewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_GRID) ? R.layout.item_vod : R.layout.item_channel;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.tvName.setText(channel.getName());
        
        if (favoriteManager == null) {
            favoriteManager = new FavoriteManager(holder.itemView.getContext());
        }

        Glide.with(holder.itemView.getContext())
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo)
                .into(holder.ivLogo);

        boolean isFav = favoriteManager.isFavorite(channel.getId());
        holder.ivFavorite.setVisibility(isFav ? View.VISIBLE : View.GONE);

        // Hazırda baxılan kanalın vizual seçilməsi
        holder.itemView.setSelected(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel));
        holder.itemView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && 
                (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                listener.onChannelClick(channel);
                return true;
            }
            return false;
        });
        holder.itemView.setOnLongClickListener(v -> {
            listener.onChannelLongClick(channel);
            return true;
        });
        
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_up));
                v.setElevation(12f);
                listener.onChannelFocus(channel);
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_down));
                v.setElevation(0f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogo, ivFavorite;
        TextView tvName;
        ViewHolder(View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.ivLogo);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvName = itemView.findViewById(R.id.tvChannelName);
        }
    }
}
