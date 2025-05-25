' scripts/launch-drivesync.vbs
' === EDIT THE PATH BELOW BEFORE USING ===

Set WshShell = CreateObject("WScript.Shell")
batchPath = "<path-to-your-start-drivesync.bat>"
WshShell.Run """" & batchPath & """", 0
Set WshShell = Nothing