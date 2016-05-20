# -*- mode: python -*-

block_cipher = None

a = Analysis(['testbed\\app.py'],
             pathex=['./testbed'],
             binaries=None,
             datas=[('./testbed/main.js', './'),
                    ('./testbed/style.css', './')],
             hiddenimports=[],
             hookspath=[],
             runtime_hooks=None,
             excludes=['_ssl', '_ctypes', 'select', 'bz2',
                       '_tkinter', 'tcl85', 'tk85'],
             win_no_prefer_redirects=None,
             win_private_assemblies=None,
             cipher=block_cipher)

# Filter some stuff out of the pure Python modules
def mod_filter(mod):
    """Return True if a module should be kept."""
    if 'encodings' in mod[0] and 'utf' not in mod[0]:
        # Excluding encodings breaks openpyxl (ElementTree)
        return False
    for name in ['ctypes', 'unittest', 'pyreadline', 'tkinter']:
        if name in mod[0]:
            return False
    return True

a.pure = [item for item in a.pure if mod_filter(item)]

pyz = PYZ(a.pure, a.zipped_data,
             cipher=block_cipher)
exe = EXE(pyz,
          a.scripts,
          a.binaries,
          a.zipfiles,
          a.datas,
          name='PyQt_Cljs_testbed',
          debug=False,
          strip=None,
          upx=True,
          console=True,#False,
)
