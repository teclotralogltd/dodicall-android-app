/*
 *
 * Copyright (C) 2016, Telco Cloud Trading & Logistic Ltd
 *
 * This file is part of dodicall.
 * dodicall is free software : you can redistribute it and / or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * dodicall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dodicall.If not, see <http://www.gnu.org/licenses/>.
 */

package ru.swisstok.dodicall.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.SelectAudioSrcButton;

public class OutgoingConferenceCallFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    @BindView(R.id.audio_src)
    SelectAudioSrcButton mAudioSrcButton;

    public static OutgoingConferenceCallFragment newInstance() {
        return new OutgoingConferenceCallFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_outgoing_confirence_call, container, false);
    }

    @OnClick({R.id.add_user, R.id.mic})
    void notImplementedClick() {
        Utils.showComingSoon(getContext());
    }

    @OnClick(R.id.audio_src)
    void onAudioClick() {

    }

    @OnClick(R.id.decline)
    void onDeclineClick() {
        mListener.onDecline();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        mAudioSrcButton.setCallback(new SelectAudioSrcButton.ProximityCallback() {
            @Override
            public void proximityOn() {
                mListener.onAcquireProximity();
            }

            @Override
            public void proximityOff() {
                mListener.onReleaseProximity();
            }
        });

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OutgoingConferenceCallFragment.OnConferenceCallFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mAudioSrcButton.release();
        mListener = null;
    }

    public interface OnFragmentInteractionListener extends OnConferenceCallFragmentInteractionListener {
        void onAddUser();
    }
}
