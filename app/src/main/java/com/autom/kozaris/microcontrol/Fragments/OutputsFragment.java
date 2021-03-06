package com.autom.kozaris.microcontrol.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.autom.kozaris.microcontrol.MainActivity;
import com.autom.kozaris.microcontrol.MicroModule;
import com.autom.kozaris.microcontrol.ModulesAdapter;
import com.autom.kozaris.microcontrol.R;

import java.util.ArrayList;
import java.util.List;


public class OutputsFragment extends Fragment{
    public List<MicroModule> OutputModuleList;
    public ModulesAdapter recyclerAdapter;
    ViewGroup mcontainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity) {
            OutputModuleList = new ArrayList<>();
            recyclerAdapter = new ModulesAdapter(getActivity(), OutputModuleList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mcontainer=container;
        View view = inflater.inflate(R.layout.fragment_main, mcontainer, false);
        RecyclerView recyclelist =view.findViewById(R.id.cardList);
        recyclelist.setHasFixedSize(true);
        LinearLayoutManager llmanager = new LinearLayoutManager(getActivity());
        llmanager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclelist.setAdapter(recyclerAdapter);
        recyclelist.setLayoutManager(llmanager);
        recyclerAdapter.notifyDataSetChanged();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);


        //  throw new RuntimeException(context.toString()
        //         + " must implement OnFragmentInteractionListener");

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



}
