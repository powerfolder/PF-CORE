/*********************************************************************************
 NativeSysTray.java
 ------------------
 author               : Tamas Bara
 copyright            : (C) 2002-2004 by SnoozeSoft
 email                : snoozesoft@compuserve.de
 *********************************************************************************/

/*******************************************************************************
 * * This library is free software; you can redistribute it and/or * modify it
 * under the terms of the GNU Lesser General Public * License as published by
 * the Free Software Foundation; either * version 2.1 of the License, or (at
 * your option) any later version. * * This library is distributed in the hope
 * that it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU * Lesser General Public License for more details. * * You should have
 * received a copy of the GNU Lesser General Public * License along with this
 * library; if not, write to the Free Software * Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA * *
 ******************************************************************************/

package snoozesoft.systray4j;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.os.OSUtil;

class NativeSysTray implements SysTrayAccess {
    // A logger for logging to powerfolder
    private final static Logger LOG = Logger.getLogger(NativeSysTray.class);

    private boolean libraryLoaded = false;
    private static String libName = "systray4j";

    NativeSysTray() {
    	if (OSUtil.loadLibrary(LOG, libName)) {
    		libraryLoaded = true;
    	}
    	
        if (libraryLoaded)
            initNative(SysTrayMenu.VERSION);
    }

    public boolean isAvailable() {
        return libraryLoaded;
    }

    public void addMainMenu(SysTrayMenu menu, String iconFileName,
        String toolTip)
    {
        menu.id = addMainMenuNative(menu, iconFileName, toolTip);
    }

    public void addSubMenu(SubMenu menu) {
        menu.id = addSubMenuNative(menu);
    }

    public void setToolTip(int menuId, String tip) {
        setToolTipNative(menuId, tip);
    }

    public void showIcon(int menuId, boolean show) {
        showIconNative(menuId, show);
    }

    public void setIcon(int menuId, String iconFileName) {
        setIconNative(menuId, iconFileName);
    }

    public void enableItem(int menuId, int itemIndex, boolean enable) {
        enableItemNative(menuId, itemIndex, enable);
    }

    public void checkItem(int menuId, int itemIndex, boolean check) {
        checkItemNative(menuId, itemIndex, check);
    }

    public void setItemLabel(int menuId, int itemIndex, String label) {
        setItemLabelNative(menuId, itemIndex, label);
    }

    public void addItem(int menuId, int itemIndex, String label,
        boolean checkable, boolean check, boolean enable)
    {
        addItemNative(menuId, itemIndex, label, checkable, check, enable);
    }

    public void removeItem(int menuId, int itemIndex) {
        removeItemNative(menuId, itemIndex);
    }

    public void removeAll(int menuId) {
        removeAllNative(menuId);
    }

    public void dispose() {
        disposeNative();
    }

    private native void initNative(String version);

    private native int addMainMenuNative(SysTrayMenu menu, String iconFileName,
        String toolTip);

    private native int addSubMenuNative(SubMenu menu);

    private native void setToolTipNative(int menuId, String tip);

    private native void showIconNative(int menuId, boolean show);

    private native void setIconNative(int menuId, String iconFileName);

    private native void enableItemNative(int menuId, int itemIndex,
        boolean enable);

    private native void checkItemNative(int menuId, int itemIndex, boolean check);

    private native void setItemLabelNative(int menuId, int itemIndex,
        String label);

    private native void addItemNative(int menuId, int itemIndex, String text,
        boolean checkable, boolean check, boolean enable);

    private native void removeItemNative(int menuId, int itemIndex);

    private native void removeAllNative(int menuId);

    private native void disposeNative();
}