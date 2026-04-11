package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.security.Account;


public interface DeviceService {


    void deleteDeviceWithCompanions(Member node, Account caller);


    void deleteDevice(Member node, Account caller);
}
