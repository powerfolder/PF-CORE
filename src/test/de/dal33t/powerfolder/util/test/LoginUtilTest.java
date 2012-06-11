package de.dal33t.powerfolder.util.test;

import java.util.Arrays;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;

public class LoginUtilTest extends TestCase {
    public void testObfuscate() {
        String password = "xC33öcn$k3444o$$44";
        String obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, new String(LoginUtil.deobfuscate(obf)));
        for (int i = 0; i < 200; i++) {
            password = IdGenerator.makeId();
            obf = LoginUtil.obfuscate(password.toCharArray());
            String deObf = new String(LoginUtil.deobfuscate(obf));
            assertEquals(deObf, password.length(), deObf.length());
            assertEquals(password, deObf);
        }
        assertNull(LoginUtil.obfuscate(null));
        assertNull(LoginUtil.deobfuscate(null));
        assertTrue(Arrays.equals("".toCharArray(),
            LoginUtil.deobfuscate(LoginUtil.obfuscate("".toCharArray()))));;
        assertTrue(Arrays.equals("  ".toCharArray(),
            LoginUtil.deobfuscate(LoginUtil.obfuscate("  ".toCharArray()))));;

        password = "%$§\"&/(09€";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, new String(LoginUtil.deobfuscate(obf)));
        
        password = "EsJs3XngawbCkMurIibtzQD23+OVPFjh2+uB4A8LaEA=";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, new String(LoginUtil.deobfuscate(obf)));
    }

    public void testHash() {
        String password = IdGenerator.makeId();
        String hasedSalted = LoginUtil.hashAndSalt(password);
        assertTrue(LoginUtil.matches(password.toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches("test".toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches(null, hasedSalted));
        // Legacy support.
        assertTrue(LoginUtil.matches("XXX".toCharArray(), "XXX"));
    }
}
