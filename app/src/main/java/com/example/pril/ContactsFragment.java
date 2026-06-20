package com.example.pril;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.pril.databinding.FragmentContactsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment {

    private FragmentContactsBinding binding;
    private final List<ContactModel> contactList = new ArrayList<>();
    private ContactAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        adapter = new ContactAdapter(contactList, new ContactAdapter.OnContactClickListener() {
            @Override
            public void onContactClick(ContactModel contact) {
                if (contact.getUid() == null) {
                    Log.e("ContactsLog", "Error: Contact UID is null");
                    return;
                }
                Bundle bundle = new Bundle();
                String name = contact.getName();
                bundle.putString("contactName", name != null ? name : getString(R.string.chat_default));
                bundle.putString("receiverId", contact.getUid());
                NavHostFragment.findNavController(ContactsFragment.this)
                        .navigate(R.id.action_ContactsFragment_to_ChatDetailFragment, bundle);
            }

            @Override
            public void onCallClick(ContactModel contact) {
                Bundle bundle = new Bundle();
                bundle.putString("receiverId", contact.getUid());
                bundle.putString("receiverName", contact.getName());
                NavHostFragment.findNavController(ContactsFragment.this)
                        .navigate(R.id.action_ContactsFragment_to_CallMenuFragment, bundle);
            }
        });
        
        binding.recyclerViewContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewContacts.setAdapter(adapter);

        binding.searchViewContacts.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        loadUsersFromFirestore();
    }

    private void loadUsersFromFirestore() {
        if (currentUserId == null) return;

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            contactList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                UserModel user = doc.toObject(UserModel.class);
                if (user != null) {
                    // Используем ID документа как UID, если поле внутри документа пустое
                    String uid = user.getUid() != null ? user.getUid() : doc.getId();
                    if (!uid.equals(currentUserId)) {
                        contactList.add(new ContactModel(
                                uid,
                                user.getName(),
                                user.getEmail(),
                                user.getAvatarUrl()
                        ));
                    }
                }
            }
            adapter.updateList(contactList);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
