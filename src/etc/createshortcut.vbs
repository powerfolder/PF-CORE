Dim ArgObj, var1,cmd,var2
Set ArgObj = WScript.Arguments 
Set WshShell = WScript.CreateObject("WScript.Shell")

var1 = ArgObj(0) 
var2 = ArgObj(1) 

Dim strPath, objShortcut
 strPath = WshShell.SpecialFolders( _
     "Desktop" )
 Set objShortcut = WshShell.CreateShortcut( _
     strPath & "\" & var1 & ".lnk" )
 strAcc = var2
 objShortcut.TargetPath = strAcc
 objShortcut.WindowStyle = 4
 objShortcut.Save