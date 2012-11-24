package de.dal33t.powerfolder.disk.encryption;

import java.util.Map;

import de.schlichtherle.truezip.key.AbstractKeyManagerProvider;
import de.schlichtherle.truezip.key.KeyManager;

public class KeyManagerProvider extends AbstractKeyManagerProvider {

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return null;
    }
    
}
