package com.op.kclock.ui;

import com.op.kclock.model.AlarmClock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;

/**
 * 
 * @author Alexander Pastukhov
 * 
 * look at trouble - http://ogrelab.ikratko.com/identifying-the-view-selected-in-a-contextmenu-in-oncontextitemselected-method/#comment-2848
 * 
 *
 */
public class TextViewWithMenu extends TextView {
	
	private AlarmClock alarm;
	
	public void setAlarm(AlarmClock alarm) {
		this.alarm = alarm;
	}

	public TextViewWithMenu(Context context) {
		super(context);
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return new TextViewMenuInfo(this);
	}

	public TextViewWithMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public TextViewWithMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public static class TextViewMenuInfo implements ContextMenu.ContextMenuInfo {
		public TextViewMenuInfo(View targetView) {
			this.targetView = (TextView) targetView;
		}

		public TextView targetView;
	}

	public AlarmClock getAlarm() {
		return alarm;
	}
}