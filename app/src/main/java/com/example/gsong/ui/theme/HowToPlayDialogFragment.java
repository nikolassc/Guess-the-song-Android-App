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

public class HowToPlayDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.how_to_play_dialog, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialog)
                .setView(view)
                .create();

        TextView closeBtn = view.findViewById(R.id.btn_close_dialog);
        closeBtn.setOnClickListener(v -> dismiss());

        return dialog;
    }
}
