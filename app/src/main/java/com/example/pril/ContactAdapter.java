package com.example.pril;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.ItemContactBinding;
import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactModel> contactList;
    private final List<ContactModel> contactListFull;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(ContactModel contact);
        void onCallClick(ContactModel contact);
    }

    public ContactAdapter(List<ContactModel> contactList, OnContactClickListener listener) {
        this.contactList = contactList;
        this.contactListFull = new ArrayList<>(contactList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);
        holder.binding.textViewContactName.setText(contact.getName());
        holder.binding.textViewContactPhone.setText(contact.getPhoneNumber());

        if (contact.getAvatarUrl() != null && !contact.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(contact.getAvatarUrl())
                    .placeholder(R.drawable.profile)
                    .circleCrop()
                    .into(holder.binding.imageViewContactAvatar);
        } else {
            holder.binding.imageViewContactAvatar.setImageResource(R.drawable.profile);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact));
        holder.binding.imageButtonCallContact.setOnClickListener(v -> listener.onCallClick(contact));
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public void filter(String text) {
        List<ContactModel> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(contactListFull);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (ContactModel item : contactListFull) {
                if (item.getName().toLowerCase().contains(filterPattern) || 
                    item.getPhoneNumber().contains(filterPattern)) {
                    filteredList.add(item);
                }
            }
        }
        this.contactList = filteredList;
        notifyDataSetChanged();
    }

    // Method to update the full list when contacts are loaded
    public void updateList(List<ContactModel> newList) {
        this.contactList = new ArrayList<>(newList);
        this.contactListFull.clear();
        this.contactListFull.addAll(newList);
        notifyDataSetChanged();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        ItemContactBinding binding;
        public ContactViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}