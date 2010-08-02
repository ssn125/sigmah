/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.client.page.common.widget;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
/*
 * @author Alex Bertram
 */

public class RemoteComboBox<T extends ModelData> extends ComboBox<T> {

    @Override
    public void doQuery(String q, boolean forceAll) {
       store.getLoader().load(getParams(q));
        expand();
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
}