package com.example.pril;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONObject;
import java.util.List;

public class SavedAccountAdapter extends RecyclerView.Adapter<SavedAccountAdapter.ViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(JSONObject account);
        void onAccountDelete(String email);
    }

    private final List<JSONObject> accounts;
    private final OnAccountClickListener listener;

    public SavedAccountAdapter(List<JSONObject> accounts, OnAccountClickListener listener) {
        this.accounts = accounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject account = accounts.get(position);
        try {
            String email = account.getString("email");
            String name = account.getString("name");
            String avatar = account.getString("avatar");

            holder.textViewName.setText(name.isEmpty() ? email : name);
            holder.textViewEmail.setText(email);

            if (!avatar.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(avatar).circleCrop().placeholder(R.drawable.profile).into(holder.imageViewAvatar);
            } else {
                holder.imageViewAvatar.setImageResource(R.drawable.profile);
            }

            holder.itemView.setOnClickListener(v -> listener.onAccountClick(account));
            holder.buttonDelete.setOnClickListener(v -> listener.onAccountDelete(email));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewName, textViewEmail;
        ImageButton buttonDelete;

        ViewHolder(View view) {
            super(view);
            imageViewAvatar = view.findViewById(R.id.imageViewAvatar);
            textViewName = view.findViewById(R.id.textViewName);
            textViewEmail = view.findViewById(R.id.textViewEmail);
            buttonDelete = view.findViewById(R.id.buttonDelete);
        }
    }
}