REG ADD HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\services\WebClient\Parameters /v FileSizeLimitInBytes /t REG_DWORD /d 0xffffffff /f
net stop webclient
net start webclient
pause
