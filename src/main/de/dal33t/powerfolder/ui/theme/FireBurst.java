/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.theme;

import javax.swing.plaf.ColorUIResource;

import com.jgoodies.looks.plastic.PlasticTheme;

/**
 * A UI Theme with the typical powerfolder colors (red, orange)
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class FireBurst extends PlasticTheme  {

    public String getName() {
        return "Fireburst";
    }

    private final ColorUIResource softWhite  = new ColorUIResource(120, 80, 0);
    //private final ColorUIResource softWhite  = new ColorUIResource(165, 157, 143);

	private final ColorUIResource primary1   = new ColorUIResource(189, 33, 23); //90,  90,  66);// Dunkel: Rollbalkenrahmen-Dunkel
	private final ColorUIResource primary2   = new ColorUIResource(253, 209,  61); //132, 123,  90);// Mittel: Rollbalkenhintergrund/Grösste fläche
	private final ColorUIResource primary3   = new ColorUIResource(253, 229, 146); //148, 140, 107); //181, 173, 148); // Hell:   Ordnerfläche, Selektion, Rollbalken-Hoch, Menühintergrund

	private final ColorUIResource secondary1 = new ColorUIResource(235,  116,  48); // Abwärts  (dunkler)73,  59,  23);
	private final ColorUIResource secondary2 = new ColorUIResource(235,  116,  48); // Aufwärts (heller)136, 112,  46);  
	private final ColorUIResource secondary3 = new ColorUIResource(254,  242,  175); // Fläche   134, 104,  22);  
	
	protected ColorUIResource getPrimary1() { return primary1; }
	protected ColorUIResource getPrimary2() { return primary2; }
	protected ColorUIResource getPrimary3() { return primary3; }
	protected ColorUIResource getSecondary1() { return secondary1; }
	protected ColorUIResource getSecondary2() { return secondary2; }
	protected ColorUIResource getSecondary3() { return secondary3; }
	protected ColorUIResource getSoftWhite() { return softWhite; }
	
//	 
//    
//    public ColorUIResource getControl() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getControlShadow() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getControlDarkShadow() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getControlInfo() { return new ColorUIResource(Color.WHITE); } 
//    public ColorUIResource getControlHighlight() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getControlDisabled() { return new ColorUIResource(Color.WHITE); }  
//
//    public ColorUIResource getPrimaryControl() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getPrimaryControlShadow() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getPrimaryControlDarkShadow() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getPrimaryControlInfo() { return new ColorUIResource(Color.WHITE); } 
//    public ColorUIResource getPrimaryControlHighlight() { return new ColorUIResource(Color.WHITE); } 
//
//    protected ColorUIResource getBlack() { return new ColorUIResource(Color.WHITE); }
//    
//    public ColorUIResource getSystemTextColor() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getControlTextColor() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getInactiveControlTextColor() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getInactiveSystemTextColor() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getUserTextColor() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getTextHighlightColor() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getHighlightedTextColor() { return new ColorUIResource(Color.WHITE); }
//
//    public ColorUIResource getWindowBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getWindowTitleBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getWindowTitleForeground() { return new ColorUIResource(Color.WHITE); }  
//    public ColorUIResource getWindowTitleInactiveBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getWindowTitleInactiveForeground() { return new ColorUIResource(Color.WHITE); }
//    
//    public ColorUIResource getMenuBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getMenuForeground() { return  new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getMenuSelectedBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getMenuSelectedForeground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getMenuDisabledForeground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getSeparatorBackground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getSeparatorForeground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getAcceleratorForeground() { return new ColorUIResource(Color.WHITE); }
//    public ColorUIResource getAcceleratorSelectedForeground() { return new ColorUIResource(Color.WHITE); }

}
