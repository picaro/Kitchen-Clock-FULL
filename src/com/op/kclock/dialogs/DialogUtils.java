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
package com.op.kclock.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.op.kclock.*;

/**
 class for little dialogs. 
 seems inused now
 */
public class DialogUtils {

    /**
     * Creates an {@link AlertDialog} with given message and one button with given text, which when pressed finishes the {@link Activity}.
     */
    public static void createDialog(final Activity activity,String title, String message, String buttonText, DialogInterface.OnClickListener lsn, boolean cancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
		builder.setTitle(title);
        builder.setCancelable(cancelable);
		
		
		builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
		});	
        builder.setPositiveButton(buttonText,lsn );
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
