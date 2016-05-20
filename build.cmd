rem PyInstaller build script for Testbed
@echo off

rem Initial test build
rem pyinstaller -D --noconfirm --paths=./lister --icon=ST.ico ./lister/app.py

rem onefile build
rem pyinstaller -F --distpath=./dist --workpath=./build ./lister/app.py

rem Final release
call lein cljsbuild once min
pyinstaller --noconfirm build.spec
