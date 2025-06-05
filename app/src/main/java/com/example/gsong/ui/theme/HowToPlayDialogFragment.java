package com.example.gsong.ui.theme;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gsong.R;

// Δημιουργία custom DialogFragment για το "Πώς παίζεται" (How to Play)
public class HowToPlayDialogFragment extends DialogFragment {

    // Δημιουργία του διαλόγου όταν ζητείται
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Φουσκώνουμε (inflate) το layout από το XML αρχείο how_to_play_dialog.xml
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.how_to_play_dialog, null);

        // Δημιουργούμε AlertDialog με custom style και το φουσκωμένο layout
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialog)
                .setView(view) // Ορίζουμε το custom περιεχόμενο του dialog
                .create();     // Δημιουργούμε το αντικείμενο του διαλόγου

        // Βρίσκουμε το κουμπί "Κλείσιμο" μέσα στο layout
        TextView closeBtn = view.findViewById(R.id.btn_close_dialog);

        // Ορίζουμε την ενέργεια του κουμπιού: απλά κλείνει το dialog
        closeBtn.setOnClickListener(v -> dismiss());

        // Επιστρέφουμε τον έτοιμο διάλογο
        return dialog;
    }
}
