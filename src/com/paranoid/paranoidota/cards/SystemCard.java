/*
 * Copyright 2014 ParanoidAndroid Project
 *
 * This file is part of Paranoid OTA.
 *
 * Paranoid OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Paranoid OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Paranoid OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paranoid.paranoidota.cards;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.TextView;

import com.paranoid.paranoidota.signalv.R;
import com.paranoid.paranoidota.updater.GappsUpdater;
import com.paranoid.paranoidota.updater.RomUpdater;
import com.paranoid.paranoidota.widget.Card;

public class SystemCard extends Card {

    public SystemCard(Context context, AttributeSet attrs, RomUpdater romUpdater,
            GappsUpdater gappsUpdater, Bundle savedInstanceState) {
        super(context, attrs, savedInstanceState);

        setTitle(R.string.system_title);
        setLayoutId(R.layout.card_system);

        TextView romView = (TextView) findLayoutViewById(R.id.rom);
        romView.setText(romUpdater.getSystemCardText(context));

        TextView gappsView = (TextView) findLayoutViewById(R.id.gapps);
        gappsView.setText(gappsUpdater.getSystemCardText(context));
    }

    @Override
    public boolean canExpand() {
        return false;
    }

}
