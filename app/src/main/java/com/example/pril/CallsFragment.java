package com.example.pril;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.pril.databinding.FragmentCallsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class CallsFragment extends Fragment {

    private FragmentCallsBinding binding;
    private CallAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new CallAdapter(new ArrayList<>(), new CallAdapter.OnCallClickListener() {
            @Override
            public void onCallClick(CallModel call) {
                Bundle bundle = new Bundle();
                bundle.putString("receiverId", call.getSenderId().equals(currentUserId) ? call.getReceiverId() : call.getSenderId());
                bundle.putString("receiverName", call.getName());
                NavHostFragment.findNavController(CallsFragment.this)
                        .navigate(R.id.action_CallsFragment_to_CallMenuFragment, bundle);
            }

            @Override
            public void onCallLongClick(CallModel call, int position) {
                showDeleteDialog(call, position);
            }
        });

        binding.recyclerViewCalls.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewCalls.setAdapter(adapter);

        binding.searchViewCalls.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        loadCallHistory();
    }

    private void loadCallHistory() {
        if (currentUserId == null) return;

        db.collection("calls")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CallsFragment", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        List<CallModel> calls = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            CallModel call = doc.toObject(CallModel.class);
                            if (call != null) {
                                call.setId(doc.getId());
                                
                                String senderId = doc.getString("senderId");
                                String sName = doc.getString("senderName");
                                String rName = doc.getString("receiverName");

                                if (currentUserId.equals(senderId)) {
                                    call.setName(rName != null ? rName : "Вызов");
                                    call.setType(CallModel.CallType.OUTGOING.name());
                                    
                                    String receiverId = doc.getString("receiverId");
                                    if (receiverId != null) {
                                        db.collection("users").document(receiverId).get().addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                call.setAvatarUrl(userDoc.getString("avatarUrl"));
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                } else {
                                    call.setName(sName != null ? sName : "Входящий");
                                    call.setType(CallModel.CallType.INCOMING.name());
                                    
                                    if (senderId != null) {
                                        db.collection("users").document(senderId).get().addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                call.setAvatarUrl(userDoc.getString("avatarUrl"));
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                                calls.add(call);
                            }
                        }
                        
                        calls.sort((c1, c2) -> {
                            if (c1.getTimestamp() == null) return 1;
                            if (c2.getTimestamp() == null) return -1;
                            return c2.getTimestamp().compareTo(c1.getTimestamp());
                        });

                        adapter.updateList(calls);
                    }
                });
    }

    private void showDeleteDialog(CallModel call, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить запись")
                .setMessage("Вы уверены, что хотите удалить запись о звонке?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (call.getId() != null) {
                        db.collection("calls").document(call.getId()).delete();
                    }
                    adapter.removeItem(position);
                    Toast.makeText(getContext(), "Запись удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}