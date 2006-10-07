 ----------------------------------------------------------
|                                                          |
|         SysTray for Java sources v2.4 (win32)            |
|                                                          |
 ----------------------------------------------------------

License: LGPL

-----------------------------------------------------------

Requirements:

   JDK + Windows libs and headers

-----------------------------------------------------------

Directories:

   java:
     contains the java code to build the systray4j.jar
     java library.
     
   win32:
     contains the C++ code to build the systray4j.dll
     native library, plus the VisualC++ project files. 

-----------------------------------------------------------    
  
v2.4:

  New:
  
    - Unicode support
    
    - systray now emulated on unsupported platforms
    
    - the KDE3 code now is based on JNI too  
  
-----------------------------------------------------------

v2.3.2:

  New:
  
    - SysTrayMenuIcon constructor now accepts URL as source
      for the icon to load.
    
    - the static member SysTrayMenu.dispose() was intro-
      duced as an alternative to System.exit(). It will
      stop all systray4j threads so they won´t keep the
      application alive.  
    
-----------------------------------------------------------    
   
v2.3.1:

  New:
  
    - Support for extended ASCII characters on win32
    
  Fixed:
    
    - setState() in CheckableMenuItem didn´t work when
      passing true as argument.

-----------------------------------------------------------    
    
v2.3

  New:
   
    - Checkable menu items.
    - Support for KDE3.

-----------------------------------------------------------    

v2.2.1
    
  Fixed:
    
    - Creating a submenu prior to the main menu made -
      at least sometimes - the virtual machine crash.
   
-----------------------------------------------------------
