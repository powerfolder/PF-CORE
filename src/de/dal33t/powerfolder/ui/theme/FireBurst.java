/* $Id: FireBurst.java,v 1.4 2005/06/12 15:25:48 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.theme;

import javax.swing.plaf.ColorUIResource;

import com.jgoodies.looks.plastic.PlasticTheme;

/**
 * A UI Theme with the typical powerfolder colors (red, orange)
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class FireBurst extends PlasticTheme  {

    public String getName() {
        return "Fireburst";
    }

    // 
    private final ColorUIResource softWhite  = new ColorUIResource(120, 80, 0);
    //private final ColorUIResource softWhite  = new ColorUIResource(165, 157, 143);

	private final ColorUIResource primary1   = new ColorUIResource(189, 33, 23); //90,  90,  66);// Dunkel: Rollbalkenrahmen-Dunkel
	private final ColorUIResource primary2   = new ColorUIResource(253, 209,  61); //132, 123,  90);// Mittel: Rollbalkenhintergrund/Gr�sste fl�che
	private final ColorUIResource primary3   = new ColorUIResource(253, 229, 146); //148, 140, 107); //181, 173, 148); // Hell:   Ordnerfl�che, Selektion, Rollbalken-Hoch, Men�hintergrund

	private final ColorUIResource secondary1 = new ColorUIResource(235,  116,  48); // Abw�rts  (dunkler)73,  59,  23);
	private final ColorUIResource secondary2 = new ColorUIResource(235,  116,  48); // Aufw�rts (heller)136, 112,  46);  
	private final ColorUIResource secondary3 = new ColorUIResource(254,  242,  175); // Fl�che   134, 104,  22);  
	
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
