package com.example.pril;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.ItemCallBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallAdapter extends RecyclerView.Adapter<CallAdapter.CallViewHolder> {

    private List<CallModel> callList;
    private final List<CallModel> callListFull;
    private final OnCallClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault());

    public interface OnCallClickListener {
        void onCallClick(CallModel call);
        void onCallLongClick(CallModel call, int position);
    }

    public CallAdapter(List<CallModel> callList, OnCallClickListener listener) {
        this.callList = callList;
        this.callListFull = new ArrayList<>(callList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCallBinding binding = ItemCallBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CallViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CallViewHolder holder, int position) {
        CallModel call = callList.get(position);
        holder.binding.textViewCallerName.setText(call.getName());
        
        if (call.getAvatarUrl() != null && !call.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(call.getAvatarUrl())
                    .placeholder(R.drawable.profile)
                    .circleCrop()
                    .into(holder.binding.imageViewCallAvatar);
        } else if (call.getAvatarRes() != 0) {
            holder.binding.imageViewCallAvatar.setImageResource(call.getAvatarRes());
        } else {
            holder.binding.imageViewCallAvatar.setImageResource(R.drawable.profile);
        }
        
        String typeText;
        int typeColor;
        
        switch (call.getCallType()) {
            case MISSED:
                typeText = "Пропущенный";
                typeColor = Color.RED;
                break;
            case OUTGOING:
                typeText = "Исходящий";
                typeColor = Color.GRAY;
                break;
            case INCOMING:
            default:
                typeText = "Входящий";
                typeColor = Color.parseColor("#4CAF50");
                break;
        }
        
        String timeStr = call.getTimestamp() != null ? sdf.format(call.getTimestamp().toDate()) : "Неизвестно";
        holder.binding.textViewCallInfo.setText(typeText + ", " + timeStr);
        holder.binding.textViewCallInfo.setTextColor(typeColor);
        
        holder.itemView.setOnClickListener(v -> listener.onCallClick(call));
        holder.binding.imageButtonCall.setOnClickListener(v -> listener.onCallClick(call));
        
        holder.itemView.setOnLongClickListener(v -> {
            listener.onCallLongClick(call, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return callList.size();
    }

    public void updateList(List<CallModel> newList) {
        callList = newList;
        callListFull.clear();
        callListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < callList.size()) {
            CallModel removedItem = callList.get(position);
            callList.remove(position);
            callListFull.remove(removedItem);
            notifyItemRemoved(position);
        }
    }

    public void filter(String text) {
        List<CallModel> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(callListFull);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (CallModel item : callListFull) {
                if (item.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(item);
                }
            }
        }
        this.callList = filteredList;
        notifyDataSetChanged();
    }

    static class CallViewHolder extends RecyclerView.ViewHolder {
        ItemCallBinding binding;
        public CallViewHolder(ItemCallBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
