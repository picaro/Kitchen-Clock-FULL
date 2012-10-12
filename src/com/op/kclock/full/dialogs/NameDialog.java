/**
 *  Kitchen Clock
 *  Copyright (C) 2012 Alexander Pastukhov
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package com.op.kclock.full.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.op.kclock.full.R;
import com.op.kclock.full.model.AlarmClock;

public class NameDialog extends Dialog {

	private AlarmClock alarm = null;
	private OnMyDialogResult mDialogResult; // the callback
	private TextView timerName;
	
	public NameDialog(Context context) {
		super(context);
	}

	public NameDialog(Context context, AlarmClock _alarm) {
		super(context);
		alarm = _alarm;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pickname);
		
		timerName = (TextView) findViewById(R.id.timepicker_input_name);
		timerName.setText(alarm.getName());

		Button buttonCancel = (Button) findViewById(R.id.cancelsettimer);
		buttonCancel.setOnClickListener(cancelHandler);
		Button settimerbtn = (Button) findViewById(R.id.settimerbtn);
		settimerbtn.setOnClickListener(saveHandler);
	}

	View.OnClickListener saveHandler = new View.OnClickListener() {
		public void onClick(View v) {
			alarm.setName(timerName.getText().toString());
			AlarmClock alarmT = alarm;
			alarm = null;
			mDialogResult.finish(alarmT);
			dismiss();
		}

	};

	
	View.OnClickListener cancelHandler = new View.OnClickListener() {
		public void onClick(View v) {
			dismiss();
		}
	};

	public void setDialogResult(Object dialogResult) {
		mDialogResult = (OnMyDialogResult) dialogResult;
	}

	public interface OnMyDialogResult {
		void finish(AlarmClock result);
	}

	@Override
	public void cancel() {
		super.cancel();
	}
	
}